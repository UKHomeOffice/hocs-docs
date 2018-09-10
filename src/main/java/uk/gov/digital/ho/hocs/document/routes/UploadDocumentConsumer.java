package uk.gov.digital.ho.hocs.document.routes;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.aws.sqs.SqsConstants;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.digital.ho.hocs.document.aws.S3DocumentService;
import uk.gov.digital.ho.hocs.document.dto.Document;
import uk.gov.digital.ho.hocs.document.dto.UpdateCaseDocumentRequest;

import java.util.UUID;

@Component
public class UploadDocumentConsumer extends RouteBuilder {
    private final String toQueue;
    private String dlq;
    private final int maximumRedeliveries;
    private final int redeliveryDelay;
    private final int backOffMultiplier;
    private S3DocumentService s3BucketService;


    @Autowired
    public UploadDocumentConsumer(
            S3DocumentService s3BucketService,
            @Value("${case.queue}") String toQueue,
            @Value("${docs.queue.dlq}") String dlq,
            @Value("${docs.queue.maximumRedeliveries}") int maximumRedeliveries,
            @Value("${docs.queue.redeliveryDelay}") int redeliveryDelay,
            @Value("${docs.queue.backOffMultiplier}") int backOffMultiplier) {
        this.s3BucketService = s3BucketService;
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
                .setProperty("caseUUID", simple("${body.caseUUID}"))
                .bean(s3BucketService, "uploadFile")
                .process(buildCaseMessage())
                .marshal().json(JsonLibrary.Jackson)
                .log(LoggingLevel.INFO, "Sending message to case queue")
                .to(toQueue)
                .log(LoggingLevel.INFO,"Document case request sent to case queue")
                .setHeader(SqsConstants.RECEIPT_HANDLE, exchangeProperty(SqsConstants.RECEIPT_HANDLE));
    }

    private Processor buildCaseMessage() {
        return exchange -> {
            Document response = exchange.getIn().getBody(Document.class);
            String caseUUID = exchange.getProperty("caseUUID").toString();
            UpdateCaseDocumentRequest request = new UpdateCaseDocumentRequest(UUID.randomUUID().toString(),caseUUID, response.getFilename(),response.getOriginalFilename());
            exchange.getOut().setBody(request);
        };
    }
}