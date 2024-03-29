package uk.gov.digital.ho.hocs.document.aws;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.internal.StaticCredentialsProvider;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.util.StringUtils;

@Configuration
@Profile({ "sqs" })
public class SqsConfiguration {

    @Bean
    public AmazonSQS sqsClient(@Value("${docs.aws.sqs.access.key}") String accessKey,
                               @Value("${docs.aws.sqs.secret.key}") String secretKey,
                               @Value("${aws.sqs.region}") String region) {

        if (StringUtils.isEmpty(accessKey)) {
            throw new BeanCreationException("Failed to create SQS client bean. Need non-blank value for access key");
        }

        if (StringUtils.isEmpty(secretKey)) {
            throw new BeanCreationException("Failed to create SQS client bean. Need non-blank values for secret key");
        }

        if (StringUtils.isEmpty(region)) {
            throw new BeanCreationException("Failed to create SQS client bean. Need non-blank values for region");
        }

        return AmazonSQSClientBuilder.standard().withRegion(region).withCredentials(
            new StaticCredentialsProvider(new BasicAWSCredentials(accessKey, secretKey))).withClientConfiguration(
            new ClientConfiguration()).build();
    }

}