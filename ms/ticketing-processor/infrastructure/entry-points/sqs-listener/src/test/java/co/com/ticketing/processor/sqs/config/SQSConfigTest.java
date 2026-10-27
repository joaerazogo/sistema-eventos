package co.com.ticketing.processor.sqs.config;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;

import static org.junit.jupiter.api.Assertions.*;

class SQSConfigTest {

    private final SQSConfig config = new SQSConfig();

    @Test
    void shouldCreateSqsAsyncClientWithEndpoint() {
        SQSProperties properties = new SQSProperties("us-east-1", "http://localhost:4566",
                "http://localhost:4566/000000000000/ticketing-queue-dev", 20, 10, 30, 1);
        SqsAsyncClient client = config.sqsAsyncClient(properties);
        assertNotNull(client);
        client.close();
    }

    @Test
    void shouldCreateSqsAsyncClientWithoutEndpoint() {
        SQSProperties properties = new SQSProperties("us-east-1", "",
                "http://localhost:4566/000000000000/ticketing-queue-dev", 20, 10, 30, 1);
        SqsAsyncClient client = config.sqsAsyncClient(properties);
        assertNotNull(client);
        client.close();
    }

    @Test
    void shouldCreateSqsAsyncClientWithNullEndpoint() {
        SQSProperties properties = new SQSProperties("us-east-1", null,
                "http://localhost:4566/000000000000/ticketing-queue-dev", 20, 10, 30, 1);
        SqsAsyncClient client = config.sqsAsyncClient(properties);
        assertNotNull(client);
        client.close();
    }

    @Test
    void shouldExposeAllSQSPropertiesFields() {
        SQSProperties props = new SQSProperties("us-east-1", "http://localhost:4566", "http://queue-url", 20, 10, 30, 2);
        assertEquals("us-east-1", props.region());
        assertEquals("http://localhost:4566", props.endpoint());
        assertEquals("http://queue-url", props.queueUrl());
        assertEquals(20, props.waitTimeSeconds());
        assertEquals(10, props.maxNumberOfMessages());
        assertEquals(30, props.visibilityTimeoutSeconds());
        assertEquals(2, props.numberOfThreads());
    }
}
