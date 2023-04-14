package uk.gov.digital.ho.hocs.document.routes;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.PurgeQueueRequest;
import com.amazonaws.services.sqs.model.QueueAttributeName;
import com.amazonaws.services.sqs.model.SetQueueAttributesRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.RestTemplate;
import uk.gov.digital.ho.hocs.document.DocumentDataService;
import uk.gov.digital.ho.hocs.document.dto.CreateDocumentRequest;
import uk.gov.digital.ho.hocs.document.model.DocumentConversionExemptTypes;
import uk.gov.digital.ho.hocs.document.model.DocumentStatus;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.configureFor;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles({ "local" })
public class DocumentConsumerIT {

    private static final String PDF = "PDF";
    private static final String ORIGINAL = "ORIGINAL";
    @Autowired
    private DocumentDataService documentService;
    @Value("${docs.untrustedS3bucketName}")
    private String untrustedBucketName;
    @Value("${docs.trustedS3bucketName}")
    private String trustedBucketName;
    @Autowired
    ObjectMapper mapper;
    @Autowired
    RestTemplate restTemplate;
    @Autowired
    AmazonSQS sqsClient;
    private static String documentQueue = "http://localhost:4566/000000000000/document-queue";
    private static String auditQueue = "http://localhost:4566/000000000000/audit-queue";
    @Autowired
    @Qualifier("UnTrusted")
    AmazonS3 untrustedClient;
    @Autowired
    @Qualifier("Trusted")
    AmazonS3 trustedClient;
    @LocalServerPort
    private int port;
    private static WireMockServer wireMockServer = new WireMockServer(wireMockConfig().port(9002));
    private static final String APPROXIMATE_NUMBER_OF_MESSAGES = "ApproximateNumberOfMessages";
    private final String filename = "someUUID.docx";
    private final String originalFilename = "sample.docx";
    private static final UUID EXTERNAL_REFERENCE_UUID = UUID.fromString("41d6f4d5-9bee-4b1c-b01c-35f5f3899f7c");
    private static final UUID USER_ID = UUID.fromString("d030c101-3ff6-43d7-9b6c-9cd54ccf5529");
    private HttpHeaders headers = new HttpHeaders();

    @BeforeClass
    public static void setup() {
        configureFor("localhost", 9002);
        wireMockServer.start();
    }

    @Before
    public void before() throws URISyntaxException, IOException {
        uploadUntrustedFiles();
        purgeTrustedBucket();
        headers.add("X-Auth-UserId", USER_ID.toString());
        headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        wireMockServer.resetAll();
        setQueueRetryPolicy();
        sqsClient.purgeQueue(new PurgeQueueRequest(documentQueue));
        sqsClient.purgeQueue(new PurgeQueueRequest(auditQueue));
    }

    @AfterClass
    public static void after() {
        wireMockServer.stop();
    }

    @Test
    public void shouldCallMalwareAndConverterServiceAndSetUploadedStatus() throws Exception {
        runSuccessfulConversion();
        //verify malware scan and conversion requests were made
        verify(1, postRequestedFor(urlEqualTo("/scan")));
        verify(1, postRequestedFor(urlEqualTo("/convert")));
    }

    @Test
    public void shouldCallMalwareForStandardLineButNotConverterService() throws Exception {
        stubFor(post(urlEqualTo("/scan")).willReturn(
            aResponse().withStatus(200).withHeader("Content-Type", "application/json").withBody(
                "Everything ok : true")));

        //create document
        ResponseEntity<UUID> response = createDocumentRequest(DocumentConversionExemptTypes.STANDARD_LINE.getDisplayValue(), PDF);

        // wait for document to be processed and status updated
        await().until(() -> documentService.getDocumentData(
            response.getBody().toString()).getStatus() == DocumentStatus.UPLOADED);

        verify(1, postRequestedFor(urlEqualTo("/scan")));
        verify(0, postRequestedFor(urlEqualTo("/convert")));
    }

    @Test
    public void shouldCallMalwareForTemplateAndNotConverterService() throws Exception {
        stubFor(post(urlEqualTo("/scan")).willReturn(
            aResponse().withStatus(200).withHeader("Content-Type", "application/json").withBody(
                "Everything ok : true")));

        //create document
        ResponseEntity<UUID> response = createDocumentRequest(DocumentConversionExemptTypes.TEMPLATE.getDisplayValue(), PDF);

        // document created in database
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        // wait for document to be processed and status updated
        await().until(() -> documentService.getDocumentData(
            response.getBody().toString()).getStatus() == DocumentStatus.UPLOADED);

        verify(1, postRequestedFor(urlEqualTo("/scan")));
        verify(0, postRequestedFor(urlEqualTo("/convert")));
    }

