package co.com.ticketing.processor.dynamodb.adapter;

import co.com.ticketing.processor.dynamodb.order.model.OrderEntity;
import co.com.ticketing.processor.dynamodb.helper.TemplateAdapterOperations;
import co.com.ticketing.processor.dynamodb.order.mapper.OrderMapper;
import co.com.ticketing.processor.model.order.Order;
import org.mapstruct.factory.Mappers;
import org.reactivecommons.utils.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;
import software.amazon.awssdk.services.dynamodb.model.ReturnValue;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;

import java.time.Instant;
import java.util.Map;

@Repository
public class OrderDynamoDBAdapter extends TemplateAdapterOperations<Order, String, OrderEntity> {

    private final DynamoDbAsyncClient dynamoDbAsyncClient;
    private final String tableName;

    public OrderDynamoDBAdapter(DynamoDbEnhancedAsyncClient connectionFactory, ObjectMapper mapper,
                                DynamoDbAsyncClient dynamoDbAsyncClient,
                                @Value("${aws.dynamodb.orders-table-name}") String tableName) {
        super(connectionFactory, mapper, Mappers.getMapper(OrderMapper.class)::toDomain, tableName, "status-expiresAt-index");
        this.dynamoDbAsyncClient = dynamoDbAsyncClient;
        this.tableName = tableName;
    }

    public Mono<Order> saveIfReserved(Order order) {
        UpdateItemRequest request = UpdateItemRequest.builder()
                .tableName(tableName)
                .key(Map.of("orderId", AttributeValue.builder().s(order.getOrderId()).build()))
                .updateExpression("SET #s = :newStatus")
                .conditionExpression("#s = :reserved")
                .expressionAttributeNames(Map.of("#s", "status"))
                .expressionAttributeValues(Map.of(
                        ":newStatus", AttributeValue.builder().s(order.getStatus().name()).build(),
                        ":reserved", AttributeValue.builder().s("RESERVED").build()
                ))
                .returnValues(ReturnValue.ALL_NEW)
                .build();
        return Mono.fromFuture(dynamoDbAsyncClient.updateItem(request))
                .thenReturn(order)
                .onErrorResume(ConditionalCheckFailedException.class, e -> Mono.empty());
    }

    public Flux<Order> findExpiredReservations(String status, Instant expiresAt) {
        QueryEnhancedRequest request = QueryEnhancedRequest.builder()
                .queryConditional(QueryConditional.sortLessThanOrEqualTo(
                        Key.builder()
                                .partitionValue(AttributeValue.builder().s(status).build())
                                .sortValue(AttributeValue.builder().s(expiresAt.toString()).build())
                                .build()))
                .build();
        return queryByIndex(request, "status-expiresAt-index")
                .flatMapIterable(list -> list);
    }

    public Mono<Order> findById(String orderId) {
        return getById(orderId);
    }

}
