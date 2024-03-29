package uk.gov.digital.ho.hocs.document.routes;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.digital.ho.hocs.document.DocumentDataService;
import uk.gov.digital.ho.hocs.document.dto.camel.ProcessDocumentRequest;
import uk.gov.digital.ho.hocs.document.model.DocumentData;
import uk.gov.digital.ho.hocs.document.model.DocumentStatus;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class DocumentConsumerTest extends CamelTestSupport {

    private String endpoint = "direct://cs-dev-document-sqs";

    private String toEndpoint = "mock:malwarecheck";

    private UUID documentUUID = UUID.randomUUID();

    ObjectMapper mapper = new ObjectMapper();

    @Mock
    DocumentDataService documentDataService;

    private ProcessDocumentRequest request = new ProcessDocumentRequest(documentUUID.toString(), "someLink", "PDF");

    @Override
    protected RouteBuilder createRouteBuilder() throws Exception {
        return new DocumentConsumer(documentDataService, endpoint, toEndpoint);
    }

    @Test
    public void shouldAddDocumentToMalwareQueueOnSuccess() throws Exception {
        UUID externalReferenceUUID = UUID.randomUUID();
        UUID uploadOwnerUUID = UUID.randomUUID();

        when(documentDataService.getDocumentData(any(String.class))).thenReturn(
            new DocumentData(externalReferenceUUID, "ORIGINAL", "SomeDisplayName", uploadOwnerUUID));

        getMockEndpoint(toEndpoint).expectedMessageCount(1);
        template.sendBody(endpoint, mapper.writeValueAsString(request));
        getMockEndpoint(toEndpoint).assertIsSatisfied();

        verify(documentDataService).updateDocument(any(), eq(DocumentStatus.AWAITING_MALWARE_SCAN));
    }

    @Test
    public void shouldNotAddDocumentToMalwareQueueOnError() throws Exception {
        getMockEndpoint(toEndpoint).expectedMessageCount(0);
        template.sendBody(endpoint, "BAD BODY");
        getMockEndpoint(toEndpoint).assertIsSatisfied();

        verify(documentDataService, times(0)).updateDocument(any(), eq(DocumentStatus.AWAITING_MALWARE_SCAN));
    }

    @Test
    public void shouldAddPropertiesToExchange() throws Exception {

        UUID externalReferenceUUID = UUID.randomUUID();
        UUID uploadOwnerUUID = UUID.randomUUID();

        when(documentDataService.getDocumentData(any(String.class))).thenReturn(
            new DocumentData(externalReferenceUUID, "ORIGINAL", "SomeDisplayName", uploadOwnerUUID));
        MockEndpoint mockEndpoint = getMockEndpoint(toEndpoint);
        mockEndpoint.expectedPropertyReceived("externalReferenceUUID", externalReferenceUUID.toString());
        mockEndpoint.expectedPropertyReceived("uuid", documentUUID.toString());
        mockEndpoint.expectedMessageCount(1);
        template.sendBody(endpoint, mapper.writeValueAsString(request));
        mockEndpoint.assertIsSatisfied();
    }

}
