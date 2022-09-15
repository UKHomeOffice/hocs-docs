package uk.gov.digital.ho.hocs.document.aws;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.Protocol;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("test")
public class TestConfiguration {

    @Bean("Trusted")
    public AmazonS3 trustedS3Client() {
        return s3Client();
    }

    @Bean("UnTrusted")
    public AmazonS3 untrustedS3Client() {
        return s3Client();
    }

    public AmazonS3 s3Client() {
        AwsClientBuilder.EndpointConfiguration endpoint = new AwsClientBuilder.EndpointConfiguration(
            "http://localhost:9003/", "eu-west-2");

        return AmazonS3ClientBuilder.standard().withClientConfiguration(
            new ClientConfiguration().withProtocol(Protocol.HTTP)).withCredentials(
            awsCredentialsProvider).withPathStyleAccessEnabled(true).withEndpointConfiguration(endpoint).build();
    }

    private final AWSCredentialsProvider awsCredentialsProvider = new AWSCredentialsProvider() {

        @Override
        public AWSCredentials getCredentials() {
            return new BasicAWSCredentials("test", "test");
        }

        @Override
        public void refresh() {

        }

    };

}
