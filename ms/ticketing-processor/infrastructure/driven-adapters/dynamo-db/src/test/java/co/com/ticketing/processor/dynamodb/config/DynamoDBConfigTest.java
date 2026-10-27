package co.com.ticketing.processor.dynamodb.config;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class DynamoDBConfigTest {

    private final DynamoDBConfig config = new DynamoDBConfig();

    @Test
    void shouldCreateDynamoDbAsyncClientWithEndpoint() {
        DynamoDbAsyncClient client = config.amazonDynamoDB("http://localhost:4566", "us-east-1");
        assertNotNull(client);
        client.close();
    }

    @Test
    void shouldCreateDynamoDbAsyncClientWithoutEndpoint() {
        DynamoDbAsyncClient client = config.amazonDynamoDB("", "us-east-1");
        assertNotNull(client);
        client.close();
    }

    @Test
    void shouldCreateDynamoDbEnhancedAsyncClient() {
        DynamoDbAsyncClient client = config.amazonDynamoDB("http://localhost:4566", "us-east-1");
        DynamoDbEnhancedAsyncClient enhancedClient = config.getDynamoDbEnhancedAsyncClient(client);
        assertNotNull(enhancedClient);
        client.close();
    }
}