    @Test
    public void shouldCallMalwareAndNotConverterServiceWhereConvertToIsNONE() throws Exception {
        stubFor(post(urlEqualTo("/scan")).willReturn(
            aResponse().withStatus(200).withHeader("Content-Type", "application/json").withBody(
                "Everything ok : true")));

        //create document
        ResponseEntity<UUID> response = createDocumentRequest(ORIGINAL, "NONE");

        // document created in database
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        // wait for document to be processed and status updated
        await().until(() -> documentService.getDocumentData(
            response.getBody().toString()).getStatus() == DocumentStatus.UPLOADED);

        verify(1, postRequestedFor(urlEqualTo("/scan")));
        verify(0, postRequestedFor(urlEqualTo("/convert")));
    }

    @Test
    public void shouldSetStatusAndNotCallConversionServiceWhenMalwareCheckFailsWith500() throws Exception {

        stubFor(post(urlEqualTo("/scan")).willReturn(
            aResponse().withStatus(500).withHeader("Content-Type", "application/json").withBody("")));

        ResponseEntity<UUID> response = createDocumentRequest(ORIGINAL, PDF);

        //wait for documented status to be set
        await().until(() -> documentService.getDocumentData(
            response.getBody().toString()).getStatus() == DocumentStatus.FAILED_MALWARE_SCAN);

        verify(1, postRequestedFor(urlEqualTo("/scan")));
        verify(0, postRequestedFor(urlEqualTo("/convert")));
    }

    @Test
    public void shouldSetStatusAndNotCallConversionServiceWhenMalwareFound() throws Exception {

        stubFor(post(urlEqualTo("/scan")).willReturn(
            aResponse().withStatus(200).withHeader("Content-Type", "application/json").withBody(
                "Everything ok : false")));

        ResponseEntity<UUID> response = createDocumentRequest(ORIGINAL, PDF);

        //wait for documented status to be set
        await().until(() -> documentService.getDocumentData(
            response.getBody().toString()).getStatus() == DocumentStatus.FAILED_VIRUS);

        verify(1, postRequestedFor(urlEqualTo("/scan")));
        verify(0, postRequestedFor(urlEqualTo("/convert")));
    }

    @Test
    public void shouldNotCopyFilesToTrustedBucketWhenMalwareFound() throws Exception {

        stubFor(post(urlEqualTo("/scan")).willReturn(
            aResponse().withStatus(200).withHeader("Content-Type", "application/json").withBody(
                "Everything ok : false")));

        ResponseEntity<UUID> response = createDocumentRequest(ORIGINAL, PDF);

        //wait for documented status to be set
        await().until(() -> documentService.getDocumentData(
            response.getBody().toString()).getStatus() == DocumentStatus.FAILED_VIRUS);

        assertThat(
            trustedClient.listObjectsV2(trustedBucketName, EXTERNAL_REFERENCE_UUID.toString()).getKeyCount()).isEqualTo(
            0);

        verify(1, postRequestedFor(urlEqualTo("/scan")));
        verify(0, postRequestedFor(urlEqualTo("/convert")));
    }

    @Test
    public void shouldRetryWhenMalwareCheckFailsWith500() throws Exception {
        wireMockServer.resetAll();

        // stub malware check to fail first time and succeed second time
        stubFor(
            post(urlEqualTo("/scan")).inScenario("malware failure").whenScenarioStateIs(Scenario.STARTED).willReturn(
                aResponse().withStatus(500).withHeader("Content-Type", "application/json").withBody("")).willSetStateTo(
                "malware failure"));

        stubFor(
            post(urlEqualTo("/scan")).inScenario("malware failure").whenScenarioStateIs("malware failure").willReturn(
                aResponse().withStatus(200).withHeader("Content-Type", "application/json").withBody(
                    "Everything ok : true")));

        // stub conversion to succeed
        stubFor(post(urlEqualTo("/convert")).willReturn(
            aResponse().withStatus(200).withHeader("Content-Type", "application/pdf").withBody(getPDFDocument())));

        ResponseEntity<UUID> response = createDocumentRequest(ORIGINAL, PDF);

        //wait for documented status to be set to malware failure and then uploaded
        await().until(() -> documentService.getDocumentData(
            response.getBody().toString()).getStatus() == DocumentStatus.FAILED_MALWARE_SCAN);
        await().atMost(20, TimeUnit.SECONDS).until(() -> documentService.getDocumentData(
            response.getBody().toString()).getStatus() == DocumentStatus.UPLOADED);

        // verify malware scan and conversion requests were made
        verify(2, postRequestedFor(urlEqualTo("/scan")));
        verify(1, postRequestedFor(urlEqualTo("/convert")));
    }

