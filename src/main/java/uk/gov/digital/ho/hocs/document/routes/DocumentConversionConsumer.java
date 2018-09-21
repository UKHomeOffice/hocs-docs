package uk.gov.digital.ho.hocs.document.routes;

import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.aws.sqs.SqsConstants;
import org.apache.camel.component.http4.HttpComponent;
import org.apache.camel.util.jsse.KeyManagersParameters;
import org.apache.camel.util.jsse.KeyStoreParameters;
import org.apache.camel.util.jsse.SSLContextParameters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.digital.ho.hocs.document.exception.ApplicationExceptions;
import uk.gov.digital.ho.hocs.document.HttpProcessors;
import uk.gov.digital.ho.hocs.document.aws.S3DocumentService;
import uk.gov.digital.ho.hocs.document.model.UploadDocument;
import uk.gov.digital.ho.hocs.document.model.DocumentStatus;

@Component
public class DocumentConversionConsumer extends RouteBuilder {

    private String dlq;
    private final int maximumRedeliveries;
    private final int redeliveryDelay;
    private final int backOffMultiplier;
    private S3DocumentService s3BucketService;
    private final String hocsConverterPath;
    private final String documentServiceQueueName;
    private final String toQueue;

    @Autowired
    public DocumentConversionConsumer(
            S3DocumentService s3BucketService,
            @Value("${hocsconverter.path}") String hocsConverterPath,
            @Value("${docs.queue.dlq}") String dlq,
            @Value("${docs.queue.maximumRedeliveries}") int maximumRedeliveries,
            @Value("${docs.queue.redeliveryDelay}") int redeliveryDelay,
            @Value("${docs.queue.backOffMultiplier}") int backOffMultiplier,
            @Value("${uploadDocumentQueueName}") String toQueue,
            @Value("${documentServiceQueueName}") String documentServiceQueueName) {
        this.s3BucketService = s3BucketService;
        this.hocsConverterPath =  String.format("%s?throwExceptionOnFailure=false&useSystemProperties=true", hocsConverterPath);
        this.documentServiceQueueName = documentServiceQueueName;
        this.dlq = dlq;
        this.maximumRedeliveries = maximumRedeliveries;
        this.redeliveryDelay = redeliveryDelay;
        this.backOffMultiplier = backOffMultiplier;
        this.toQueue = toQueue;


    }

    @Override
    public void configure() {
        errorHandler(deadLetterChannel(dlq)
                .loggingLevel(LoggingLevel.ERROR)
                .retryAttemptedLogLevel(LoggingLevel.WARN)
                .log("Failed to convert document")
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

        onException(ApplicationExceptions.DocumentConversionException.class).maximumRedeliveries(5);

        this.getContext().setStreamCaching(true);

        from("direct:convertdocument").routeId("conversion-queue")
                .log(LoggingLevel.INFO, "Retrieving document from S3: ${body.fileLink}")
                .setProperty("caseUUID", simple("${body.caseUUID}"))
                .bean(s3BucketService, "getFileFromTrustedS3(${body.fileLink})")
                .setProperty("originalFilename", simple("${body.filename}"))
                .process(HttpProcessors.buildMultipartEntity())
                .log("Calling document converter service")
                .to(hocsConverterPath)
                .log("DocumentDto conversion complete")
                .choice()
                .when(HttpProcessors.validateHttpResponse)
                    .log(LoggingLevel.INFO, "DocumentDto conversion successful")
                    .process(generateUploadDocument())
                    .to(toQueue)
                .otherwise()
                    .log(LoggingLevel.ERROR, "Error ${body}")
                    .setProperty("status", simple(DocumentStatus.FAILED_CONVERSION.toString()))
                    .to(documentServiceQueueName)
                    .throwException(new ApplicationExceptions.DocumentConversionException("DocumentDto conversion failed"))
                    .setHeader(SqsConstants.RECEIPT_HANDLE, exchangeProperty(SqsConstants.RECEIPT_HANDLE));
    }

    private Processor generateUploadDocument() {
        return exchange -> {
            byte[] content =  exchange.getIn().getBody(byte[].class);
            String filename = exchange.getProperty("originalFilename").toString();
            String caseUUID = exchange.getProperty("caseUUID").toString();
            exchange.getOut().setBody(new UploadDocument(filename, content, caseUUID));
        };
    }


}
