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
import uk.gov.digital.ho.hocs.document.application.RequestData;
import uk.gov.digital.ho.hocs.document.dto.camel.ProcessDocumentRequest;
import uk.gov.digital.ho.hocs.document.model.DocumentData;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;


@RunWith(MockitoJUnitRunner.class)
public class DocumentConsumerTest extends CamelTestSupport {

    private String endpoint = "direct://cs-dev-document-sqs";
    private String toEndpoint = "mock:malwarecheck";
    private UUID documentUUID = UUID.randomUUID();
    private String userId = "user123";
    private String correlationID = "correlationId321";


    ObjectMapper mapper = new ObjectMapper();

    @Mock
    DocumentDataService documentDataService;
    @Mock
    RequestData requestData;

    private ProcessDocumentRequest request = new ProcessDocumentRequest(documentUUID.toString(), "someLink", "PDF", userId, correlationID);

    @Override
    protected RouteBuilder createRouteBuilder() throws Exception {
      return new DocumentConsumer(documentDataService, endpoint, toEndpoint, requestData);
    }

    @Test
    public void shouldAddDocumentToMalwareQueueOnSuccess() throws Exception {
        UUID externalReferenceUUID = UUID.randomUUID();

        when(documentDataService.getDocumentData(any(String.class))).thenReturn(new DocumentData(externalReferenceUUID, "ORIGINAL", "SomeDisplayName"));

        getMockEndpoint(toEndpoint).expectedMessageCount(1);
        template.sendBody(endpoint, mapper.writeValueAsString(request));
        getMockEndpoint(toEndpoint).assertIsSatisfied();
    }

    @Test
    public void shouldNotAddDocumentToMalwareQueueOnError() throws Exception {
        getMockEndpoint(toEndpoint).expectedMessageCount(0);
        template.sendBody(endpoint, "BAD BODY");
        getMockEndpoint(toEndpoint).assertIsSatisfied();
    }

    @Test
    public void shouldAddPropertiesToExchange() throws Exception {

        UUID externalReferenceUUID = UUID.randomUUID();

        when(documentDataService.getDocumentData(any(String.class))).thenReturn(new DocumentData(externalReferenceUUID, "ORIGINAL", "SomeDisplayName"));
        MockEndpoint mockEndpoint = getMockEndpoint(toEndpoint);
        mockEndpoint.expectedPropertyReceived("externalReferenceUUID", externalReferenceUUID.toString());
        mockEndpoint.expectedPropertyReceived("uuid", documentUUID.toString());
        mockEndpoint.expectedMessageCount(1);
        template.sendBody(endpoint, mapper.writeValueAsString(request));
        mockEndpoint.assertIsSatisfied();
    }


}