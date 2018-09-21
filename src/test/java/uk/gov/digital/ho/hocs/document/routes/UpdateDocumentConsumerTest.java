package uk.gov.digital.ho.hocs.document.routes;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.digital.ho.hocs.document.DocumentDataService;
import uk.gov.digital.ho.hocs.document.exception.ApplicationExceptions;
import uk.gov.digital.ho.hocs.document.model.DocumentStatus;
import java.util.Properties;
import java.util.UUID;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.Silent.class)
public class UpdateDocumentConsumerTest extends CamelTestSupport {


    @Mock
    DocumentDataService documentDataService;

    private final String endpoint = "direct:updaterecord";
    private final String dlq = "mock:cs-dev-document-sqs-dlq";
    private UUID documentUUID = UUID.randomUUID();

    @Override
    protected RouteBuilder createRouteBuilder() throws Exception {

        return new UpdateDocumentConsumer(documentDataService, dlq, 0,0,0);
    }

    @Override
    protected Properties useOverridePropertiesWithPropertiesComponent() {

            Properties properties = new Properties();
            properties.put("uuid", documentUUID);
            properties.put("status", DocumentStatus.UPLOADED);
            properties.put("originalFilename", "oldfile.docx");
            properties.put("pdfFilename", "some.pdf");
            return properties;
    }

    @Test
    public void shouldAddMessagetoDLQnDocumentServiceError() throws Exception {
        doThrow(new ApplicationExceptions.EntityNotFoundException("Case not found")).when(documentDataService)
                .updateDocument(documentUUID, DocumentStatus.UPLOADED, "oldfile.docx", "some.pdf");
        template.sendBody(endpoint,"");
        getMockEndpoint(dlq).assertIsSatisfied();

    }

    @Test
    public void shouldCallDocumentDataService() throws Exception {
        doNothing().when(documentDataService)
                .updateDocument(documentUUID, DocumentStatus.UPLOADED, "oldfile.docx", "some.pdf");
        template.sendBody(endpoint,"");
        getMockEndpoint(dlq).assertIsSatisfied();
    }


}