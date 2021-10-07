package uk.gov.digital.ho.hocs.document.routes;

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
    private final DocumentDataService documentDataService;
    private final RequestData requestData;

    @Autowired
    public DocumentConsumer(
            DocumentDataService documentDataService,
            @Value("${docs.queue}") String docsQueue,
            @Value("${malwareQueueName}") String toQueue,
            RequestData requestData) {
        this.documentDataService = documentDataService;
        this.fromQueue = docsQueue;
        this.toQueue = toQueue;
        this.requestData = requestData;
    }

    @Override
    public void configure() {

        errorHandler(deadLetterChannel("log:document-queue"));

        from(fromQueue).routeId("document-queue")
                .setProperty(SqsConstants.RECEIPT_HANDLE, header(SqsConstants.RECEIPT_HANDLE))
                .process(transferHeadersToMDC())
                .log(LoggingLevel.INFO, "Reading document request for case")
                .log(LoggingLevel.DEBUG,"Received process document request ${body}")
                .unmarshal().json(JsonLibrary.Jackson, ProcessDocumentRequest.class)
                .setProperty("uuid", simple("${body.uuid}"))
                .setProperty("fileLink", simple("${body.fileLink}"))
                .setProperty("convertTo", simple("${body.convertTo}"))
                .setProperty("userId", simple("${body.userId}"))
                .setProperty("correlationId", simple("${body.correlationId}"))
                .bean(documentDataService, "getDocumentData(${body.uuid})")
                .setProperty("externalReferenceUUID", simple("${body.externalReferenceUUID}"))
                .setProperty("documentType", simple("${body.type}") )
                .log(LoggingLevel.DEBUG, "Doc type - ${body.type}")
                .process(RequestData.transferAuthPropertiesToQueue())
                .process(generateMalwareCheck())
                .process(RequestData.transferHeadersToQueue())
                .to(toQueue);
    }


    private Processor generateMalwareCheck() {
        return exchange -> {
            UUID documentUUID = UUID.fromString(exchange.getProperty("uuid").toString());
            UUID externalReferenceUUID = UUID.fromString(exchange.getProperty("externalReferenceUUID").toString());
            String fileLink = exchange.getProperty("fileLink").toString();
            String convertTo = exchange.getProperty("convertTo").toString();
            exchange.getOut().setBody( new DocumentMalwareRequest(documentUUID,fileLink, externalReferenceUUID, convertTo, requestData.userId(), requestData.correlationId()));
        };
    }

}
