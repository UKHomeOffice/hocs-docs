package uk.gov.digital.ho.hocs.document;


import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.util.IOUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.digital.ho.hocs.document.dto.Document;

import java.io.FileNotFoundException;
import java.io.IOException;

@Service
public class S3FileUtils {


    private String s3BucketName;
    private AmazonS3 s3Client;

    public S3FileUtils(@Value("${docs.untrustedS3bucket}") String s3BucketName, AmazonS3 s3Client) {

        this.s3BucketName = s3BucketName;
        this.s3Client = s3Client;
    }

    public Document getFileFromS3(String key) throws IOException {
        try {
            S3Object s3File = s3Client.getObject(new GetObjectRequest(s3BucketName, key));

            String originalName = s3File.getObjectMetadata().getUserMetaDataOf("originalName");

            return new Document(originalName,
                    IOUtils.toByteArray(s3File.getObjectContent()),
                    s3File.getObjectMetadata().getContentMD5());
        } catch (AmazonS3Exception ex) {
            if (ex.getStatusCode() == 404) {
                    throw new FileNotFoundException("File not found in S3 bucket");
            }
            else {
                throw new IOException("Error retrieving document from S3");
            }
        }
    }
}
