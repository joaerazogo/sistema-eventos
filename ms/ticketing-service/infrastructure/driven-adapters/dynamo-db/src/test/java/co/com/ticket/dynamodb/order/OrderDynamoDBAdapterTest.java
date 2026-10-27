package co.com.ticket.dynamodb.order;

import co.com.ticket.dynamodb.adapter.OrderDynamoDBAdapter;
import co.com.ticket.dynamodb.order.model.OrderEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.reactivecommons.utils.ObjectMapper;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncIndex;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderDynamoDBAdapterTest {

    @Mock private DynamoDbEnhancedAsyncClient connectionFactory;
    @Mock private DynamoDbAsyncTable<OrderEntity> table;
    @Mock private DynamoDbAsyncIndex<OrderEntity> index;
    @Mock private ObjectMapper mapper;

    @Test
    void shouldInstantiateAdapter() {
        when(connectionFactory.table("orders-dev", TableSchema.fromBean(OrderEntity.class))).thenReturn(table);
        when(table.index("status-expiresAt-index")).thenReturn(index);
        OrderDynamoDBAdapter adapter = new OrderDynamoDBAdapter(connectionFactory, mapper, "orders-dev");
        assertNotNull(adapter);
    }
}
