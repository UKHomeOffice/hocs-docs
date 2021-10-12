package uk.gov.digital.ho.hocs.document.routes;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.amazonaws.services.sqs.AmazonSQSAsync;
import com.amazonaws.services.sqs.AmazonSQSAsyncClientBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.test.spring.CamelSpringBootRunner;
import org.junit.*;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.utility.DockerImageName;
import uk.gov.digital.ho.hocs.document.DocumentDataService;
import uk.gov.digital.ho.hocs.document.application.RequestData;
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
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.S3;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.SQS;


@ActiveProfiles("test")
@SpringBootTest
@RunWith(CamelSpringBootRunner.class)
public class DocumentConsumerIT {

    @ClassRule
    public static LocalStackContainer localStackContainer = new LocalStackContainer(DockerImageName.parse("localstack/localstack:latest"))
            .withServices(S3, SQS);

    @TestConfiguration
    static class AwsTestConfig {

        @Bean({"Trusted","UnTrusted"})
        public AmazonS3 amazonS3() {
            return AmazonS3ClientBuilder.standard()
                    .withCredentials(localStackContainer.getDefaultCredentialsProvider())
                    .withEndpointConfiguration(localStackContainer.getEndpointConfiguration(S3))
                    .build();
        }

        @Bean
        public AmazonSQSAsync amazonSQS() {
            return AmazonSQSAsyncClientBuilder.standard()
                    .withCredentials(localStackContainer.getDefaultCredentialsProvider())
                    .withEndpointConfiguration(localStackContainer.getEndpointConfiguration(SQS))
                    .build();
        }

    }

    private static boolean setUpIsDone = false;

    @Autowired
    private ProducerTemplate template;

    @Autowired
    private DocumentDataService documentService;

    @Value("${docs.queue}") private String endpoint;

    @Value("${docs.untrustedS3bucketName}") private String untrustedBucketName;
    @Value("${docs.trustedS3bucketName}") private String trustedBucketName;

    @Autowired
    ObjectMapper mapper;

    @Autowired
    @Qualifier("UnTrusted")
    AmazonS3 untrustedClient;

    @Autowired
    @Qualifier("Trusted")
    AmazonS3 trustedClient;

    private static WireMockServer wireMockServer = new WireMockServer(wireMockConfig().port(9002));

    private final String externalReferenceUUID = UUID.randomUUID().toString();
    private String documentUUID;
    private String documentStandardLineUUID;
    private String documentTemplateUUID;
    private final String filename = "someUUID.docx";
    private final String originalFilename = "sample.docx";

    private DocumentData document;
    private DocumentData documentStandardLine;
    private DocumentData documentTemplate;
    private ProcessDocumentRequest request ;
    private ProcessDocumentRequest requestStandardLine ;
    private ProcessDocumentRequest requestTemplate ;

    @Before
    public void setUp() throws URISyntaxException, IOException {
        clearS3Buckets();
        uploadUntrustedFiles();
    }

