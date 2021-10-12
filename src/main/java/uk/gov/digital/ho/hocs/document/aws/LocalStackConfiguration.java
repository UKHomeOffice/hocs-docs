package uk.gov.digital.ho.hocs.document.aws;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.Protocol;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.AmazonSNSClientBuilder;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile({"local", "test"})
public class LocalStackConfiguration {

    private static final String EU_WEST_2 = "eu-west-2";

    @Value("${aws.local.host:localhost}")
    private String awsHost;

    @Bean("auditSqsClient")
    public AmazonSQS auditSqsClient() {
        return sqsClient();
    }

    @Bean
    public AmazonSQS sqsClient() {

        String host = String.format("http://%s:4576/", awsHost);

        AwsClientBuilder.EndpointConfiguration endpoint = new AwsClientBuilder.EndpointConfiguration(host, EU_WEST_2);
        return AmazonSQSClientBuilder.standard()
                .withClientConfiguration(new ClientConfiguration().withProtocol(Protocol.HTTP))
                .withCredentials(awsCredentialsProvider)
                .withEndpointConfiguration(endpoint)
                .build();
    }

    @Bean("auditSnsClient")
    public AmazonSNS auditSnsClient() {
        String host = String.format("http://%s:4575/", awsHost);
        AwsClientBuilder.EndpointConfiguration endpoint = new AwsClientBuilder.EndpointConfiguration(host, EU_WEST_2);
        return AmazonSNSClientBuilder.standard()
                .withClientConfiguration(new ClientConfiguration().withProtocol(Protocol.HTTP))
                .withCredentials(awsCredentialsProvider)
                .withEndpointConfiguration(endpoint)
                .build();
    }

    @Bean("Trusted")
    public AmazonS3 trustedS3Client() {
        return s3Client();
    }

    @Bean("UnTrusted")
    public AmazonS3 untrustedS3Client() {
        return s3Client();
    }

    public AmazonS3 s3Client() {
        String host = String.format("http://%s:4572/", awsHost);

        AwsClientBuilder.EndpointConfiguration endpoint = new AwsClientBuilder.EndpointConfiguration(host, EU_WEST_2);

        return AmazonS3ClientBuilder.standard()
                .withClientConfiguration(new ClientConfiguration().withProtocol(Protocol.HTTP))
                .withCredentials(awsCredentialsProvider)
                .withPathStyleAccessEnabled(true)
                .withEndpointConfiguration(endpoint)
                .disableChunkedEncoding()
                .build();
    }

    private final AWSCredentialsProvider awsCredentialsProvider = new AWSCredentialsProvider() {

        @Override
        public AWSCredentials getCredentials() {
            return new BasicAWSCredentials("test", "test");
        }

        @Override
        public void refresh() {
            // not needed locally
        }

    };

}