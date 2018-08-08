package uk.gov.digital.ho.hocs.document;

import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.Predicate;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.aws.sqs.SqsConstants;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.ContentBody;
import org.apache.http.entity.mime.content.InputStreamBody;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.digital.ho.hocs.document.dto.ProcessDocumentRequest;
import uk.gov.digital.ho.hocs.document.dto.Document;
import uk.gov.digital.ho.hocs.document.exceptions.MalwareCheckException;

import java.io.ByteArrayInputStream;

import static uk.gov.digital.ho.hocs.document.RequestData.transferHeadersToMDC;


@Component
public class DocumentConsumer extends RouteBuilder {


    private final String docsQueue;
    private final String caseQueue;
    private String dlq;
    private final int maximumRedeliveries;
    private final int redeliveryDelay;
    private final int backOffMultiplier;
    private final String clamAvPath;
    private S3FileUtils s3BucketService;
    private final String hocsConverterPath;

    @Autowired
    public DocumentConsumer(
                        S3FileUtils s3BucketService,
                        @Value("${clamav.path}") String clamAvPath,
                        @Value("${hocsconverter.path}") String hocsConverterPath,
                        @Value("${docs.queue}") String docsQueue,
                        @Value("${case.queue}") String caseQueue,
                        @Value("${docs.queue.dlq}") String dlq,
                        @Value("${docs.queue.maximumRedeliveries}") int maximumRedeliveries,
                        @Value("${docs.queue.redeliveryDelay}") int redeliveryDelay,
                        @Value("${docs.queue.backOffMultiplier}") int backOffMultiplier) {
        this.s3BucketService = s3BucketService;
        this.clamAvPath = String.format("http4://%s?throwExceptionOnFailure=false", clamAvPath);
        this.hocsConverterPath = String.format("http4://%s", hocsConverterPath);
        this.docsQueue = docsQueue;
        this.dlq = dlq;
        this.maximumRedeliveries = maximumRedeliveries;
        this.redeliveryDelay = redeliveryDelay;
        this.backOffMultiplier = backOffMultiplier;
        this.caseQueue = caseQueue;
    }

    @Override
    public void configure() throws Exception {
        errorHandler(deadLetterChannel(dlq)
                .loggingLevel(LoggingLevel.ERROR)
                .log("Failed to process document")
                .useOriginalMessage()
                .retryAttemptedLogLevel(LoggingLevel.WARN)
                .maximumRedeliveries(maximumRedeliveries)
                .redeliveryDelay(redeliveryDelay)
                .backOffMultiplier(backOffMultiplier)
                .asyncDelayedRedelivery()
                .logRetryStackTrace(true)
                .onPrepareFailure(exchange -> {
                    exchange.getIn().setHeader("FailureMessage", exchange.getProperty(Exchange.EXCEPTION_CAUGHT,
                            Exception.class).getMessage());
                }));

        onException(MalwareCheckException.class).maximumRedeliveries(0);

        from(docsQueue)
                .setProperty(SqsConstants.RECEIPT_HANDLE, header(SqsConstants.RECEIPT_HANDLE))
                .process(transferHeadersToMDC())
                .log(LoggingLevel.INFO, "Reading document request")
                .unmarshal().json(JsonLibrary.Jackson, ProcessDocumentRequest.class)
                .setProperty("caseUUID",simple("${body.caseUUID}"))
                .setProperty("fileLink",simple("${body.fileLink}"))
                .log(LoggingLevel.INFO, "Retrieving document from S3")
                .bean(s3BucketService, "getFileFromS3(${property.fileLink})")
                .setProperty("md5",simple("${body.md5}"))
                .to("direct:malwarecheck");

        from("direct:malwarecheck")
                .setProperty(SqsConstants.RECEIPT_HANDLE, header(SqsConstants.RECEIPT_HANDLE))
                .setHeader(SqsConstants.RECEIPT_HANDLE, exchangeProperty(SqsConstants.RECEIPT_HANDLE))
                .log(LoggingLevel.INFO, "Calling Clam AV service")
                .log(LoggingLevel.INFO, "Creating multipart POST request")
                .process(buildMultipartEntity())
                .log(LoggingLevel.INFO, "Created multipart POST request")
                .to(clamAvPath)
                .choice()
                    .when(body().not().contains("Everything ok : true"))
                    .throwException(new MalwareCheckException("Document failed malware check"))
                .otherwise()
                .log(LoggingLevel.INFO, "Clam AV Response: ${body}")
                .log("Clam AV check complete")
                .to("direct:convertdocument");

        from("direct:convertdocument")
                .setProperty(SqsConstants.RECEIPT_HANDLE, header(SqsConstants.RECEIPT_HANDLE))
                .setHeader(SqsConstants.RECEIPT_HANDLE, exchangeProperty(SqsConstants.RECEIPT_HANDLE))
                .log("Calling document converter")
                .log(LoggingLevel.INFO,"Retrieving document from S3")
                .bean(s3BucketService, "getFileFromS3(${property.fileLink})")
                .log(LoggingLevel.INFO, "Validating S3 hash matches virus scanned document")
                .validate(validateMd5Hash())
                .log(LoggingLevel.INFO, "Validated S3 hash matches virus scanned document")
                .process(buildMultipartEntity())
                .to(hocsConverterPath)
                .choice()
                    .when(validateHttpResponse)
                    .throwException(new MalwareCheckException("Document failed malware check"))
                .otherwise()
                .log("Document conversion complete")
                .log(LoggingLevel.INFO, "Adding document to Case Queue")

                //TODO transform message into a case-docs Add Document to Case request and add to case-queue

                .setHeader(SqsConstants.RECEIPT_HANDLE, exchangeProperty(SqsConstants.RECEIPT_HANDLE));
    }

    private Predicate validateMd5Hash() {
        return exchangeProperty("md5").isEqualTo(simple("${body.md5}"));
    }

    private Predicate validateHttpResponse = header(Exchange.HTTP_RESPONSE_CODE).isLessThan(300);

    private Processor buildMultipartEntity() {
        return exchange -> {
            Document response = exchange.getIn().getBody(Document.class);
            ContentBody content = new InputStreamBody(new ByteArrayInputStream(response.getData()), response.getFilename());
            MultipartEntityBuilder multipartEntityBuilder = MultipartEntityBuilder.create()
            .setMode(HttpMultipartMode.BROWSER_COMPATIBLE)
            .addPart("file", content)
            .addTextBody("name", response.getFilename());
            exchange.getOut().setBody(multipartEntityBuilder.build());
        };
    }
}
