package uk.gov.digital.ho.hocs.document.aws;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.Protocol;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.internal.StaticCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.S3ClientOptions;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.util.StringUtils;

@Configuration
@Profile({ "local"})
public class LocalStackConfiguration {


    @Bean
    public AmazonSQS sqsClient() {

        AwsClientBuilder.EndpointConfiguration endpoint = new AwsClientBuilder.EndpointConfiguration("http://localhost:4576/", "eu-west-2");
        return AmazonSQSClientBuilder.standard()
                .withClientConfiguration(new ClientConfiguration().withProtocol(Protocol.HTTP))
                .withCredentials(awsCredentialsProvider)
                .withEndpointConfiguration(endpoint)
                .build();
    }

    @Bean
    public AmazonS3 s3Client() {
        AwsClientBuilder.EndpointConfiguration endpoint = new AwsClientBuilder.EndpointConfiguration("http://localhost:4572/", "eu-west-2");

        return AmazonS3ClientBuilder.standard()
                .withClientConfiguration(new ClientConfiguration().withProtocol(Protocol.HTTP))
                .withCredentials(awsCredentialsProvider)
                .withPathStyleAccessEnabled(true)
                .withEndpointConfiguration(endpoint)
                .build();
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