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
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestTemplate;
import uk.gov.digital.ho.hocs.document.DocumentDataService;
import uk.gov.digital.ho.hocs.document.application.RequestData;
import uk.gov.digital.ho.hocs.document.dto.CreateDocumentRequest;
import uk.gov.digital.ho.hocs.document.dto.camel.ProcessDocumentRequest;
import uk.gov.digital.ho.hocs.document.model.DocumentData;
import uk.gov.digital.ho.hocs.document.model.DocumentStatus;

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
import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@RunWith(CamelSpringBootRunner.class)
@DisableJmx
public class DocumentConsumerIT {

    private static boolean setUpIsDone = false;

    @Autowired
    private ProducerTemplate template;

    @Autowired
    private DocumentDataService documentService;

    private String endpoint = "direct://cs-dev-document-sqs";

    @Value("${docs.untrustedS3bucketName}")
    private String untrustedBucketName;

    @Value("${docs.trustedS3bucketName}")
    private String trustedBucketName;

    @Autowired
    ObjectMapper mapper;

    @Autowired
    RestTemplate restTemplate;

    private int LOCAL_S3_PORT = 9003;

    @Autowired
    @Qualifier("UnTrusted")
    AmazonS3 untrustedClient;

    @Autowired
    @Qualifier("Trusted")
    AmazonS3 trustedClient;

    @LocalServerPort
    private int port;

    private static WireMockServer wireMockServer = new WireMockServer(wireMockConfig().port(9002));

    private String documentUUID;

    private String documentStandardLineUUID;

    private String documentTemplateUUID;

    private final String filename = "someUUID.docx";

    private final String originalFilename = "sample.docx";

    private ProcessDocumentRequest request;

    private ProcessDocumentRequest requestStandardLine;

    private ProcessDocumentRequest requestTemplate;

    private static final UUID EXTERNAL_REFERENCE_UUID = UUID.fromString("41d6f4d5-9bee-4b1c-b01c-35f5f3899f7c");

    private static final UUID USER_ID = UUID.fromString("d030c101-3ff6-43d7-9b6c-9cd54ccf5529");

    private HttpHeaders headers = new HttpHeaders();

    @Before
    public void setup() throws Exception {

        headers.add("X-Auth-UserId", USER_ID.toString());
        headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);

        CreateDocumentRequest requestBody = new CreateDocumentRequest(EXTERNAL_REFERENCE_UUID, "some document",
            "some fileName", "ORIGINAL", "PDF");
        HttpEntity<String> documentReqBody = new HttpEntity<>(mapper.writeValueAsString(requestBody), headers);
        ResponseEntity<UUID> documentRequest = restTemplate.postForEntity("http://localhost:" + port + "/document",
            documentReqBody, UUID.class);

        requestBody = new CreateDocumentRequest(EXTERNAL_REFERENCE_UUID, "some document", "some fileName",
            "STANDARD_LINE", "PDF");
        documentReqBody = new HttpEntity<>(mapper.writeValueAsString(requestBody), headers);
        ResponseEntity<UUID> documentStandardLineRequest = restTemplate.postForEntity(
            "http://localhost:" + port + "/document", documentReqBody, UUID.class);

        requestBody = new CreateDocumentRequest(EXTERNAL_REFERENCE_UUID, "some document", "some fileName", "TEMPLATE",
            "PDF");
        documentReqBody = new HttpEntity<>(mapper.writeValueAsString(requestBody), headers);
        ResponseEntity<UUID> documentTemplateRequest = restTemplate.postForEntity(
            "http://localhost:" + port + "/document", documentReqBody, UUID.class);

        if (documentRequest.getBody() == null || documentStandardLineRequest.getBody() == null || documentTemplateRequest.getBody() == null) {
            throw new RuntimeException("Test set up failure");
        }