    @Test
    public void shouldUpdateStatusAndNotRetryWhenConversionFailsWith400() throws Exception {

        stubFor(post(urlEqualTo("/scan")).willReturn(
            aResponse().withStatus(200).withHeader("Content-Type", "application/json").withBody(
                "Everything ok : true")));

        stubFor(post(urlEqualTo("/convert")).willReturn(
            aResponse().withStatus(400).withHeader("Content-Type", "application/json").withBody("")));

        ResponseEntity<UUID> response = createDocumentRequest(ORIGINAL, PDF);

        await().until(() -> documentService.getDocumentData(
            response.getBody().toString()).getStatus() == DocumentStatus.FAILED_CONVERSION);

        verify(1, postRequestedFor(urlEqualTo("/scan")));
        verify(1, postRequestedFor(urlEqualTo("/convert")));
    }

    @Test
    public void shouldCopyOriginalFileToTrustedBucketOnConversionFailure() throws Exception {

        stubFor(post(urlEqualTo("/scan")).willReturn(
            aResponse().withStatus(200).withHeader("Content-Type", "application/json").withBody(
                "Everything ok : true")));

        stubFor(post(urlEqualTo("/convert")).willReturn(
            aResponse().withStatus(400).withHeader("Content-Type", "application/json").withBody("")));

        ResponseEntity<UUID> response = createDocumentRequest(ORIGINAL, PDF);

        await().until(() -> documentService.getDocumentData(
            response.getBody().toString()).getStatus() == DocumentStatus.FAILED_CONVERSION);

        // assert original file is uploaded
        String docxKey = getKeyFromExtension("docx");
        assertThat(trustedClient.doesObjectExist(trustedBucketName, docxKey)).isTrue();

        verify(1, postRequestedFor(urlEqualTo("/scan")));
        verify(1, postRequestedFor(urlEqualTo("/convert")));
    }

    @Test
    public void shouldUpdateStatusAndRetryOnConversionFailure500() throws Exception {

        stubFor(post(urlEqualTo("/scan")).willReturn(
            aResponse().withStatus(200).withHeader("Content-Type", "application/json").withBody(
                "Everything ok : true")));

        // stub conversion to fail first time and succeed second time
        stubFor(post(urlEqualTo("/convert")).inScenario("conversion failure").whenScenarioStateIs(
            Scenario.STARTED).willReturn(
            aResponse().withStatus(500).withHeader("Content-Type", "application/json").withBody("")).willSetStateTo(
            "conversion failure"));

        stubFor(post(urlEqualTo("/convert")).inScenario("conversion failure").whenScenarioStateIs(
            "conversion failure").willReturn(
            aResponse().withStatus(200).withHeader("Content-Type", "application/pdf").withBody(getPDFDocument())));

        ResponseEntity<UUID> response = createDocumentRequest(ORIGINAL, PDF);

        // wait for document status to be set to conversion failure and then uploaded
        await().until(() -> documentService.getDocumentData(
            response.getBody().toString()).getStatus() == DocumentStatus.FAILED_CONVERSION);
        await().atMost(20, TimeUnit.SECONDS).until(() -> documentService.getDocumentData(
            response.getBody().toString()).getStatus() == DocumentStatus.UPLOADED);

        verify(2, postRequestedFor(urlEqualTo("/scan")));
        verify(2, postRequestedFor(urlEqualTo("/convert")));
    }

    @Test
    public void shouldUploadOriginalDocumentAndConvertedPDFAfterMalwareScan() throws Exception {
        runSuccessfulConversion();
        String pdfKey = getKeyFromExtension("pdf");
        String docxKey = getKeyFromExtension("docx");

        assertThat(trustedClient.doesObjectExist(trustedBucketName, pdfKey)).isTrue();
        assertThat(trustedClient.doesObjectExist(trustedBucketName, docxKey)).isTrue();
    }

