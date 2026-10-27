package co.com.ticket.dynamodb.adapter;

import co.com.ticket.dynamodb.helper.TemplateAdapterOperations;
import co.com.ticket.dynamodb.event.mapper.EventMapper;
import co.com.ticket.dynamodb.event.model.EventEntity;
import co.com.ticket.model.event.Event;
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

import java.util.Map;

@Repository
public class DynamoDBTemplateAdapter extends TemplateAdapterOperations<Event, String, EventEntity> {

    private final DynamoDbAsyncClient dynamoDbAsyncClient;
    private final String tableName;

    public DynamoDBTemplateAdapter(DynamoDbEnhancedAsyncClient connectionFactory,
                                   DynamoDbAsyncClient dynamoDbAsyncClient,
                                   ObjectMapper mapper,
                                   @Value("${aws.dynamodb.table-name}") String tableName) {
        super(connectionFactory, mapper, Mappers.getMapper(EventMapper.class)::toDomain, tableName, "venue-date-index");
        this.dynamoDbAsyncClient = dynamoDbAsyncClient;
        this.tableName = tableName;
    }

    public Mono<Boolean> existsByVenueAndDate(String venue, String date) {
        QueryEnhancedRequest request = QueryEnhancedRequest.builder()
                .queryConditional(QueryConditional.keyEqualTo(
                        Key.builder()
                                .partitionValue(venue)
                                .sortValue(date)
                                .build()))
                .limit(1)
                .build();
        return queryByIndex(request, "venue-date-index")
                .map(list -> !list.isEmpty());
    }

    public Flux<Event> findAll() {
        return scan().flatMapIterable(list -> list);
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
                .flatMap(response -> getById(eventId))
                .onErrorMap(ConditionalCheckFailedException.class,
                        e -> new IllegalStateException("No available tickets for event " + eventId));
    }

}