        documentUUID = documentRequest.getBody().toString();
        documentStandardLineUUID = documentStandardLineRequest.getBody().toString();
        documentTemplateUUID = documentTemplateRequest.getBody().toString();
        request = new ProcessDocumentRequest(documentUUID, filename, "PDF");
        requestStandardLine = new ProcessDocumentRequest(documentStandardLineUUID, filename, "PDF");
        requestTemplate = new ProcessDocumentRequest(documentTemplateUUID, filename, "PDF");

        if (!setUpIsDone) {
            configureFor("localhost", 9002);
            wireMockServer.start();
            startMockS3Service();
            uploadUntrustedFiles();
            setUpIsDone = true;
        }
    }

    @AfterClass
    public static void after() {
        wireMockServer.stop();
    }

    @Test
    public void shouldCallMalwareAndConverterService() throws Exception {
        runSuccessfulConversion();
    }

    @Test
    public void shouldCallMalwareForStandardLineAndNotConverterService() throws Exception {
        runSuccessfulConversionForStandardLine();
    }

    @Test
    public void shouldCallMalwareForTemplateAndNotConverterService() throws Exception {
        runSuccessfulConversionForTemplate();
    }

    @Test
    public void shouldNotCallConversionServiceWhenMalwareCheckFails() throws Exception {
        wireMockServer.resetAll();

        stubFor(post(urlEqualTo("/scan")).willReturn(
            aResponse().withStatus(500).withHeader("Content-Type", "application/json").withBody("")));

        template.sendBodyAndHeaders(endpoint, mapper.writeValueAsString(request), getHeaders());

        verify(moreThanOrExactly(1), postRequestedFor(urlEqualTo("/scan")));
        verify(0, postRequestedFor(urlEqualTo("/convert")));
    }

    @Test
    public void shouldNotCallConversionServiceWhenMalwareFound() throws Exception {
        wireMockServer.resetAll();
        stubFor(post(urlEqualTo("/scan")).willReturn(
            aResponse().withStatus(200).withHeader("Content-Type", "application/json").withBody(
                "Everything ok : false")));
        template.sendBodyAndHeaders(endpoint, mapper.writeValueAsString(request), getHeaders());
        verify(1, postRequestedFor(urlEqualTo("/scan")));
        verify(0, postRequestedFor(urlEqualTo("/convert")));
    }

    @Test
    public void shouldNotCallConversionServiceIfDocumentTypeIsStandardLine() throws Exception {
        wireMockServer.resetAll();
        stubFor(post(urlEqualTo("/scan")).willReturn(
            aResponse().withStatus(200).withHeader("Content-Type", "application/json").withBody(
                "Everything ok : True")));
        template.sendBodyAndHeaders(endpoint, mapper.writeValueAsString(request), getHeaders());
        verify(1, postRequestedFor(urlEqualTo("/scan")));
        verify(0, postRequestedFor(urlEqualTo("/convert")));
    }

    @Test
    public void shouldNotRetryWhenMalwareCheckFails() throws Exception {
        wireMockServer.resetAll();

        stubFor(post(urlEqualTo("/scan")).willReturn(
            aResponse().withStatus(500).withHeader("Content-Type", "application/json").withBody("")));
        template.sendBodyAndHeaders(endpoint, mapper.writeValueAsString(request), getHeaders());
        verify(1, postRequestedFor(urlEqualTo("/scan")));
        verify(0, postRequestedFor(urlEqualTo("/convert")));
    }

    @Test
    public void shouldNotRetryWhenConversionFails() throws Exception {

        wireMockServer.resetAll();

        stubFor(post(urlEqualTo("/scan")).willReturn(
            aResponse().withStatus(200).withHeader("Content-Type", "application/json").withBody(
                "Everything ok : true")));

        stubFor(post(urlEqualTo("/convert")).willReturn(
            aResponse().withStatus(500).withHeader("Content-Type", "application/json").withBody("")));

        template.sendBodyAndHeaders(endpoint, mapper.writeValueAsString(request), getHeaders());
        verify(1, postRequestedFor(urlEqualTo("/scan")));
        verify(1, postRequestedFor(urlEqualTo("/convert")));
    }

    @Test
    public void shouldUploadOriginalDocumentAndConvertedPDFAfterMalwareScan() throws Exception {

        runSuccessfulConversion();
        assertThat(trustedClient.listObjectsV2(trustedBucketName).getKeyCount()).isEqualTo(2);
    }

    @Test
    public void shouldAddMetaDataToPDFandOriginalFile() throws Exception {
        runSuccessfulConversion();
        String pdfKey = getKeyFromExtension("pdf");
        String docxKey = getKeyFromExtension("docx");

        ObjectMetadata pdfMetadata = trustedClient.getObjectMetadata(trustedBucketName, pdfKey);
        assertThat(pdfMetadata.getContentType()).isEqualTo("application/pdf");
        assertThat(pdfMetadata.getUserMetaDataOf("externalReferenceUUID")).isEqualTo(
            EXTERNAL_REFERENCE_UUID.toString());
        assertThat(pdfMetadata.getUserMetaDataOf("originalName")).isEqualTo("sample.pdf");

        ObjectMetadata docxMetadata = trustedClient.getObjectMetadata(trustedBucketName, docxKey);
        assertThat(docxMetadata.getUserMetaDataOf("externalReferenceUUID")).isEqualTo(
            EXTERNAL_REFERENCE_UUID.toString());
        assertThat(docxMetadata.getUserMetaDataOf("originalName")).isEqualTo(originalFilename);
    }

    @Test
    public void shouldUpdateDocumentStatusInDatabaseOnSuccess() throws Exception {
        wireMockServer.resetAll();

        runSuccessfulConversion();

        DocumentData updatedDocument = documentService.getDocumentData(documentUUID);
        assertThat(updatedDocument.getStatus()).isEqualTo(DocumentStatus.UPLOADED);
    }

    @Test
    public void shouldUpdateDocumentStatusInDatabaseOnMalwareFound() throws Exception {

        wireMockServer.resetAll();

        stubFor(post(urlEqualTo("/scan")).willReturn(
            aResponse().withStatus(200).withHeader("Content-Type", "application/json").withBody(
                "Everything ok : false")));

        template.sendBodyAndHeaders(endpoint, mapper.writeValueAsString(request), getHeaders());
        verify(1, postRequestedFor(urlEqualTo("/scan")));

        DocumentData updatedDocument = documentService.getDocumentData(documentUUID);
        assertThat(updatedDocument.getStatus()).isEqualTo(DocumentStatus.FAILED_VIRUS);
    }

    @Test
    public void shouldUpdateDocumentStatusInDatabaseOnConversionFailure500() throws Exception {
        wireMockServer.resetAll();

        stubFor(post(urlEqualTo("/scan")).willReturn(
            aResponse().withStatus(200).withHeader("Content-Type", "application/json").withBody(
                "Everything ok : true")));

        stubFor(post(urlEqualTo("/convert")).willReturn(
            aResponse().withStatus(500).withHeader("Content-Type", "application/json").withBody("")));

        template.sendBodyAndHeaders(endpoint, mapper.writeValueAsString(request), getHeaders());
        verify(1, postRequestedFor(urlEqualTo("/scan")));
        verify(moreThanOrExactly(1), postRequestedFor(urlEqualTo("/convert")));

        DocumentData updatedDocument = documentService.getDocumentData(documentUUID);
        assertThat(updatedDocument.getStatus()).isEqualTo(DocumentStatus.FAILED_CONVERSION);
    }

    @Test
    public void shouldUpdateDocumentStatusInDatabaseOnConversionFailure400() throws Exception {
        wireMockServer.resetAll();

        stubFor(post(urlEqualTo("/scan")).willReturn(
            aResponse().withStatus(200).withHeader("Content-Type", "application/json").withBody(
                "Everything ok : true")));

        stubFor(post(urlEqualTo("/convert")).willReturn(
            aResponse().withStatus(400).withHeader("Content-Type", "application/json").withBody("")));

        template.sendBodyAndHeaders(endpoint, mapper.writeValueAsString(request), getHeaders());
        verify(1, postRequestedFor(urlEqualTo("/scan")));
        verify(moreThanOrExactly(1), postRequestedFor(urlEqualTo("/convert")));

        DocumentData updatedDocument = documentService.getDocumentData(documentUUID);
        assertThat(updatedDocument.getStatus()).isEqualTo(DocumentStatus.FAILED_CONVERSION);
    }

    private String getKeyFromExtension(String extension) {
        return trustedClient.listObjectsV2(trustedBucketName).getObjectSummaries().stream().filter(
            s -> (s.getKey().endsWith(extension) && (s.getKey().startsWith(
                EXTERNAL_REFERENCE_UUID.toString())))).findFirst().get().getKey();
    }

    private void runSuccessfulConversion() throws IOException, URISyntaxException {
        wireMockServer.resetAll();

        stubFor(post(urlEqualTo("/scan")).willReturn(
            aResponse().withStatus(200).withHeader("Content-Type", "application/json").withBody(
                "Everything ok : true")));

        stubFor(post(urlEqualTo("/convert")).willReturn(
            aResponse().withStatus(200).withHeader("Content-Type", "application/pdf").withBody(getPDFDocument())));

        template.sendBodyAndHeaders(endpoint, mapper.writeValueAsString(request), getHeaders());
        verify(1, postRequestedFor(urlEqualTo("/scan")));
        verify(1, postRequestedFor(urlEqualTo("/convert")));
    }

    private void runSuccessfulConversionForStandardLine() throws IOException, URISyntaxException {
        wireMockServer.resetAll();

        stubFor(post(urlEqualTo("/scan")).willReturn(
            aResponse().withStatus(200).withHeader("Content-Type", "application/json").withBody(
                "Everything ok : true")));

        template.sendBodyAndHeaders(endpoint, mapper.writeValueAsString(requestStandardLine), getHeaders());
        verify(1, postRequestedFor(urlEqualTo("/scan")));
        verify(0, postRequestedFor(urlEqualTo("/convert")));
    }

    private void runSuccessfulConversionForTemplate() throws IOException, URISyntaxException {
        wireMockServer.resetAll();

        stubFor(post(urlEqualTo("/scan")).willReturn(
            aResponse().withStatus(200).withHeader("Content-Type", "application/json").withBody(
                "Everything ok : true")));

        template.sendBody(endpoint, mapper.writeValueAsString(requestTemplate));
        verify(1, postRequestedFor(urlEqualTo("/scan")));
        verify(0, postRequestedFor(urlEqualTo("/convert")));
    }

    private byte[] getDocumentByteArray() throws URISyntaxException, IOException {
        return Files.readAllBytes(
            Paths.get(this.getClass().getClassLoader().getResource("testdata/sample.docx").toURI()));
    }

    private byte[] getPDFDocument() throws URISyntaxException, IOException {
        return Files.readAllBytes(
            Paths.get(this.getClass().getClassLoader().getResource("testdata/sample.pdf").toURI()));
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
        metaData.addUserMetadata("originalName", originalFilename);
        metaData.addUserMetadata("filename", filename);
        untrustedClient.putObject(
            new PutObjectRequest(untrustedBucketName, filename, new ByteArrayInputStream(getDocumentByteArray()),
                metaData));
    }

    Map<String, Object> getHeaders() {
        Map<String, Object> headers = new HashMap<>();
        headers.put(RequestData.CORRELATION_ID_HEADER, UUID.randomUUID());
        headers.put(RequestData.USER_ID_HEADER, USER_ID);
        headers.put(RequestData.USERNAME_HEADER, "some user");
        return headers;
    }

}