    @Test
    public void shouldAddMetaDataToPDFAndOriginalFile() throws Exception {
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
    public void shouldAuditOnSuccess() throws Exception {
        runSuccessfulConversion();
        await().until(() -> getNumberOfMessagesOnQueue(auditQueue) == 3);
    }

    @Test
    public void shouldAuditOnMalwareFailure() throws Exception {
        stubFor(post(urlEqualTo("/scan")).willReturn(
            aResponse().withStatus(500).withHeader("Content-Type", "application/json")));

        ResponseEntity<UUID> response = createDocumentRequest(ORIGINAL, PDF);

        await().until(() -> documentService.getDocumentData(
            response.getBody().toString()).getStatus() == DocumentStatus.FAILED_MALWARE_SCAN);
        await().until(() -> getNumberOfMessagesOnQueue(auditQueue) == 2);
    }

    @Test
    public void shouldAuditOnConversionFailure() throws Exception {
        stubFor(post(urlEqualTo("/scan")).willReturn(
            aResponse().withStatus(200).withHeader("Content-Type", "application/json").withBody(
                "Everything ok : true")));

        stubFor(post(urlEqualTo("/convert")).willReturn(
            aResponse().withStatus(400).withHeader("Content-Type", "application/json").withBody("")));

        ResponseEntity<UUID> response = createDocumentRequest(ORIGINAL, PDF);

        await().until(() -> documentService.getDocumentData(
            response.getBody().toString()).getStatus() == DocumentStatus.FAILED_CONVERSION);

        await().until(() -> getNumberOfMessagesOnQueue(auditQueue) == 3);
    }

    private String getKeyFromExtension(String extension) {
        return trustedClient.listObjectsV2(trustedBucketName).getObjectSummaries().stream().filter(
            s -> (s.getKey().endsWith(extension) && (s.getKey().startsWith(
                EXTERNAL_REFERENCE_UUID.toString())))).findFirst().get().getKey();
    }

    private void runSuccessfulConversion() throws IOException, URISyntaxException {

        stubFor(post(urlEqualTo("/scan")).willReturn(
            aResponse().withStatus(200).withHeader("Content-Type", "application/json").withBody(
                "Everything ok : true")));
        stubFor(post(urlEqualTo("/convert")).willReturn(
            aResponse().withStatus(200).withHeader("Content-Type", "application/pdf").withBody(getPDFDocument())));

        ResponseEntity<UUID> response = createDocumentRequest(ORIGINAL, PDF);

        //wait for documented to be converted and uploadeed to trusted bucket
        await().until(() -> documentService.getDocumentData(
            response.getBody().toString()).getStatus() == DocumentStatus.UPLOADED);
    }

    private ResponseEntity<UUID> createDocumentRequest(String documentType, String convertTo) {
        //create document
        CreateDocumentRequest requestBody = new CreateDocumentRequest(EXTERNAL_REFERENCE_UUID, "some document",
            filename, documentType, convertTo);
        HttpEntity<CreateDocumentRequest> documentReqBody = new HttpEntity<CreateDocumentRequest>(requestBody, headers);
        ResponseEntity<UUID> response = restTemplate.postForEntity("http://localhost:" + port + "/document",
            documentReqBody, UUID.class);

        // document created in database
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return response;
    }

    private int getNumberOfMessagesOnQueue(String queueName) {
        return getValueFromQueue(queueName, APPROXIMATE_NUMBER_OF_MESSAGES);
    }

    private int getValueFromQueue(String queue, String attribute) {
        var queueAttributes = sqsClient.getQueueAttributes(queue, List.of(attribute));
        var messageCount = queueAttributes.getAttributes().get(attribute);
        return messageCount == null ? 0 : Integer.parseInt(messageCount);
    }

    private void setQueueRetryPolicy() {
        SetQueueAttributesRequest attributesRequest = new SetQueueAttributesRequest().withQueueUrl(
            documentQueue).addAttributesEntry(QueueAttributeName.RedrivePolicy.toString(),
            "{\"deadLetterTargetArn\":\"arn:aws:sqs:eu-west-2:000000000000:document-queue-dlq\", \"maxReceiveCount\":2}");
        sqsClient.setQueueAttributes(attributesRequest);
    }

    private byte[] getDocumentByteArray() throws URISyntaxException, IOException {
        return Files.readAllBytes(
            Paths.get(this.getClass().getClassLoader().getResource("testdata/sample.docx").toURI()));
    }

    private byte[] getPDFDocument() throws URISyntaxException, IOException {
        return Files.readAllBytes(
            Paths.get(this.getClass().getClassLoader().getResource("testdata/sample.pdf").toURI()));
    }

    private void uploadUntrustedFiles() throws URISyntaxException, IOException {

        if (untrustedClient.doesObjectExist(untrustedBucketName, filename)) {
            ObjectMetadata metaData = new ObjectMetadata();
            metaData.setContentType("application/docx");
            metaData.addUserMetadata("originalName", originalFilename);
            metaData.addUserMetadata("filename", filename);
            untrustedClient.putObject(
                new PutObjectRequest(untrustedBucketName, filename, new ByteArrayInputStream(getDocumentByteArray()),
                    metaData));
        }
    }

    private void purgeTrustedBucket() {
        trustedClient.listObjectsV2(trustedBucketName, EXTERNAL_REFERENCE_UUID.toString()).getObjectSummaries().forEach(
            s -> trustedClient.deleteObject(trustedBucketName, s.getKey()));
    }

}
