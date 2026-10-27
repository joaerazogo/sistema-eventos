package co.com.ticketing.processor.dynamodb.adapter;

import co.com.ticketing.processor.dynamodb.event.model.EventEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.reactivecommons.utils.ObjectMapper;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventDynamoDBAdapterTest {

    @Mock private DynamoDbEnhancedAsyncClient connectionFactory;
    @Mock private DynamoDbAsyncClient dynamoDbAsyncClient;
    @Mock private DynamoDbAsyncTable<EventEntity> table;
    @Mock private ObjectMapper mapper;

    @Test
    void shouldInstantiateAdapter() {
        when(connectionFactory.table("events-dev", TableSchema.fromBean(EventEntity.class))).thenReturn(table);
        assertNotNull(new EventDynamoDBAdapter(connectionFactory, dynamoDbAsyncClient, mapper, "events-dev"));
    }
}
