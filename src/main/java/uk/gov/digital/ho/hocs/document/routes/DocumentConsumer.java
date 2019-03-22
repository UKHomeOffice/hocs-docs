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
import uk.gov.digital.ho.hocs.document.DocumentDataService;
import uk.gov.digital.ho.hocs.document.application.RequestData;
import uk.gov.digital.ho.hocs.document.dto.camel.DocumentMalwareRequest;
import uk.gov.digital.ho.hocs.document.dto.camel.ProcessDocumentRequest;

import java.util.UUID;

import static uk.gov.digital.ho.hocs.document.application.RequestData.transferHeadersToMDC;

@Component
public class DocumentConsumer extends RouteBuilder {

    private final String fromQueue;
    private final String toQueue;
    private String dlq;
    private final int maximumRedeliveries;
    private final int redeliveryDelay;
    private final int backOffMultiplier;
    private final DocumentDataService documentDataService;

    @Autowired
    public DocumentConsumer(
            DocumentDataService documentDataService,
            @Value("${docs.queue}") String docsQueue,
            @Value("${docs.queue.dlq}") String dlq,
            @Value("${docs.queue.maximumRedeliveries}") int maximumRedeliveries,
            @Value("${docs.queue.redeliveryDelay}") int redeliveryDelay,
            @Value("${docs.queue.backOffMultiplier}") int backOffMultiplier,
            @Value("${malwareQueueName}") String toQueue) {
        this.documentDataService = documentDataService;
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
                .useOriginalMessage()
                .maximumRedeliveries(maximumRedeliveries)
                .redeliveryDelay(redeliveryDelay)
                .backOffMultiplier(backOffMultiplier)
                .asyncDelayedRedelivery()
                .logRetryStackTrace(false)
                .onPrepareFailure(exchange -> {
                    exchange.getIn().setHeader("FailureMessage", exchange.getProperty(Exchange.EXCEPTION_CAUGHT,
                            Exception.class).getMessage());
                    exchange.getIn().setHeader(SqsConstants.RECEIPT_HANDLE, exchangeProperty(SqsConstants.RECEIPT_HANDLE));
                }));


        from(fromQueue).routeId("document-queue")
                .setProperty(SqsConstants.RECEIPT_HANDLE, header(SqsConstants.RECEIPT_HANDLE))
                .process(transferHeadersToMDC())
                .log(LoggingLevel.INFO, "Reading document request for case")
                .log(LoggingLevel.DEBUG,"Received process document request ${body}")
                .unmarshal().json(JsonLibrary.Jackson, ProcessDocumentRequest.class)
                .setProperty("uuid", simple("${body.uuid}"))
                .setProperty("fileLink", simple("${body.fileLink}"))
                .bean(documentDataService, "getDocumentData(${body.uuid})")
                .setProperty("externalReferenceUUID", simple("${body.externalReferenceUUID}"))
                .setProperty("documentType", simple("${body.type}") )
                .log(LoggingLevel.DEBUG, "Doc type - ${body.type}")
                .process(generateMalwareCheck())
                .process(RequestData.transferHeadersToQueue())
                .to(toQueue);
    }


    private Processor generateMalwareCheck() {
        return exchange -> {
            UUID documentUUID = UUID.fromString(exchange.getProperty("uuid").toString());
            UUID externalReferenceUUID = UUID.fromString(exchange.getProperty("externalReferenceUUID").toString());
            String fileLink = exchange.getProperty("fileLink").toString();
            exchange.getOut().setBody( new DocumentMalwareRequest(documentUUID,fileLink, externalReferenceUUID));
        };
    }

}
