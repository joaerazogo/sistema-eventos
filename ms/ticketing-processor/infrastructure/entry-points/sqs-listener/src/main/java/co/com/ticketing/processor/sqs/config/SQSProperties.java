package co.com.ticketing.processor.sqs.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "entrypoint.sqs")
public record SQSProperties(
        String region,
        String endpoint,
        String queueUrl,
        int waitTimeSeconds,
        int maxNumberOfMessages,
        int visibilityTimeoutSeconds,
        int numberOfThreads
) {}
