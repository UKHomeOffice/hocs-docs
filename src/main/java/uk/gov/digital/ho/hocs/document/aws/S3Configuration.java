package uk.gov.digital.ho.hocs.document.aws;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.util.StringUtils;

@Configuration
@Profile({ "s3"})
public class S3Configuration {

    @Bean("Trusted")
    public AmazonS3 trustedS3Client(@Value("${trusted.aws.s3.access.key}") String accessKey,
                             @Value("${trusted.aws.s3.secret.key}") String secretKey,
                             @Value("${aws.sqs.region}") String region) {
        //return s3Client(accessKey, secretKey, region);
        AWSCredentials credentials = new BasicAWSCredentials(accessKey, secretKey);
        AmazonS3 s3client = new AmazonS3Client(credentials);
        s3client.setRegion(Region.getRegion(Regions.valueOf(region)));
        return s3client;
    }

    @Bean("UnTrusted")
    public AmazonS3 untrustedS3Client(@Value("${untrusted.aws.s3.access.key}") String accessKey,
                             @Value("${untrusted.aws.s3.secret.key}") String secretKey,
                             @Value("${aws.sqs.region}") String region) {
        //return s3Client(accessKey, secretKey, region);
        AWSCredentials credentials = new BasicAWSCredentials(accessKey, secretKey);
        AmazonS3 s3client = new AmazonS3Client(credentials);
        s3client.setRegion(Region.getRegion(Regions.valueOf(region)));
        return s3client;
    }

    private static AmazonS3 s3Client(String accessKey, String secretKey, String region) {
        if (StringUtils.isEmpty(accessKey)) {
            throw new BeanCreationException("Failed to create S3 client bean. Need non-blank value for access key");
        }

        if (StringUtils.isEmpty(secretKey)) {
            throw new BeanCreationException("Failed to create S3 client bean. Need non-blank values for secret key");
        }

        if (StringUtils.isEmpty(region)) {
            throw new BeanCreationException("Failed to create S3 bean. Need non-blank values for region");
        }

        return AmazonS3ClientBuilder.standard()
                .withRegion(region)
                .withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials(accessKey, secretKey)))
                .withClientConfiguration(new ClientConfiguration())
                .build();
    }
}