    @Before
    public void setup() throws Exception {
        document = documentService.createDocument(UUID.fromString(externalReferenceUUID), "some document", "some fileName", "ORIGINAL", "PDF");
        documentStandardLine = documentService.createDocument(UUID.fromString(externalReferenceUUID), "some document", "some fileName", "STANDARD_LINE", "PDF");
        documentTemplate = documentService.createDocument(UUID.fromString(externalReferenceUUID), "some document", "some fileName", "TEMPLATE", "PDF");
        documentUUID = document.getUuid().toString();
        documentStandardLineUUID = documentStandardLine.getUuid().toString();
        documentTemplateUUID = documentTemplate.getUuid().toString();
        request = new ProcessDocumentRequest(documentUUID, filename, "PDF");
        requestStandardLine = new ProcessDocumentRequest(documentStandardLineUUID, filename, "PDF");
        requestTemplate = new ProcessDocumentRequest(documentTemplateUUID, filename, "PDF");

        if(!setUpIsDone) {
            configureFor("localhost", 9002);
            wireMockServer.start();
            clearS3Buckets();
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

        stubFor(post(urlEqualTo("/scan"))
                .willReturn(aResponse().withStatus(500).withHeader("Content-Type", "application/json").withBody("")));

        template.sendBodyAndHeaders(endpoint, mapper.writeValueAsString(request), getHeaders());

        verify(moreThanOrExactly(1), postRequestedFor(urlEqualTo("/scan")));
        verify(0, postRequestedFor(urlEqualTo("/convert")));
    }

    @Test
    public void shouldNotCallConversionServiceWhenMalwareFound() throws Exception {
        wireMockServer.resetAll();
        stubFor(post(urlEqualTo("/scan"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json").withBody("Everything ok : false")));
        template.sendBodyAndHeaders(endpoint, mapper.writeValueAsString(request), getHeaders());
        verify(1, postRequestedFor(urlEqualTo("/scan")));
        verify(0, postRequestedFor(urlEqualTo("/convert")));
    }

    @Test
    public void shouldNotCallConversionServiceIfDocumentTypeIsStandardLine() throws Exception {
        wireMockServer.resetAll();
        stubFor(post(urlEqualTo("/scan"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json").withBody("Everything ok : True")));
        template.sendBodyAndHeaders(endpoint, mapper.writeValueAsString(request), getHeaders());
        verify(1, postRequestedFor(urlEqualTo("/scan")));
        verify(0, postRequestedFor(urlEqualTo("/convert")));
    }

    @Test
    public void shouldNotRetryWhenMalwareCheckFails() throws Exception {
        wireMockServer.resetAll();

        stubFor(post(urlEqualTo("/scan"))
                .willReturn(aResponse().withStatus(500).withHeader("Content-Type", "application/json").withBody("")));
        template.sendBodyAndHeaders(endpoint, mapper.writeValueAsString(request), getHeaders());
        verify(1, postRequestedFor(urlEqualTo("/scan")));
        verify(0, postRequestedFor(urlEqualTo("/convert")));
    }


    @Test
    public void shouldNotRetryWhenConversionFails() throws Exception {

        wireMockServer.resetAll();

        stubFor(post(urlEqualTo("/scan"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json").withBody("Everything ok : true")));

        stubFor(post(urlEqualTo("/convert"))
                .willReturn(aResponse().withStatus(500).withHeader("Content-Type", "application/json").withBody("")));

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
       assertThat(pdfMetadata.getUserMetaDataOf("externalReferenceUUID")).isEqualTo(externalReferenceUUID);
       assertThat(pdfMetadata.getUserMetaDataOf("originalName")).isEqualTo("sample.pdf");

        ObjectMetadata docxMetadata = trustedClient.getObjectMetadata(trustedBucketName, docxKey);
        assertThat(docxMetadata.getUserMetaDataOf("externalReferenceUUID")).isEqualTo(externalReferenceUUID);
        assertThat(docxMetadata.getUserMetaDataOf("originalName")).isEqualTo(originalFilename);
    }

    @Test
    public void shouldUpdateDocumentStatusInDatabaseOnSuccess() throws Exception {
        wireMockServer.resetAll();

        assertThat(document.getStatus()).isEqualTo(DocumentStatus.PENDING);

        runSuccessfulConversion();

        DocumentData updatedDocument = documentService.getDocumentData(document.getUuid());
        assertThat(updatedDocument.getStatus()).isEqualTo(DocumentStatus.UPLOADED);
    }

    @Test
    public void shouldUpdateDocumentStatusInDatabaseOnMalwareFound() throws Exception {
        wireMockServer.resetAll();

        assertThat(document.getStatus()).isEqualTo(DocumentStatus.PENDING);

        wireMockServer.resetAll();

        stubFor(post(urlEqualTo("/scan"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json").withBody("Everything ok : false")));

        template.sendBodyAndHeaders(endpoint, mapper.writeValueAsString(request), getHeaders());
        verify(1, postRequestedFor(urlEqualTo("/scan")));

        DocumentData updatedDocument = documentService.getDocumentData(document.getUuid());
        assertThat(updatedDocument.getStatus()).isEqualTo(DocumentStatus.FAILED_VIRUS);
    }

    @Test
    public void shouldUpdateDocumentStatusInDatabaseOnConversionFailure500() throws Exception {
        wireMockServer.resetAll();

        assertThat(document.getStatus()).isEqualTo(DocumentStatus.PENDING);

        wireMockServer.resetAll();

        stubFor(post(urlEqualTo("/scan"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json").withBody("Everything ok : true")));

        stubFor(post(urlEqualTo("/convert"))
                .willReturn(aResponse().withStatus(500).withHeader("Content-Type", "application/json").withBody("")));

        template.sendBodyAndHeaders(endpoint, mapper.writeValueAsString(request), getHeaders());
        verify(1, postRequestedFor(urlEqualTo("/scan")));
        verify(moreThanOrExactly(1), postRequestedFor(urlEqualTo("/convert")));

        DocumentData updatedDocument = documentService.getDocumentData(document.getUuid());
        assertThat(updatedDocument.getStatus()).isEqualTo(DocumentStatus.FAILED_CONVERSION);
    }

    @Test
    public void shouldUpdateDocumentStatusInDatabaseOnConversionFailure400() throws Exception {
        wireMockServer.resetAll();

        assertThat(document.getStatus()).isEqualTo(DocumentStatus.PENDING);

        wireMockServer.resetAll();

        stubFor(post(urlEqualTo("/scan"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json").withBody("Everything ok : true")));

        stubFor(post(urlEqualTo("/convert"))
                .willReturn(aResponse().withStatus(400).withHeader("Content-Type", "application/json").withBody("")));

        template.sendBodyAndHeaders(endpoint, mapper.writeValueAsString(request), getHeaders());
        verify(1, postRequestedFor(urlEqualTo("/scan")));
        verify(moreThanOrExactly(1), postRequestedFor(urlEqualTo("/convert")));

        DocumentData updatedDocument = documentService.getDocumentData(document.getUuid());
        assertThat(updatedDocument.getStatus()).isEqualTo(DocumentStatus.FAILED_CONVERSION);
    }


    private String getKeyFromExtension(String extension) {
        return trustedClient.listObjectsV2(trustedBucketName)
                .getObjectSummaries().stream().filter(s -> (s.getKey().endsWith(extension) && (s.getKey().startsWith(externalReferenceUUID))))
                .findFirst().get().getKey();
    }

    private void runSuccessfulConversion() throws IOException, URISyntaxException {
        wireMockServer.resetAll();

        stubFor(post(urlEqualTo("/scan"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json").withBody("Everything ok : true")));

        stubFor(post(urlEqualTo("/convert"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/pdf").withBody(getPDFDocument())));

        template.sendBodyAndHeaders(endpoint, mapper.writeValueAsString(request), getHeaders());
        verify(1, postRequestedFor(urlEqualTo("/scan")));
        verify(1, postRequestedFor(urlEqualTo("/convert")));
    }

    private void runSuccessfulConversionForStandardLine() throws IOException, URISyntaxException {
        wireMockServer.resetAll();

        stubFor(post(urlEqualTo("/scan"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json").withBody("Everything ok : true")));

        template.sendBodyAndHeaders(endpoint, mapper.writeValueAsString(requestStandardLine), getHeaders());
        verify(1, postRequestedFor(urlEqualTo("/scan")));
        verify(0, postRequestedFor(urlEqualTo("/convert")));
    }

    private void runSuccessfulConversionForTemplate() throws IOException, URISyntaxException {
        wireMockServer.resetAll();

        stubFor(post(urlEqualTo("/scan"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json").withBody("Everything ok : true")));

        template.sendBody(endpoint, mapper.writeValueAsString(requestTemplate));
        verify(1, postRequestedFor(urlEqualTo("/scan")));
        verify(0, postRequestedFor(urlEqualTo("/convert")));
    }


    private byte[] getDocumentByteArray() throws URISyntaxException, IOException {
        return Files.readAllBytes(Paths.get(this.getClass().getClassLoader().getResource("testdata/sample.docx").toURI()));
    }

    private byte[] getPDFDocument() throws URISyntaxException, IOException {
        return Files.readAllBytes(Paths.get(this.getClass().getClassLoader().getResource("testdata/sample.pdf").toURI()));
    }

    private void uploadUntrustedFiles() throws URISyntaxException, IOException {
        ObjectMetadata metaData = new ObjectMetadata();
        metaData.setContentType("application/docx");
        metaData.addUserMetadata("originalName", originalFilename);
        metaData.addUserMetadata("filename", filename);
        untrustedClient.putObject(new PutObjectRequest(untrustedBucketName, filename, new ByteArrayInputStream(getDocumentByteArray()), metaData));
    }

    Map<String, Object> getHeaders() {
        Map<String, Object> headers = new HashMap<>();
        headers.put(RequestData.CORRELATION_ID_HEADER, UUID.randomUUID());
        headers.put(RequestData.USER_ID_HEADER, UUID.randomUUID());
        headers.put(RequestData.USERNAME_HEADER, "some user");
        return headers;
    }

    private void clearS3Buckets() {
        if(untrustedClient.doesBucketExistV2(untrustedBucketName)) {
            ObjectListing objectListing = untrustedClient.listObjects(untrustedBucketName);
            for (S3ObjectSummary s3ObjectSummary : objectListing.getObjectSummaries()) {
                untrustedClient.deleteObject(untrustedBucketName, s3ObjectSummary.getKey());
            }
        } else {
            untrustedClient.createBucket(untrustedBucketName);
        }

        if(trustedClient.doesBucketExistV2(trustedBucketName)) {
            ObjectListing objectListing = trustedClient.listObjects(trustedBucketName);
            for (S3ObjectSummary s3ObjectSummary : objectListing.getObjectSummaries()) {
                trustedClient.deleteObject(trustedBucketName, s3ObjectSummary.getKey());
            }
        } else {
            trustedClient.createBucket(trustedBucketName);
        }

    }
}
