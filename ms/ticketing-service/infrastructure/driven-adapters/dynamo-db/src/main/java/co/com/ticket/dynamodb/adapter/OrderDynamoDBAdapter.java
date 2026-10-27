package co.com.ticket.dynamodb.adapter;

import co.com.ticket.dynamodb.helper.TemplateAdapterOperations;
import co.com.ticket.dynamodb.order.mapper.OrderMapper;
import co.com.ticket.dynamodb.order.model.OrderEntity;
import co.com.ticket.model.order.Order;
import org.mapstruct.factory.Mappers;
import org.reactivecommons.utils.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;

@Repository
public class OrderDynamoDBAdapter extends TemplateAdapterOperations<Order, String, OrderEntity> {

    public OrderDynamoDBAdapter(DynamoDbEnhancedAsyncClient connectionFactory, ObjectMapper mapper,
                                @Value("${aws.dynamodb.orders-table-name}") String tableName) {
        super(connectionFactory, mapper, Mappers.getMapper(OrderMapper.class)::toDomain, tableName, "status-expiresAt-index");
    }

}
