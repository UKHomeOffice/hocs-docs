package uk.gov.digital.ho.hocs.document.aws;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.*;
import com.amazonaws.util.IOUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.digital.ho.hocs.document.dto.Document;
import uk.gov.digital.ho.hocs.document.dto.UploadDocument;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class S3DocumentService {

    private String untrustedS3BucketName;
    private String trustedS3BucketName;
    private AmazonS3 s3Client;
    private final String CONVERTED_DOCUMENT_EXTENSION = "pdf";

    public S3DocumentService(@Value("${docs.untrustedS3bucket}") String untrustedS3BucketName, @Value("${docs.trustedS3bucket}") String trustedS3Bucket, AmazonS3 s3Client) {

        this.untrustedS3BucketName = untrustedS3BucketName;
        this.trustedS3BucketName = trustedS3Bucket;
        this.s3Client = s3Client;
    }

    public Document getFileFromS3(String key) throws IOException {
        return getFileFromS3Bucket(key, untrustedS3BucketName);
    }

    public Document copyToTrustedBucket(String sourceKey, String caseUUID, String fileType) throws IOException {

            ObjectMetadata metaData = new ObjectMetadata();

            String destinationKey = String.format("%s/%s.%s", caseUUID, UUID.randomUUID().toString(), fileType);

            Map<String, String> userMetaData = new HashMap<>(1);
            userMetaData.put("caseUUID", caseUUID);
            userMetaData.put("filename", destinationKey);
            metaData.setUserMetadata(userMetaData);

            CopyObjectRequest request = new CopyObjectRequest(untrustedS3BucketName,
                    sourceKey,
                    trustedS3BucketName,
                    destinationKey)
                    .withNewObjectMetadata(metaData)
                    .withMetadataDirective(MetadataDirective.COPY);
        try {
            s3Client.copyObject(request);

        } catch (AmazonS3Exception ex) {
            throw new IOException("Error copying document from to trusted S3 bucket");
        }

            return getFileFromS3Bucket(destinationKey, trustedS3BucketName);
    }

    public Document uploadFile(UploadDocument document) {

        ObjectMetadata metaData = new ObjectMetadata();

        String destinationKey = String.format("%s/%s.%s", document.getCaseUUID(), UUID.randomUUID().toString(),CONVERTED_DOCUMENT_EXTENSION);

        Map<String, String> userMetaData = new HashMap<>(1);
        userMetaData.put("caseUUID", document.getCaseUUID());
        metaData.setUserMetadata(userMetaData);

        PutObjectResult response = s3Client.putObject(trustedS3BucketName, destinationKey, new ByteArrayInputStream(document.getData()),metaData);
        return new Document(destinationKey, document.getFilename(),null,  response.getContentMd5());

    }

    private Document getFileFromS3Bucket(String key, String bucketName) throws IOException {
        try {
            S3Object s3File = s3Client.getObject(new GetObjectRequest(bucketName, key));

            String originalName = Optional.ofNullable(s3File.getObjectMetadata().getUserMetaDataOf("originalName"))
                    .orElse("");

            String filename = Optional.ofNullable(s3File.getObjectMetadata().getUserMetaDataOf("filename"))
                    .orElse(key);

            String extension = getFileExtension(originalName);

            return new Document(filename, originalName,
                    IOUtils.toByteArray(s3File.getObjectContent()),extension);

        } catch (AmazonS3Exception ex) {
            if (ex.getStatusCode() == 404) {
                throw new FileNotFoundException("File not found in S3 bucket");
            }
            else {
                throw new IOException("Error retrieving document from S3");
            }
        }
    }

    private String getFileExtension(String fileName) {
        return fileName.substring(fileName.lastIndexOf('.') + 1);
    }
}
