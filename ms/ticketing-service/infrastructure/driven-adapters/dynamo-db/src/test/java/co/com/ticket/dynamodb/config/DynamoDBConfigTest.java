package co.com.ticket.dynamodb.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(MockitoExtension.class)
class DynamoDBConfigTest {

    @Mock
    private DynamoDbAsyncClient dynamoDbAsyncClient;

    private final DynamoDBConfig dynamoDBConfig = new DynamoDBConfig();

    @Test
    void testAmazonDynamoDB() {

        DynamoDbAsyncClient result = dynamoDBConfig.amazonDynamoDB(
                "http://aws.dynamo.test",
                "us-east-1");

        assertNotNull(result);
    }

    @Test
    void testAmazonDynamoDBWithoutEndpointOverride() {

        DynamoDbAsyncClient result = dynamoDBConfig.amazonDynamoDB(
                "",
                "us-east-1");

        assertNotNull(result);
    }


    @Test
    void testGetDynamoDbEnhancedAsyncClient() {
        DynamoDbEnhancedAsyncClient result = dynamoDBConfig.getDynamoDbEnhancedAsyncClient(dynamoDbAsyncClient);

        assertNotNull(result);
    }
}
