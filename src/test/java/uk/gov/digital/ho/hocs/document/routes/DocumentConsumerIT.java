package uk.gov.digital.ho.hocs.document.routes;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.camel.EndpointInject;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.mock.MockEndpoint;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import uk.gov.digital.ho.hocs.document.model.Document;
import uk.gov.digital.ho.hocs.document.dto.ProcessDocumentRequest;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;


@Ignore
@TestPropertySource(locations="classpath:application-test.properties")
@ActiveProfiles("local")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class DocumentConsumerIT {


    @Autowired
    private ProducerTemplate template;

    private String endpoint = "direct://cs-dev-document-sqs";

    @Autowired
    ObjectMapper mapper;

    @EndpointInject(uri = "mock:direct:malwarecheck")
    MockEndpoint mock;


    private ProcessDocumentRequest request = new ProcessDocumentRequest("someuuid", "/somecase/someuuid", "someLink");


    @Test
    public void shouldCallCollaborators() throws Exception {

        mock.expectedMessageCount(1);
        template.sendBody(endpoint, mapper.writeValueAsString(request));
        mock.assertIsSatisfied();
           }

    private Document getTestDocument() throws URISyntaxException, IOException {
        byte[] data = Files.readAllBytes(Paths.get(this.getClass().getClassLoader().getResource("testdata/sample.docx").toURI()));

        return new Document("UUID", "sample.docx", data, "docx", "");

    }

}