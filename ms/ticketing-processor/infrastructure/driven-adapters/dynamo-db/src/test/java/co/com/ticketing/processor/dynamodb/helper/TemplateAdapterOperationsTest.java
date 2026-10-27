package co.com.ticketing.processor.dynamodb.helper;

import co.com.ticketing.processor.dynamodb.adapter.EventDynamoDBAdapter;
import co.com.ticketing.processor.dynamodb.event.model.EventEntity;
import co.com.ticketing.processor.model.event.Event;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.reactivecommons.utils.ObjectMapper;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import reactor.test.StepVerifier;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncIndex;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.enhanced.dynamodb.model.PagePublisher;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemResponse;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

class TemplateAdapterOperationsTest {

    @Mock private DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient;
    @Mock private DynamoDbAsyncClient dynamoDbAsyncClient;
    @Mock private ObjectMapper mapper;
    @Mock private DynamoDbAsyncTable<EventEntity> customerTable;
    @Mock private DynamoDbAsyncIndex<EventEntity> customerIndex;
    @Mock private PagePublisher<EventEntity> pagePublisher;

    private EventEntity modelEntity;
    private Event eventModel;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(dynamoDbEnhancedAsyncClient.table("events-dev", TableSchema.fromBean(EventEntity.class)))
                .thenReturn(customerTable);
        modelEntity = new EventEntity();
        modelEntity.setEventId("id");
        modelEntity.setName("Concert");
        eventModel = Event.builder().eventId("id").name("Concert").build();
    }

    @Test
    void modelEntityPropertiesMustNotBeNull() {
        assertNotNull(modelEntity.getEventId());
        assertNotNull(modelEntity.getName());
    }

    @Test
    void testSave() {
        when(mapper.map(eventModel, EventEntity.class)).thenReturn(modelEntity);
        when(customerTable.putItem(modelEntity)).thenReturn(CompletableFuture.runAsync(() -> {}));
        EventDynamoDBAdapter adapter = new EventDynamoDBAdapter(dynamoDbEnhancedAsyncClient, dynamoDbAsyncClient, mapper, "events-dev");
        StepVerifier.create(adapter.save(eventModel)).expectNext(eventModel).verifyComplete();
    }

    @Test
    void testGetById() {
        when(customerTable.getItem(Key.builder().partitionValue(AttributeValue.builder().s("id").build()).build()))
                .thenReturn(CompletableFuture.completedFuture(modelEntity));
        when(mapper.map(modelEntity, Event.class)).thenReturn(eventModel);
        EventDynamoDBAdapter adapter = new EventDynamoDBAdapter(dynamoDbEnhancedAsyncClient, dynamoDbAsyncClient, mapper, "events-dev");
        StepVerifier.create(adapter.getById("id")).expectNext(eventModel).verifyComplete();
    }

    @Test
    void testGetByIdReturnsEmptyWhenNotFound() {
        when(customerTable.getItem(any(Key.class))).thenReturn(CompletableFuture.completedFuture(null));
        EventDynamoDBAdapter adapter = new EventDynamoDBAdapter(dynamoDbEnhancedAsyncClient, dynamoDbAsyncClient, mapper, "events-dev");
        StepVerifier.create(adapter.getById("nonexistent")).verifyComplete();
    }

    @Test
    void testDelete() {
        when(mapper.map(eventModel, EventEntity.class)).thenReturn(modelEntity);
        when(mapper.map(modelEntity, Event.class)).thenReturn(eventModel);
        when(customerTable.deleteItem(modelEntity)).thenReturn(CompletableFuture.completedFuture(modelEntity));
        EventDynamoDBAdapter adapter = new EventDynamoDBAdapter(dynamoDbEnhancedAsyncClient, dynamoDbAsyncClient, mapper, "events-dev");
        StepVerifier.create(adapter.delete(eventModel)).expectNext(eventModel).verifyComplete();
    }

    @Test
    @SuppressWarnings("unchecked")
    void testQuery() {
        Page<EventEntity> page = Page.create(List.of(modelEntity));
        QueryEnhancedRequest queryRequest = QueryEnhancedRequest.builder().build();
        when(customerTable.query(queryRequest)).thenReturn(pagePublisher);
        doAnswer(inv -> {
            Subscriber<Page<EventEntity>> sub = inv.getArgument(0);
            sub.onSubscribe(new Subscription() {
                public void request(long n) { sub.onNext(page); sub.onComplete(); }
                public void cancel() {}
            });
            return null;
        }).when(pagePublisher).subscribe(any(Subscriber.class));
        when(mapper.map(modelEntity, Event.class)).thenReturn(eventModel);
        EventDynamoDBAdapter adapter = new EventDynamoDBAdapter(dynamoDbEnhancedAsyncClient, dynamoDbAsyncClient, mapper, "events-dev");
        StepVerifier.create(adapter.query(queryRequest))
                .expectNextMatches(list -> list.size() == 1 && list.get(0).equals(eventModel))
                .verifyComplete();
    }

    @Test
    @SuppressWarnings("unchecked")
    void testQueryByIndexExplicit() {
        Page<EventEntity> page = Page.create(List.of(modelEntity));
        QueryEnhancedRequest queryRequest = QueryEnhancedRequest.builder().build();
        when(customerTable.index("my-index")).thenReturn(customerIndex);
        SdkPublisher<Page<EventEntity>> sdkPublisher = subscriber -> subscriber.onSubscribe(new Subscription() {
            public void request(long n) { subscriber.onNext(page); subscriber.onComplete(); }
            public void cancel() {}
        });
        when(customerIndex.query(queryRequest)).thenReturn(sdkPublisher);
        when(mapper.map(modelEntity, Event.class)).thenReturn(eventModel);
        EventDynamoDBAdapter adapter = new EventDynamoDBAdapter(dynamoDbEnhancedAsyncClient, dynamoDbAsyncClient, mapper, "events-dev");
        StepVerifier.create(adapter.queryByIndex(queryRequest, "my-index"))
                .expectNextMatches(list -> list.size() == 1)
                .verifyComplete();
    }

    @Test
    void testUpdateAvailableTicketsSuccess() {
        when(dynamoDbAsyncClient.updateItem(any(UpdateItemRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(UpdateItemResponse.builder().build()));
        when(customerTable.getItem(any(Key.class))).thenReturn(CompletableFuture.completedFuture(modelEntity));
        when(mapper.map(modelEntity, Event.class)).thenReturn(eventModel);
        EventDynamoDBAdapter adapter = new EventDynamoDBAdapter(dynamoDbEnhancedAsyncClient, dynamoDbAsyncClient, mapper, "events-dev");
        StepVerifier.create(adapter.updateAvailableTickets("id", -2)).expectNext(eventModel).verifyComplete();
    }

    @Test
    void testUpdateAvailableTicketsFailsWithConditionalCheck() {
        when(dynamoDbAsyncClient.updateItem(any(UpdateItemRequest.class)))
                .thenReturn(CompletableFuture.failedFuture(
                        ConditionalCheckFailedException.builder().message("check failed").build()));
        EventDynamoDBAdapter adapter = new EventDynamoDBAdapter(dynamoDbEnhancedAsyncClient, dynamoDbAsyncClient, mapper, "events-dev");
        StepVerifier.create(adapter.updateAvailableTickets("id", -5))
                .expectErrorMatches(e -> e instanceof IllegalStateException && e.getMessage().contains("No available tickets"))
                .verify();
    }
}
