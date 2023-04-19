package uk.gov.digital.ho.hocs.document.aws;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.*;
import com.amazonaws.util.IOUtils;
import com.amazonaws.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.digital.ho.hocs.document.dto.camel.DocumentCopyRequest;
import uk.gov.digital.ho.hocs.document.dto.camel.S3Document;
import uk.gov.digital.ho.hocs.document.dto.camel.UploadDocument;
import uk.gov.digital.ho.hocs.document.exception.ApplicationExceptions;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

import static net.logstash.logback.argument.StructuredArguments.value;
import static uk.gov.digital.ho.hocs.document.application.LogEvent.*;

@Service
@Slf4j
public class S3DocumentService {

    private String trustedS3BucketName;

    private AmazonS3 trustedS3Client;

    private String untrustedS3BucketName;

    private AmazonS3 untrustedS3Client;

    private static final String CONVERTED_DOCUMENT_EXTENSION = "pdf";

    private static final String ORIGINAL_NAME = "originalName";

    private static final String FILENAME = "filename";

    private String trustedBucketKMSkeyId;

    public S3DocumentService(@Value("${docs.untrustedS3bucket}") String untrustedS3BucketName,
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

    public S3Document getFileFromUntrustedS3(String key) throws IOException {
        return getFileFromS3Bucket(key, untrustedS3Client, untrustedS3BucketName);
    }

    public S3Document getFileFromTrustedS3(String key) throws IOException {
        return getFileFromS3Bucket(key, trustedS3Client, trustedS3BucketName);
    }

    public S3Document copyToTrustedBucket(DocumentCopyRequest copyRequest) throws IOException {
        String destinationKey = String.format("%s/%s.%s", copyRequest.getExternalReferenceUUID(),
            UUID.randomUUID().toString(), copyRequest.getFileType());
        log.info(String.format("Copying %s from untrusted %s to %s trusted bucket %s", copyRequest.getFileLink(),
            untrustedS3BucketName, destinationKey, trustedS3BucketName), value(EVENT, S3_TRUSTED_COPY_REQUEST));

        S3Document copyDocument = getFileFromUntrustedS3(copyRequest.getFileLink());

        ObjectMetadata metaData = new ObjectMetadata();
        metaData.setContentType(copyDocument.getMimeType());
        metaData.addUserMetadata("externalReferenceUUID", copyRequest.getExternalReferenceUUID());
        metaData.addUserMetadata(FILENAME, destinationKey);
        metaData.addUserMetadata(ORIGINAL_NAME, copyDocument.getOriginalFilename());
        metaData.setContentLength(copyDocument.getData().length);

        PutObjectRequest uploadRequest = new PutObjectRequest(trustedS3BucketName, destinationKey,
            new ByteArrayInputStream(copyDocument.getData()), metaData);

        if (StringUtils.hasValue(trustedBucketKMSkeyId)) {
            uploadRequest = uploadRequest.withSSEAwsKeyManagementParams(
                new SSEAwsKeyManagementParams(trustedBucketKMSkeyId));
        }

        try {
            trustedS3Client.putObject(uploadRequest);
        } catch (AmazonServiceException e) {
            log.error("Unable to copy file {} to S3 bucket {}", destinationKey, trustedS3BucketName, value(EVENT, S3_TRUSTED_COPY_FAILURE));
            throw new ApplicationExceptions.S3Exception(
                String.format("Unable to upload file %s to S3 bucket %s", destinationKey, trustedS3BucketName),
                S3_TRUSTED_COPY_FAILURE, e);
        }

        return new S3Document(destinationKey, copyDocument.getOriginalFilename(), null, copyDocument.getFileType(),
            copyDocument.getMimeType());
    }

    public S3Document uploadFile(UploadDocument document) {

        ObjectMetadata metaData = new ObjectMetadata();
        metaData.setContentType("application/pdf");
        String destinationKey = String.format("%s/%s.%s", document.getExternalReferenceUUID(),
            UUID.randomUUID().toString(), CONVERTED_DOCUMENT_EXTENSION);
        metaData.addUserMetadata("externalReferenceUUID", document.getExternalReferenceUUID());
        metaData.addUserMetadata(ORIGINAL_NAME, getPDFFilename(document.getOriginalFileName()));

        PutObjectRequest uploadRequest = new PutObjectRequest(trustedS3BucketName, destinationKey,
            new ByteArrayInputStream(document.getData()), metaData);
        if (StringUtils.hasValue(trustedBucketKMSkeyId)) {
            uploadRequest = uploadRequest.withSSEAwsKeyManagementParams(
                new SSEAwsKeyManagementParams(trustedBucketKMSkeyId));
        }

        PutObjectResult response;
        try {
            response = trustedS3Client.putObject(uploadRequest);
        } catch (AmazonServiceException e) {
            log.error("Unable to upload file {} to S3 bucket {}", destinationKey, trustedS3BucketName, value(EVENT, S3_UPLOAD_FAILURE));
            throw new ApplicationExceptions.S3Exception(
                String.format("Unable to upload file %s to bucket %s", destinationKey, trustedS3BucketName),
                S3_UPLOAD_FAILURE, e);
        }

        return new S3Document(destinationKey, document.getFilename(), null, response.getContentMd5(),
            "application/pdf");

    }

    private S3Document getFileFromS3Bucket(String key, AmazonS3 s3Client, String bucketName) throws IOException {
        try {

            S3Object s3File = s3Client.getObject(new GetObjectRequest(bucketName, key));

            String originalName = Optional.ofNullable(
                s3File.getObjectMetadata().getUserMetaDataOf(ORIGINAL_NAME)).orElse("");

            String filename = Optional.ofNullable(s3File.getObjectMetadata().getUserMetaDataOf(FILENAME)).orElse(key);

            String extension = getFileExtension(originalName);

            return new S3Document(filename, originalName, IOUtils.toByteArray(s3File.getObjectContent()), extension,
                s3File.getObjectMetadata().getContentType());

        } catch (AmazonS3Exception ex) {
            if (ex.getStatusCode() == 404) {
                log.error("File not found in S3 bucket {}", key, value(EVENT, S3_FILE_NOT_FOUND));
                throw new ApplicationExceptions.S3Exception("File not found in S3 bucket", S3_FILE_NOT_FOUND, ex);
            } else {
                log.error("Failed to get file from S3 bucket {}", key, value(EVENT, S3_DOWNLOAD_FAILURE));
                throw new ApplicationExceptions.S3Exception("Error retrieving document from S3", S3_DOWNLOAD_FAILURE,
                    ex);
            }
        }
    }

    private String getFileExtension(String fileName) {
        return fileName.substring(fileName.lastIndexOf('.') + 1);
    }

    private String getPDFFilename(String fileName) {
        return String.format("%s.%s", fileName.substring(0, fileName.lastIndexOf('.')), "pdf");
    }

}
