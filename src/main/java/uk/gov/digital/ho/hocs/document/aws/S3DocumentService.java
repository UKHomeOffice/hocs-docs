package uk.gov.digital.ho.hocs.document.aws;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.*;
import com.amazonaws.util.IOUtils;
import com.amazonaws.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.digital.ho.hocs.document.dto.DocumentConversionRequest;
import uk.gov.digital.ho.hocs.document.exception.ApplicationExceptions;
import uk.gov.digital.ho.hocs.document.model.Document;
import uk.gov.digital.ho.hocs.document.dto.DocumentCopyRequest;
import uk.gov.digital.ho.hocs.document.model.UploadDocument;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
public class S3DocumentService {

    private String trustedS3BucketName;
    private AmazonS3 trustedS3Client;
    private String untrustedS3BucketName;
    private AmazonS3 untrustedS3Client;
    private final String CONVERTED_DOCUMENT_EXTENSION = "pdf";
    private String trustedBucketKMSkeyId;

    public S3DocumentService( @Value("${docs.untrustedS3bucket}") String untrustedS3BucketName,
                              @Value("${docs.trustedS3bucket}") String trustedS3Bucket,
                              @Qualifier("Trusted") AmazonS3 trustedS3Client,
                              @Qualifier("UnTrusted") AmazonS3 untrustedS3Client,
                              @Value("${docs.trustedS3bucketKMSKeyId}") String trustedBucketKMSkeyId) {

        this.untrustedS3BucketName = untrustedS3BucketName;
        this.trustedS3BucketName = trustedS3Bucket;
        this.untrustedS3Client = untrustedS3Client;
        this.trustedS3Client = trustedS3Client;
        this.trustedBucketKMSkeyId = trustedBucketKMSkeyId;
    }

    public Document getFileFromUntrustedS3(String key) {
        return getFileFromS3Bucket(key, untrustedS3Client, untrustedS3BucketName);
    }

    public Document getFileFromTrustedS3(String key)  {
        return getFileFromS3Bucket(key, trustedS3Client, trustedS3BucketName);
    }

    public DocumentConversionRequest copyToTrustedBucket(DocumentCopyRequest copyRequest) throws IOException {
            String destinationKey = String.format("%s/%s.%s", copyRequest.getCaseUUID(), UUID.randomUUID().toString(), copyRequest.getFileType());
            log.info("Copying {} from untrusted {} to {} trusted bucket {}", copyRequest.getFileLink(), untrustedS3BucketName, destinationKey, trustedS3BucketName);
            ObjectMetadata metaData = new ObjectMetadata();
            metaData.addUserMetadata("caseUUID", copyRequest.getCaseUUID());
            metaData.addUserMetadata("filename", destinationKey);
            metaData.addUserMetadata("originalName", copyRequest.getFileLink());

            Document copyDocument = getFileFromUntrustedS3(copyRequest.getFileLink());

            PutObjectRequest uploadRequest = new PutObjectRequest(trustedS3BucketName, destinationKey, new ByteArrayInputStream(copyDocument.getData()), metaData);

            if(StringUtils.hasValue(trustedBucketKMSkeyId)) {
                uploadRequest = uploadRequest.withSSEAwsKeyManagementParams(new SSEAwsKeyManagementParams(trustedBucketKMSkeyId));
            }
            trustedS3Client.putObject(uploadRequest);

            return new DocumentConversionRequest(destinationKey, copyRequest.getCaseUUID(), copyDocument.getFileType());
    }

    public Document uploadFile(UploadDocument document) {

        ObjectMetadata metaData = new ObjectMetadata();

        metaData.setContentType("application/pdf");
        String destinationKey = String.format("%s/%s.%s", document.getCaseUUID(), UUID.randomUUID().toString(),CONVERTED_DOCUMENT_EXTENSION);

        Map<String, String> userMetaData = new HashMap<>(1);
        userMetaData.put("caseUUID", document.getCaseUUID());
        metaData.setUserMetadata(userMetaData);

        PutObjectRequest uploadRequest = new PutObjectRequest(trustedS3BucketName, destinationKey, new ByteArrayInputStream(document.getData()), metaData);
        if(StringUtils.hasValue(trustedBucketKMSkeyId)) {
            uploadRequest = uploadRequest.withSSEAwsKeyManagementParams(new SSEAwsKeyManagementParams(trustedBucketKMSkeyId));
        }

        PutObjectResult response = trustedS3Client.putObject(uploadRequest);
        return new Document(destinationKey, document.getFilename(),null,  response.getContentMd5(), "application/pdf");

    }

    private Document getFileFromS3Bucket(String key, AmazonS3 s3Client, String bucketName) {
        try {

            S3Object s3File = s3Client.getObject(new GetObjectRequest(bucketName, key));

            String mimeType = s3File.getObjectMetadata().getContentType();
            String originalName = Optional.ofNullable(s3File.getObjectMetadata().getUserMetaDataOf("originalName"))
                    .orElse("");

            String filename = Optional.ofNullable(s3File.getObjectMetadata().getUserMetaDataOf("filename"))
                    .orElse(key);

            String extension = getFileExtension(originalName);

            return new Document(filename, originalName,
                    IOUtils.toByteArray(s3File.getObjectContent()),extension, mimeType);

        } catch (AmazonS3Exception ex) {
            if (ex.getStatusCode() == 404) {
                throw new ApplicationExceptions.EntityNotFoundException("File not found in S3 bucket");
            }
            else {
                throw new ApplicationExceptions.EntityNotFoundException("Error retrieving document from S3");
            }
        } catch (IOException e) {
            throw new ApplicationExceptions.EntityNotFoundException("Error converting document to byteArray");
        }
    }

    private String getFileExtension(String fileName) {
        return fileName.substring(fileName.lastIndexOf('.') + 1);
    }
}
