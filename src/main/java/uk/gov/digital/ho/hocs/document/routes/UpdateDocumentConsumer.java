package uk.gov.digital.ho.hocs.document.routes;

import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.aws.sqs.SqsConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.digital.ho.hocs.document.DocumentDataService;

@Component
public class UpdateDocumentConsumer extends RouteBuilder {

    private String dlq;

    private DocumentDataService documentDataService;

    @Autowired
    public UpdateDocumentConsumer(
            DocumentDataService documentDataService) {
        this.documentDataService = documentDataService;
    }

    @Override
    public void configure()  {

        errorHandler(deadLetterChannel("log:document-update-queue"));

        from("direct:updaterecord").routeId("document-update-queue")
                .bean(documentDataService, "updateDocument(${body.uuid},${body.status}, ${body.fileLink},${body.pdfLink})")
                .setHeader(SqsConstants.RECEIPT_HANDLE, exchangeProperty(SqsConstants.RECEIPT_HANDLE));
    }
}
