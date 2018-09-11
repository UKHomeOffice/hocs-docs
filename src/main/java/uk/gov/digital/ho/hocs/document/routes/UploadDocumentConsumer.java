package uk.gov.digital.ho.hocs.document.routes;

import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.aws.sqs.SqsConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.digital.ho.hocs.document.DocumentDataService;
import uk.gov.digital.ho.hocs.document.aws.S3DocumentService;
import uk.gov.digital.ho.hocs.document.model.DocumentStatus;

@Component
public class UploadDocumentConsumer extends RouteBuilder {
    private final String toQueue;
    private String dlq;
    private final int maximumRedeliveries;
    private final int redeliveryDelay;
    private final int backOffMultiplier;
    private S3DocumentService s3BucketService;
    private DocumentDataService documentDataService;


    @Autowired
    public UploadDocumentConsumer(
            S3DocumentService s3BucketService,
            DocumentDataService documentDataService,
            @Value("${documentServiceQueueName}") String toQueue,
            @Value("${docs.queue.dlq}") String dlq,
            @Value("${docs.queue.maximumRedeliveries}") int maximumRedeliveries,
            @Value("${docs.queue.redeliveryDelay}") int redeliveryDelay,
            @Value("${docs.queue.backOffMultiplier}") int backOffMultiplier) {
        this.s3BucketService = s3BucketService;
        this.documentDataService = documentDataService;
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

        from("direct:uploadtrustedfile").routeId("case-queue")
                .log(LoggingLevel.INFO, "Uploading file to trusted bucket")
                .bean(s3BucketService, "uploadFile")
                .log("${body.filename}")
                .setProperty("pdfFilename", simple("${body.filename}"))
                .setProperty("status", simple(DocumentStatus.UPLOADED.toString()))
                .to(toQueue);

        from("direct:updaterecord")
                .log(LoggingLevel.INFO, "Updating document record")
                // TODO: PDF LINK
                .bean(documentDataService, "updateDocument(${property.uuid},${property.status},${property.originalFilename}, ${property.pdfFilename})")
                .log(LoggingLevel.INFO, "Updated document record")
                .setHeader(SqsConstants.RECEIPT_HANDLE, exchangeProperty(SqsConstants.RECEIPT_HANDLE));
    }

}