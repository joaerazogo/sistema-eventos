package co.com.ticket.sqs.sender.config;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class SQSSenderConfigTest {

    private final SQSSenderConfig config = new SQSSenderConfig();

    @Test
    void shouldCreateSqsClientWithEndpointOverride() {
        SQSSenderProperties properties = new SQSSenderProperties("us-east-1", "http://localhost:4566/000000000000/test-queue", "http://localhost:4566");

        SqsAsyncClient client = config.configSqs(properties);

        assertNotNull(client);
    }

    @Test
    void shouldCreateSqsClientWithoutEndpointOverride() {
        SQSSenderProperties properties = new SQSSenderProperties("us-east-1", "https://sqs.us-east-1.amazonaws.com/123456789/test-queue", null);

        SqsAsyncClient client = config.configSqs(properties);

        assertNotNull(client);
    }

}
