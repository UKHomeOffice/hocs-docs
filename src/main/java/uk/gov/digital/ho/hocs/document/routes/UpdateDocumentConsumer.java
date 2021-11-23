package uk.gov.digital.ho.hocs.document.routes;

import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.aws.sqs.SqsConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.digital.ho.hocs.document.DocumentDataService;
import uk.gov.digital.ho.hocs.document.application.RequestData;

@Component
public class UpdateDocumentConsumer extends RouteBuilder {

    private DocumentDataService documentDataService;

    @Autowired
    public UpdateDocumentConsumer(
            DocumentDataService documentDataService){
        this.documentDataService = documentDataService;
    }

    @Override
    public void configure()  {

        errorHandler(deadLetterChannel("log:document-update-queue"));

        from("direct:updaterecord")
                .process(RequestData.transferHeadersToMDC())
                .bean(documentDataService, "updateDocument(${body.uuid},${body.status}, ${body.fileLink},${body.pdfLink})")

                .setHeader(SqsConstants.RECEIPT_HANDLE, exchangeProperty(SqsConstants.RECEIPT_HANDLE));
    }
}
