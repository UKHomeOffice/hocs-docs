package uk.gov.digital.ho.hocs.document.aws;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.*;
import org.apache.camel.test.spring.CamelSpringBootRunner;
import org.junit.*;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.utility.DockerImageName;
import uk.gov.digital.ho.hocs.document.application.LogEvent;
import uk.gov.digital.ho.hocs.document.dto.camel.DocumentCopyRequest;
import uk.gov.digital.ho.hocs.document.dto.camel.S3Document;
import uk.gov.digital.ho.hocs.document.dto.camel.UploadDocument;
import uk.gov.digital.ho.hocs.document.exception.ApplicationExceptions;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.S3;

@ActiveProfiles("test")
@SpringBootTest
@RunWith(CamelSpringBootRunner.class)
public class S3DocumentServiceTest {

    @ClassRule
    public static LocalStackContainer localStackContainer = new LocalStackContainer(DockerImageName.parse("quay.io/ukhomeofficedigital/localstack:latest"))
            .withServices(S3);

    @TestConfiguration
    static class AwsTestConfig {

        @Bean({"Trusted","UnTrusted"})
        public AmazonS3 amazonS3() {
            return AmazonS3ClientBuilder.standard()
                    .withCredentials(localStackContainer.getDefaultCredentialsProvider())
                    .withEndpointConfiguration(localStackContainer.getEndpointConfiguration(S3))
                    .build();
        }

    }

    @Autowired
    @Qualifier("UnTrusted")
    AmazonS3 untrustedClient;

    @Autowired
    @Qualifier("Trusted")
    AmazonS3 trustedClient;

    @Value("${docs.untrustedS3bucketName}")
    private String untrustedBucketName;
    @Value("${docs.trustedS3bucketName}")
    private String trustedBucketName;

    @Autowired
    private S3DocumentService service;

    @Before
    public void setUp() throws URISyntaxException, IOException {
        clearS3Buckets();
        uploadUntrustedFiles();
    }

    @Test
    public void shouldGetOriginalUploadedFileFromS3() throws IOException, URISyntaxException {
        S3Document document = service.getFileFromUntrustedS3("someUUID.docx");
        byte[] originalUploadedDocument = getDocumentByteArray();
        assertThat(document.getData()).isEqualTo(originalUploadedDocument);
    }

    @Test
    public void shouldReturnUploadedMetaData() throws IOException {
        S3Document document = service.getFileFromUntrustedS3("someUUID.docx");
        assertThat(document.getOriginalFilename()).isEqualTo("sample.docx");
        assertThat(document.getFilename()).isEqualTo("someUUID.docx");
        assertThat(document.getFileType()).isEqualTo("docx");
    }

    @Test
    public void shouldThrowNotFoundExceptionWhenFileNotInUntrustedBucket() {
        assertThatThrownBy(() -> service.getFileFromUntrustedS3("a missing file.ext"))
                .isInstanceOf(ApplicationExceptions.S3Exception.class)
                .hasMessage("File not found in S3 bucket")
                .hasFieldOrPropertyWithValue("event", LogEvent.S3_FILE_NOT_FOUND);
    }

    @Test
    public void shouldThrowExceptionWhenAnyOtherErrorOccurs() {
        AmazonS3 failingClient = Mockito.mock(AmazonS3.class);
        AmazonS3Exception S3500Exception = new AmazonS3Exception("something went wrong");
        S3500Exception.setStatusCode(500);
        when(failingClient.getObject(any())).thenThrow(S3500Exception);
        S3DocumentService badService = new S3DocumentService(untrustedBucketName, trustedBucketName, failingClient, failingClient, "");
        assertThatThrownBy(() -> badService.getFileFromUntrustedS3("a bad file"))
                .isInstanceOf(ApplicationExceptions.S3Exception.class)
                .hasFieldOrPropertyWithValue("event", LogEvent.S3_DOWNLOAD_FAILURE);
    }

    @Test
    public void shouldThrowNotFoundExceptionWhenFileNotInTrustedBucket() {
        assertThatThrownBy(() -> service.getFileFromTrustedS3("a missing file.ext"))
                .isInstanceOf(ApplicationExceptions.S3Exception.class)
                .hasMessage("File not found in S3 bucket")
                .hasFieldOrPropertyWithValue("event", LogEvent.S3_FILE_NOT_FOUND);
    }

    @Test
    public void shouldCopyToTrustedBucket() throws IOException {

        assertThat(untrustedClient.listObjectsV2(trustedBucketName).getKeyCount()).isEqualTo(0);
        DocumentCopyRequest copyRequest = new DocumentCopyRequest("someUUID.docx","someCase", "docx", "PDF");
        S3Document document = service.copyToTrustedBucket(copyRequest);
        assertThat(trustedClient.doesObjectExist(trustedBucketName, document.getFilename())).isTrue();
    }

    @Test
    public void shouldSetMetaDataWhenCopyToTrustedBucket() throws IOException {

        DocumentCopyRequest copyRequest = new DocumentCopyRequest("someUUID.docx","someCase", "docx", "PDF");
        S3Document document = service.copyToTrustedBucket(copyRequest);

        ObjectMetadata metadata = trustedClient.getObjectMetadata(trustedBucketName,document.getFilename());
        assertThat(metadata.getContentType()).isEqualTo("application/docx");
        assertThat(metadata.getUserMetaDataOf("filename")).startsWith("someCase");
        assertThat(metadata.getUserMetaDataOf("originalName")).isEqualTo("sample.docx");
    }

    @Test
    public void shouldUploadToTrustedBucket() throws IOException, URISyntaxException {

        assertThat(trustedClient.listObjectsV2(trustedBucketName).getKeyCount()).isEqualTo(0);
        UploadDocument uploadRequest = new UploadDocument("someUUID.docx", getPDFDocument(),"someCase", "sample.docx");
        S3Document document = service.uploadFile(uploadRequest);
        assertThat(trustedClient.doesObjectExist(trustedBucketName, document.getFilename())).isTrue();
    }

    public void uploadUntrustedFiles() throws URISyntaxException, IOException {
        ObjectMetadata metaData = new ObjectMetadata();
        metaData.setContentType("application/docx");
        metaData.addUserMetadata("originalName", "sample.docx");
        metaData.addUserMetadata("filename", "someUUID.docx");
        untrustedClient.putObject(new PutObjectRequest(untrustedBucketName, "someUUID.docx", new ByteArrayInputStream(getDocumentByteArray()), metaData));
    }

    private byte[] getPDFDocument() throws URISyntaxException, IOException {
        return Files.readAllBytes(Paths.get(this.getClass().getClassLoader().getResource("testdata/sample.pdf").toURI()));
    }

    private byte[] getDocumentByteArray() throws URISyntaxException, IOException {
        return Files.readAllBytes(Paths.get(this.getClass().getClassLoader().getResource("testdata/sample.docx").toURI()));
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
