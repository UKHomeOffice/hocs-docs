package uk.gov.digital.ho.hocs.document;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import uk.gov.digital.ho.hocs.document.aws.S3DocumentService;
import uk.gov.digital.ho.hocs.document.dto.ProcessDocumentRequest;
import uk.gov.digital.ho.hocs.document.dto.Document;
import uk.gov.digital.ho.hocs.document.dto.UpdateDocumentFromQueueRequest;
import uk.gov.digital.ho.hocs.document.dto.UploadDocument;

import java.io.ByteArrayInputStream;
import java.util.UUID;
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
    private S3DocumentService s3BucketService;
    private final String hocsConverterPath;

    @Autowired
    public DocumentConsumer(
            S3DocumentService s3BucketService,
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
    public void configure() {
        errorHandler(deadLetterChannel(dlq)
                .loggingLevel(LoggingLevel.ERROR)
                .retryAttemptedLogLevel(LoggingLevel.WARN)
                .log("Failed to process document")
                .useOriginalMessage()
                .maximumRedeliveries(maximumRedeliveries)
                .redeliveryDelay(redeliveryDelay)
                .backOffMultiplier(backOffMultiplier)
                .asyncDelayedRedelivery()
                .logRetryStackTrace(true)
                .onPrepareFailure(exchange -> {
                    exchange.getIn().setHeader("FailureMessage", exchange.getProperty(Exchange.EXCEPTION_CAUGHT,
                            Exception.class).getMessage());
                }));


        onException(ApplicationExceptions.MalwareCheckException.class).maximumRedeliveries(0);
        onException(ApplicationExceptions.DocumentConversionException.class).maximumRedeliveries(1);

        this.getContext().setStreamCaching(true);

        from(docsQueue)
                .setProperty(SqsConstants.RECEIPT_HANDLE, header(SqsConstants.RECEIPT_HANDLE))
                .process(transferHeadersToMDC())
                .log(LoggingLevel.INFO, "Reading document request for case")
                .unmarshal().json(JsonLibrary.Jackson, ProcessDocumentRequest.class)
                .setProperty("caseUUID",simple("${body.caseUUID}"))
                .setProperty("fileLink",simple("${body.fileLink}"))
                .log(LoggingLevel.INFO, "Retrieving document from S3")
                .bean(s3BucketService, "getFileFromS3(${property.fileLink})")
                .setProperty("fileType",simple("${body.fileType}"))
                .setProperty("filename",simple("${body.filename}"))
                .to("direct:malwarecheck");


        from("direct:malwarecheck")
                .log(LoggingLevel.INFO, "Calling Clam AV service")
                .process(buildMultipartEntity())
                .to(clamAvPath)
                .log(LoggingLevel.INFO, "Clam AV Response: ${body}")
                .choice()
                    .when(bodyAs(String.class).not().contains("Everything ok : true"))
                    .throwException(new ApplicationExceptions.MalwareCheckException("Document failed malware check"))
                .otherwise()
                    .log(LoggingLevel.INFO,"Clam AV check complete")
                    .to("direct:convertdocument");

        from("direct:convertdocument")
                .log("Calling document converter")
                .log(LoggingLevel.INFO,"Retrieving document from S3")
                .bean(s3BucketService, "copyToTrustedBucket(${property.fileLink},${property.caseUUID},${property.fileType})")
                .setProperty("originalFilename",simple("${body.filename}"))
                .process(buildMultipartEntity())
                .log("Calling document converter service")
                .to(hocsConverterPath)
                .log("Document conversion complete")
                .choice()
                .when(validateHttpResponse)
                    .convertBodyTo(byte[].class)
                    .process(generateUploadDocument())
                    .to("direct:uploadtrustedfile")
                .otherwise()
                    .throwException(new ApplicationExceptions.DocumentConversionException("Document conversion failed"));

        from("direct:uploadtrustedfile")
                .log(LoggingLevel.INFO, "Uploading file to trusted bucket")
                .bean(s3BucketService, "uploadFile")
                .process(buildCaseMessage())
                .log(LoggingLevel.INFO, "Sending message to case queue")
                .to(caseQueue)
                .log(LoggingLevel.INFO,"Document case request sent to case queue")
                .setHeader(SqsConstants.RECEIPT_HANDLE, exchangeProperty(SqsConstants.RECEIPT_HANDLE));
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

    private Processor buildCaseMessage() {
        return exchange -> {
            Document response = exchange.getIn().getBody(Document.class);
            String caseUUID = exchange.getProperty("caseUUID").toString();
            String originalFilename = exchange.getProperty("originalFilename").toString();
            UpdateDocumentFromQueueRequest request = new UpdateDocumentFromQueueRequest(UUID.randomUUID().toString(),caseUUID, response.getFilename(),originalFilename);
            ObjectMapper mapper = new ObjectMapper();
            exchange.getOut().setBody(mapper.writeValueAsString(request));
        };
    }

    private Processor generateUploadDocument() {
        return exchange -> {
            byte[] content =  (byte[]) exchange.getIn().getBody();
            String filename = exchange.getProperty("filename").toString();
            String caseUUID = exchange.getProperty("caseUUID").toString();
            exchange.getOut().setBody(new UploadDocument(filename, content, caseUUID));
        };
    }
}
