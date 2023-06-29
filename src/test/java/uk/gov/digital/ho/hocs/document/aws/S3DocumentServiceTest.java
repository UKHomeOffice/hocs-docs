package uk.gov.digital.ho.hocs.document.aws;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import org.junit.*;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.digital.ho.hocs.document.application.LogEvent;
import uk.gov.digital.ho.hocs.document.dto.camel.DocumentCopyRequest;
import uk.gov.digital.ho.hocs.document.dto.camel.S3Document;
import uk.gov.digital.ho.hocs.document.dto.camel.S3DocumentMetaData;
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

@SpringBootTest
@RunWith(SpringRunner.class)
@ActiveProfiles("local")
public class S3DocumentServiceTest {

    @Value("${docs.untrustedS3bucketName}")
    private String untrustedBucketName;
    @Value("${docs.trustedS3bucketName}")
    private String trustedBucketName;

    @Autowired
    @Qualifier("UnTrusted")
    AmazonS3 untrustedClient;
    @Autowired
    @Qualifier("Trusted")
    AmazonS3 trustedClient;

    S3DocumentService service;

    @Before
    public void setUp() throws Exception {
        service = new S3DocumentService(untrustedBucketName, trustedBucketName, trustedClient, untrustedClient, "");
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
        assertThatThrownBy(() -> service.getFileFromUntrustedS3("a missing file.ext")).isInstanceOf(
            ApplicationExceptions.S3Exception.class).hasMessage(
            "File not found in S3 bucket").hasFieldOrPropertyWithValue("event", LogEvent.S3_FILE_NOT_FOUND);
    }

    @Test
    public void shouldThrowExceptionWhenAnyOtherErrorOccurs() {
        AmazonS3 failingClient = Mockito.mock(AmazonS3.class);
        AmazonS3Exception S3500Exception = new AmazonS3Exception("something went wrong");
        S3500Exception.setStatusCode(500);
        when(failingClient.getObject(any())).thenThrow(S3500Exception);
        S3DocumentService badService = new S3DocumentService(untrustedBucketName, trustedBucketName, failingClient,
            failingClient, "");
        assertThatThrownBy(() -> badService.getFileFromUntrustedS3("a bad file")).isInstanceOf(
            ApplicationExceptions.S3Exception.class).hasFieldOrPropertyWithValue("event", LogEvent.S3_DOWNLOAD_FAILURE);
    }

    @Test
    public void shouldThrowNotFoundExceptionWhenFileNotInTrustedBucket() {
        assertThatThrownBy(() -> service.getFileFromTrustedS3("a missing file.ext")).isInstanceOf(
            ApplicationExceptions.S3Exception.class).hasMessage(
            "File not found in S3 bucket").hasFieldOrPropertyWithValue("event", LogEvent.S3_FILE_NOT_FOUND);
    }

    @Test
    public void shouldCopyToTrustedBucket() throws IOException {

        assertThat(trustedClient.listObjectsV2(trustedBucketName).getKeyCount()).isEqualTo(0);
        DocumentCopyRequest copyRequest = new DocumentCopyRequest("someUUID.docx", "someCase", "docx", "PDF");
        S3Document document = service.copyToTrustedBucket(copyRequest);
        assertThat(trustedClient.doesObjectExist(trustedBucketName, document.getFilename())).isTrue();
    }

    @Test
    public void shouldReturnMetaDataFromTrustedBucket() throws IOException {
        DocumentCopyRequest copyRequest = new DocumentCopyRequest("someUUID.docx", "someCase", "docx", "PDF");
        service.copyToTrustedBucket(copyRequest);

        String objectKey = trustedClient.listObjectsV2(trustedBucketName).getObjectSummaries().get(0).getKey();
        S3DocumentMetaData metaData = service.getMetaDataFromTrustedS3(objectKey);
        assertThat(metaData.getOriginalFilename()).isEqualTo("sample.docx");
        assertThat(metaData.getFilename()).isEqualTo(objectKey);
        assertThat(metaData.getFileType()).isEqualTo("docx");
    }

    @Test
    public void shouldSetMetaDataWhenCopyToTrustedBucket() throws IOException {

        DocumentCopyRequest copyRequest = new DocumentCopyRequest("someUUID.docx", "someCase", "docx", "PDF");
        S3Document document = service.copyToTrustedBucket(copyRequest);

        ObjectMetadata metadata = trustedClient.getObjectMetadata(trustedBucketName, document.getFilename());
        assertThat(metadata.getContentType()).isEqualTo("application/docx");
        assertThat(metadata.getUserMetaDataOf("filename")).startsWith("someCase");
        assertThat(metadata.getUserMetaDataOf("originalName")).isEqualTo("sample.docx");
    }

    @Test
    public void shouldUploadToTrustedBucket() throws IOException, URISyntaxException {

        assertThat(trustedClient.listObjectsV2(trustedBucketName).getKeyCount()).isEqualTo(0);
        UploadDocument uploadRequest = new UploadDocument("someUUID.docx", getPDFDocument(), "someCase", "sample.docx");
        S3Document document = service.uploadFile(uploadRequest);
        assertThat(trustedClient.doesObjectExist(trustedBucketName, document.getFilename())).isTrue();
    }

    public void uploadUntrustedFiles() throws URISyntaxException, IOException {
        ObjectMetadata metaData = new ObjectMetadata();
        metaData.setContentType("application/docx");
        metaData.addUserMetadata("originalName", "sample.docx");
        metaData.addUserMetadata("filename", "someUUID.docx");
        untrustedClient.putObject(
            new PutObjectRequest(untrustedBucketName, "someUUID.docx", new ByteArrayInputStream(getDocumentByteArray()),
                metaData));
    }

    private byte[] getPDFDocument() throws URISyntaxException, IOException {
        return Files.readAllBytes(
            Paths.get(this.getClass().getClassLoader().getResource("testdata/sample.pdf").toURI()));
    }

    private byte[] getDocumentByteArray() throws URISyntaxException, IOException {
        return Files.readAllBytes(
            Paths.get(this.getClass().getClassLoader().getResource("testdata/sample.docx").toURI()));
    }

    private void clearS3Buckets() {
        trustedClient.listObjectsV2(trustedBucketName).getObjectSummaries().forEach(
            s -> trustedClient.deleteObject(trustedBucketName, s.getKey()));

        untrustedClient.listObjectsV2(trustedBucketName).getObjectSummaries().forEach(
            s -> trustedClient.deleteObject(trustedBucketName, s.getKey()));
    }
}
