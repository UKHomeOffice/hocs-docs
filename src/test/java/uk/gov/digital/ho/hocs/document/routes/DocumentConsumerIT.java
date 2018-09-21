package uk.gov.digital.ho.hocs.document.routes;

import com.adobe.testing.s3mock.S3MockApplication;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.test.spring.CamelSpringBootRunner;
import org.apache.camel.test.spring.DisableJmx;
import org.junit.*;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.digital.ho.hocs.document.dto.ProcessDocumentRequest;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;


@ActiveProfiles("test")
@SpringBootTest
@RunWith(CamelSpringBootRunner.class)
@DisableJmx
public class DocumentConsumerIT {

    private static boolean setUpIsDone = false;

    @Autowired
    private ProducerTemplate template;

    private String endpoint = "direct://cs-dev-document-sqs";

    @Value("${docs.untrustedS3bucketName}") private String untrustedBucketName;
    @Value("${docs.trustedS3bucketName}") private String trustedBucketName;

    @Autowired
    ObjectMapper mapper;

    private int LOCAL_S3_PORT = 9001;

    @Autowired
    @Qualifier("UnTrusted")
    AmazonS3 untrustedClient;

    private ProcessDocumentRequest request = new ProcessDocumentRequest(UUID.randomUUID().toString(), UUID.randomUUID().toString(), "someUUID.docx");
    private static WireMockServer wireMockServer = new WireMockServer(wireMockConfig().port(9002));


    @Before
    public void setup() throws Exception {
        if(!setUpIsDone) {
            configureFor("localhost", 9002);
            wireMockServer.start();
            startMockS3Service();
            uploadUntrustedFiles();
            setUpIsDone =true;
        }
    }

    @AfterClass
    public static void after() {
        wireMockServer.stop();
    }

    @Test
    public void shouldCallMalwareAndConverterService() throws Exception {

        wireMockServer.resetAll();

             stubFor(post(urlEqualTo("/scan"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json").withBody("Everything ok : true")));

        stubFor(post(urlEqualTo("/uploadFile"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/pdf").withBody(getPDFDocument())));

        template.sendBody(endpoint, mapper.writeValueAsString(request));

        verify(1, postRequestedFor(urlEqualTo("/scan")));
        verify(1, postRequestedFor(urlEqualTo("/uploadFile")));
    }

    @Test
    public void shouldNotCallConversionServiceWhenMalwareCheckFails() throws Exception {
        wireMockServer.resetAll();

        stubFor(post(urlEqualTo("/scan"))
                .willReturn(aResponse().withStatus(500).withHeader("Content-Type", "application/json").withBody("")));

        stubFor(post(urlEqualTo("/scan"))
                .willReturn(aResponse().withStatus(500).withHeader("Content-Type", "application/json").withBody("")));

        stubFor(post(urlEqualTo("/scan"))
                .willReturn(aResponse().withStatus(500).withHeader("Content-Type", "application/json").withBody("")));

        template.sendBody(endpoint, mapper.writeValueAsString(request));

        verify(moreThanOrExactly(1), postRequestedFor(urlEqualTo("/scan")));
        verify(0, postRequestedFor(urlEqualTo("/uploadFile")));
    }

    @Test
    public void shouldNotCallConversionServiceWhenMalwareFound() throws Exception {
        wireMockServer.resetAll();
        stubFor(post(urlEqualTo("/scan"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json").withBody("Everything ok : false")));
        template.sendBody(endpoint, mapper.writeValueAsString(request));
        verify(1, postRequestedFor(urlEqualTo("/scan")));
        verify(0, postRequestedFor(urlEqualTo("/uploadFile")));
    }

    @Test
    public void shouldRetryWhenMalwareCheckFails() throws Exception {

        wireMockServer.resetAll();

        stubFor(post(urlEqualTo("/scan"))
                .willReturn(aResponse().withStatus(500).withHeader("Content-Type", "application/json").withBody("")));
        template.sendBody(endpoint, mapper.writeValueAsString(request));
        verify(3, postRequestedFor(urlEqualTo("/scan")));
        verify(0, postRequestedFor(urlEqualTo("/uploadFile")));
    }

    private byte[] getDocumentByteArray() throws URISyntaxException, IOException {
        return Files.readAllBytes(Paths.get(this.getClass().getClassLoader().getResource("testdata/sample.docx").toURI()));
    }

    private byte[] getPDFDocument() throws URISyntaxException, IOException {
        return Files.readAllBytes(Paths.get(this.getClass().getClassLoader().getResource("testdata/sample.pdf").toURI()));
    }

    private void startMockS3Service() {
        Map<String, Object> properties = new HashMap<>();
        properties.put(S3MockApplication.PROP_HTTP_PORT, LOCAL_S3_PORT);
        properties.put(S3MockApplication.PROP_SECURE_CONNECTION, false);
        properties.put(S3MockApplication.PROP_SILENT, false);
        properties.put(S3MockApplication.PROP_INITIAL_BUCKETS, trustedBucketName + ", " + untrustedBucketName);
        S3MockApplication.start(properties);
    }

    private void uploadUntrustedFiles() throws URISyntaxException, IOException {
        ObjectMetadata metaData = new ObjectMetadata();
        metaData.setContentType("application/docx");
        metaData.addUserMetadata("originalName", "sample.docx");
        metaData.addUserMetadata("filename", "someUUID.docx");
        untrustedClient.putObject(new PutObjectRequest(untrustedBucketName, "someUUID.docx", new ByteArrayInputStream(getDocumentByteArray()), metaData));
    }
}