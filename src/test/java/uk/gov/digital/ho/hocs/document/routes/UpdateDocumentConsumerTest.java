package uk.gov.digital.ho.hocs.document.routes;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.digital.ho.hocs.document.DocumentDataService;
import uk.gov.digital.ho.hocs.document.dto.camel.UpdateDocumentRequest;
import uk.gov.digital.ho.hocs.document.model.DocumentStatus;
import java.util.UUID;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class UpdateDocumentConsumerTest extends CamelTestSupport {


    @Mock
    DocumentDataService documentDataService;

    private final String endpoint = "direct:updaterecord";
    private UUID documentUUID = UUID.randomUUID();

    private UpdateDocumentRequest request = new UpdateDocumentRequest(documentUUID, DocumentStatus.UPLOADED, "oldfile.docx", "some.pdf");

    @Override
    protected RouteBuilder createRouteBuilder() throws Exception {

        return new UpdateDocumentConsumer(documentDataService);
    }



    @Test
    public void shouldNotCallDocumentDataServiceOnError() throws Exception {
        template.sendBody(endpoint,"");
        verifyNoInteractions(documentDataService);
    }

    @Test
    public void shouldCallDocumentDataService() throws Exception {
        doNothing().when(documentDataService)
                .updateDocument(documentUUID, DocumentStatus.UPLOADED, "oldfile.docx", "some.pdf");

        template.sendBody(endpoint,request);
        verify( documentDataService, times(1)).updateDocument(documentUUID, DocumentStatus.UPLOADED, "oldfile.docx", "some.pdf");
    }

}