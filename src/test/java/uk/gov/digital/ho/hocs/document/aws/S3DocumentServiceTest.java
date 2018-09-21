package uk.gov.digital.ho.hocs.document.aws;

import com.adobe.testing.s3mock.junit4.S3MockRule;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import uk.gov.digital.ho.hocs.document.dto.camel.DocumentCopyRequest;
import uk.gov.digital.ho.hocs.document.dto.camel.S3Document;
import uk.gov.digital.ho.hocs.document.dto.camel.UploadDocument;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


public class S3DocumentServiceTest {
    private static String untrustedBucketName = "untrusted-bucked";
    private static String trustedBucketName = "trusted-bucked";

    @ClassRule
    public static final S3MockRule S3_MOCK_RULE = S3MockRule.builder().withSecureConnection(false).build();

    private AmazonS3 untrustedClient = S3_MOCK_RULE.createS3Client();
    private AmazonS3 trustedClient = S3_MOCK_RULE.createS3Client();
    private S3DocumentService service = new S3DocumentService(untrustedBucketName, trustedBucketName, trustedClient, untrustedClient, "");


    @Before
    public void setUp() throws Exception {
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
    public void shouldReturnUploadedMetaData() throws IOException, URISyntaxException {
        S3Document document = service.getFileFromUntrustedS3("someUUID.docx");
        assertThat(document.getOriginalFilename()).isEqualTo("sample.docx");
        assertThat(document.getFilename()).isEqualTo("someUUID.docx");
        assertThat(document.getFileType()).isEqualTo("docx");
    }

    @Test
    public void shouldThrowNotFoundExceptionWhenFileNotInUntrustedBucket() {
        assertThatThrownBy(() -> service.getFileFromUntrustedS3("a missing file.ext"))
                .isInstanceOf(FileNotFoundException.class)
                .hasMessage("File not found in S3 bucket");
    }

    @Test
    public void shouldThrowNotFoundExceptionWhenFileNotInTrustedBucket() {
        assertThatThrownBy(() -> service.getFileFromTrustedS3("a missing file.ext"))
                .isInstanceOf(FileNotFoundException.class)
                .hasMessage("File not found in S3 bucket");
    }

    @Test
    public void shouldCopyToTrustedBucket() throws IOException {

        assertThat(trustedClient.listObjectsV2(trustedBucketName).getKeyCount()).isEqualTo(0);
        DocumentCopyRequest copyRequest = new DocumentCopyRequest("someUUID.docx","someCase", "docx");
        S3Document document = service.copyToTrustedBucket(copyRequest);
        assertThat(trustedClient.doesObjectExist(trustedBucketName, document.getFilename())).isTrue();
    }

    @Test
    public void shouldSetMetaDataWhenCopyToTrustedBucket() throws IOException {

        DocumentCopyRequest copyRequest = new DocumentCopyRequest("someUUID.docx","someCase", "docx");
        S3Document document = service.copyToTrustedBucket(copyRequest);
        assertThat(document.getFilename()).startsWith("someCase");
        assertThat(document.getFileType()).isEqualTo("docx");
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
            untrustedClient.deleteBucket(untrustedBucketName);
        }

        if(trustedClient.doesBucketExistV2(trustedBucketName)) {
            trustedClient.deleteBucket(trustedBucketName);
        }

        untrustedClient.createBucket(untrustedBucketName);
        trustedClient.createBucket(trustedBucketName);
    }
}
