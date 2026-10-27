package co.com.ticketing.processor.sqs.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProviderChain;
import software.amazon.awssdk.auth.credentials.ContainerCredentialsProvider;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.auth.credentials.InstanceProfileCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.auth.credentials.WebIdentityTokenFileCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;

import java.net.URI;

@Configuration
@EnableConfigurationProperties(SQSProperties.class)
public class SQSConfig {

    @Bean
    public SqsAsyncClient sqsAsyncClient(SQSProperties properties) {
        var builder = SqsAsyncClient.builder()
                .region(Region.of(properties.region()))
                .credentialsProvider(credentialsProviderChain());

        if (properties.endpoint() != null && !properties.endpoint().isBlank()) {
            builder.endpointOverride(URI.create(properties.endpoint()));
        }

        return builder.build();
    }

    private AwsCredentialsProviderChain credentialsProviderChain() {
        return AwsCredentialsProviderChain.builder()
                .addCredentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("test", "test")))
                .addCredentialsProvider(EnvironmentVariableCredentialsProvider.create())
                .addCredentialsProvider(WebIdentityTokenFileCredentialsProvider.create())
                .addCredentialsProvider(ContainerCredentialsProvider.builder().build())
                .addCredentialsProvider(InstanceProfileCredentialsProvider.create())
                .build();
    }
}
