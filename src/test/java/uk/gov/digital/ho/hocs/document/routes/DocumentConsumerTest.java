package uk.gov.digital.ho.hocs.document.routes;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.apache.http.entity.ContentType;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.util.MimeType;
import uk.gov.digital.ho.hocs.document.aws.S3DocumentService;
import uk.gov.digital.ho.hocs.document.dto.Document;
import uk.gov.digital.ho.hocs.document.dto.ProcessDocumentRequest;

import javax.activation.MimetypesFileTypeMap;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;


@RunWith(MockitoJUnitRunner.class)
public class DocumentConsumerTest extends CamelTestSupport {

    @Mock
    S3DocumentService s3BucketService;

    private String endpoint = "direct://cs-dev-document-sqs";
    private String dlq = "mock:cs-dev-document-sqs-dlq";
    private String toEndpoint = "mock:malwarecheck";

    ObjectMapper mapper = new ObjectMapper();

    private ProcessDocumentRequest request = new ProcessDocumentRequest("someuuid", "/somecase/someuuid");

    @Override
    protected RouteBuilder createRouteBuilder() throws Exception {
      return new DocumentConsumer(s3BucketService,endpoint, dlq, 0,0,0,toEndpoint);
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