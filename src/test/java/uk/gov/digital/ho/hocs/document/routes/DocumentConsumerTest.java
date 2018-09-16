package uk.gov.digital.ho.hocs.document.routes;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.digital.ho.hocs.document.dto.ProcessDocumentRequest;


@RunWith(MockitoJUnitRunner.class)
public class DocumentConsumerTest extends CamelTestSupport {

    private String endpoint = "direct://cs-dev-document-sqs";
    private String dlq = "mock:cs-dev-document-sqs-dlq";
    private String toEndpoint = "mock:malwarecheck";

    ObjectMapper mapper = new ObjectMapper();

    private ProcessDocumentRequest request = new ProcessDocumentRequest("someuuid", "/somecase/someuuid", "someLink");

    @Override
    protected RouteBuilder createRouteBuilder() throws Exception {
      return new DocumentConsumer(endpoint, dlq, 0,0,0,toEndpoint);
    }

    @Test
    public void shouldAddDocumentToMalwareQueueOnSuccess() throws Exception {
        MockEndpoint mockEndpoint = getMockEndpoint(toEndpoint);
        mockEndpoint.expectedMessageCount(1);
        template.sendBody(endpoint, mapper.writeValueAsString(request));
        mockEndpoint.assertIsSatisfied();
    }

    @Test
    public void shouldAddMessagetoDLQOnError() throws Exception {
        getMockEndpoint(dlq).expectedMessageCount(1);
        template.sendBody(endpoint, "BAD BODY");
        getMockEndpoint(dlq).assertIsSatisfied();
    }

}