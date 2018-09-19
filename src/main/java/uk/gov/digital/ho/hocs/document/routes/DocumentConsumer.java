package uk.gov.digital.ho.hocs.document.routes;

import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.aws.sqs.SqsConstants;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import uk.gov.digital.ho.hocs.document.dto.DocumentMalwareRequest;
import uk.gov.digital.ho.hocs.document.dto.ProcessDocumentRequest;

import static uk.gov.digital.ho.hocs.document.application.RequestData.transferHeadersToMDC;

@Component
public class DocumentConsumer extends RouteBuilder {

    private final String fromQueue;
    private final String toQueue;
    private String dlq;
    private final int maximumRedeliveries;
    private final int redeliveryDelay;
    private final int backOffMultiplier;

    @Autowired
    public DocumentConsumer(
            @Value("${docs.queue}") String docsQueue,
            @Value("${docs.queue.dlq}") String dlq,
            @Value("${docs.queue.maximumRedeliveries}") int maximumRedeliveries,
            @Value("${docs.queue.redeliveryDelay}") int redeliveryDelay,
            @Value("${docs.queue.backOffMultiplier}") int backOffMultiplier,
            @Value("${malwareQueueName}") String toQueue) {
        this.fromQueue = docsQueue;
        this.toQueue = toQueue;
        this.dlq = dlq;
        this.maximumRedeliveries = maximumRedeliveries;
        this.redeliveryDelay = redeliveryDelay;
        this.backOffMultiplier = backOffMultiplier;
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

        this.getContext().setStreamCaching(true);

        from(fromQueue).routeId("document-queue")
                .setProperty(SqsConstants.RECEIPT_HANDLE, header(SqsConstants.RECEIPT_HANDLE))
                .process(transferHeadersToMDC())
                .log(LoggingLevel.INFO, "Reading document request for case")
                .log("Received process document request ${body}")
                .unmarshal().json(JsonLibrary.Jackson, ProcessDocumentRequest.class)
                .setProperty("uuid", simple("${body.uuid}"))
                .process(generateMalwareCheck())
                .to(toQueue);
    }


    private Processor generateMalwareCheck() {
        return exchange -> {
            ProcessDocumentRequest request = exchange.getIn().getBody(ProcessDocumentRequest.class);
            exchange.getOut().setBody(new DocumentMalwareRequest(request.getFileLink(), request.getCaseUUID()));
        };
    }

}
