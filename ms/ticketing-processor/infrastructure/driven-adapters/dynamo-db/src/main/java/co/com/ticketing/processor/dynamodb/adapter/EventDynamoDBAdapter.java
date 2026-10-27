package co.com.ticketing.processor.dynamodb.adapter;

import co.com.ticketing.processor.dynamodb.event.model.EventEntity;
import co.com.ticketing.processor.dynamodb.event.mapper.EventMapper;
import co.com.ticketing.processor.dynamodb.helper.TemplateAdapterOperations;
import co.com.ticketing.processor.model.event.Event;
import org.mapstruct.factory.Mappers;
import org.reactivecommons.utils.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;
import software.amazon.awssdk.services.dynamodb.model.ReturnValue;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;

import java.util.Map;

@Repository
public class EventDynamoDBAdapter extends TemplateAdapterOperations<Event, String, EventEntity> {

    private final DynamoDbAsyncClient dynamoDbAsyncClient;
    private final String tableName;

    public EventDynamoDBAdapter(DynamoDbEnhancedAsyncClient connectionFactory,
                                DynamoDbAsyncClient dynamoDbAsyncClient,
                                ObjectMapper mapper,
                                @Value("${aws.dynamodb.table-name}") String tableName) {
        super(connectionFactory, mapper, Mappers.getMapper(EventMapper.class)::toDomain, tableName);
        this.dynamoDbAsyncClient = dynamoDbAsyncClient;
        this.tableName = tableName;
    }

    public Mono<Event> findById(String eventId) {
        return getById(eventId);
    }

    public Mono<Event> updateAvailableTickets(String eventId, int delta) {
        UpdateItemRequest request = UpdateItemRequest.builder()
                .tableName(tableName)
                .key(Map.of("eventId", AttributeValue.builder().s(eventId).build()))
                .updateExpression("SET availableTickets = availableTickets + :delta")
                .conditionExpression("availableTickets >= :minRequired")
                .expressionAttributeValues(Map.of(
                        ":delta", AttributeValue.builder().n(String.valueOf(delta)).build(),
                        ":minRequired", AttributeValue.builder().n(String.valueOf(Math.abs(delta))).build()
                ))
                .returnValues(ReturnValue.ALL_NEW)
                .build();

        return Mono.fromFuture(dynamoDbAsyncClient.updateItem(request))
                .flatMap(response -> findById(eventId))
                .onErrorMap(ConditionalCheckFailedException.class,
                        e -> new IllegalStateException("No available tickets for event " + eventId));
    }

}
