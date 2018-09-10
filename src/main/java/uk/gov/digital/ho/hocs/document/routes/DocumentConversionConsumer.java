package uk.gov.digital.ho.hocs.document.routes;

import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.aws.sqs.SqsConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.digital.ho.hocs.document.ApplicationExceptions;
import uk.gov.digital.ho.hocs.document.HttpProcessors;
import uk.gov.digital.ho.hocs.document.aws.S3DocumentService;
import uk.gov.digital.ho.hocs.document.dto.UploadDocument;

@Component
public class DocumentConversionConsumer extends RouteBuilder {

    private String dlq;
    private final int maximumRedeliveries;
    private final int redeliveryDelay;
    private final int backOffMultiplier;
    private S3DocumentService s3BucketService;
    private final String hocsConverterPath;
    private final String toQueue;

    @Autowired
    public DocumentConversionConsumer(
            S3DocumentService s3BucketService,
            @Value("${hocsconverter.path}") String hocsConverterPath,
            @Value("${docs.queue.dlq}") String dlq,
            @Value("${docs.queue.maximumRedeliveries}") int maximumRedeliveries,
            @Value("${docs.queue.redeliveryDelay}") int redeliveryDelay,
            @Value("${docs.queue.backOffMultiplier}") int backOffMultiplier,
            @Value("${uploadDocumentQueueName}") String toQueue) {
        this.s3BucketService = s3BucketService;
        this.hocsConverterPath =  String.format("%s?throwExceptionOnFailure=false", hocsConverterPath);
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

        onException(ApplicationExceptions.DocumentConversionException.class).maximumRedeliveries(5);

        this.getContext().setStreamCaching(true);

        from("direct:convertdocument").routeId("conversion-queue")
                .log(LoggingLevel.INFO, "Retrieving document from S3")
                .setProperty("caseUUID", simple("${body.caseUUID}"))
                .bean(s3BucketService, "copyToTrustedBucket")
                .log("${body.filename}")
                .setProperty("originalFilename", simple("${body.filename}"))
                .process(HttpProcessors.buildMultipartEntity())
                .log("Calling document converter service")
                .to(hocsConverterPath)
                .log("Document conversion complete")
                .choice()
                .when(HttpProcessors.validateHttpResponse)
                    .convertBodyTo(byte[].class)
                    .process(generateUploadDocument())
                    .to(toQueue)
                .otherwise()
                    .log(LoggingLevel.ERROR, "Error ${body}")
                    .throwException(new ApplicationExceptions.DocumentConversionException("Document conversion failed"))
                    .setHeader(SqsConstants.RECEIPT_HANDLE, exchangeProperty(SqsConstants.RECEIPT_HANDLE));
    }

    private Processor generateUploadDocument() {
        return exchange -> {
            byte[] content =  (byte[]) exchange.getIn().getBody();
            String filename = exchange.getProperty("originalFilename").toString();
            String caseUUID = exchange.getProperty("caseUUID").toString();
            exchange.getOut().setBody(new UploadDocument(filename, content, caseUUID));
        };
    }


}
