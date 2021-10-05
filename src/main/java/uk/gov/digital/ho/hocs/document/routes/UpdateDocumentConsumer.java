package uk.gov.digital.ho.hocs.document.routes;

import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.aws.sqs.SqsConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.digital.ho.hocs.document.DocumentDataService;

@Component
public class UpdateDocumentConsumer extends RouteBuilder {

    private DocumentDataService documentDataService;

    @Autowired
    public UpdateDocumentConsumer(
            DocumentDataService documentDataService) {
        this.documentDataService = documentDataService;
    }

    @Override
    public void configure()  {

        from("direct:updaterecord")
                .log(LoggingLevel.DEBUG, "Updating document record")
                .bean(documentDataService, "updateDocument(${body.uuid},${body.status}," +
                        "${body.fileLink},${body.pdfLink})")
                .log(LoggingLevel.DEBUG, "Updated document record")
                .setHeader(SqsConstants.RECEIPT_HANDLE, exchangeProperty(SqsConstants.RECEIPT_HANDLE));
    }
}
