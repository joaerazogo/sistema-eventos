package co.com.ticketing.processor.dynamodb.adapter;

import co.com.ticketing.processor.dynamodb.order.model.OrderEntity;
import co.com.ticketing.processor.model.enums.TicketStatus;
import co.com.ticketing.processor.model.order.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.reactivecommons.utils.ObjectMapper;
import org.reactivestreams.Subscription;
import reactor.test.StepVerifier;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncIndex;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemResponse;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderDynamoDBAdapterTest {

    @Mock private DynamoDbEnhancedAsyncClient connectionFactory;
    @Mock private DynamoDbAsyncTable<OrderEntity> table;
    @Mock private DynamoDbAsyncIndex<OrderEntity> index;
    @Mock private ObjectMapper mapper;
    @Mock private DynamoDbAsyncClient dynamoDbAsyncClient;

    private OrderDynamoDBAdapter buildAdapter() {
        when(connectionFactory.table("orders-dev", TableSchema.fromBean(OrderEntity.class))).thenReturn(table);
        when(table.index("status-expiresAt-index")).thenReturn(index);
        return new OrderDynamoDBAdapter(connectionFactory, mapper, dynamoDbAsyncClient, "orders-dev");
    }

    @Test
    void shouldInstantiateAdapter() {
        assertNotNull(buildAdapter());
    }

    @Test
    void shouldFindExpiredReservations() {
        Instant reservedAt = Instant.parse("2026-12-01T20:00:00Z");
        Instant expiresAt = Instant.parse("2026-12-01T20:10:00Z");
        Order order = Order.builder().orderId("order-1").eventId("event-1").userId("user-1")
                .quantity(2).status(TicketStatus.RESERVED)
                .reservedAt(reservedAt).expiresAt(expiresAt).build();
        OrderEntity entity = new OrderEntity();
        entity.setOrderId("order-1");
        entity.setEventId("event-1");
        entity.setUserId("user-1");
        entity.setQuantity(2);
        entity.setStatus("RESERVED");
        entity.setReservedAt(reservedAt.toString());
        entity.setExpiresAt(expiresAt.toString());
        Page<OrderEntity> page = Page.create(List.of(entity));
        SdkPublisher<Page<OrderEntity>> sdkPublisher = subscriber -> subscriber.onSubscribe(new Subscription() {
            public void request(long n) { subscriber.onNext(page); subscriber.onComplete(); }
            public void cancel() {}
        });
        when(index.query(any(QueryEnhancedRequest.class))).thenReturn(sdkPublisher);
        StepVerifier.create(buildAdapter().findExpiredReservations("RESERVED", Instant.now()))
                .expectNext(order).verifyComplete();
    }

    @Test
    void shouldSaveIfReservedWhenConditionPasses() {
        Order order = Order.builder().orderId("order-1").eventId("event-1").userId("user-1")
                .quantity(2).status(TicketStatus.AVAILABLE)
                .reservedAt(Instant.parse("2026-12-01T20:00:00Z"))
                .expiresAt(Instant.parse("2026-12-01T20:10:00Z")).build();
        when(dynamoDbAsyncClient.updateItem(any(software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(UpdateItemResponse.builder().build()));
        StepVerifier.create(buildAdapter().saveIfReserved(order))
                .expectNext(order).verifyComplete();
    }

    @Test
    void shouldReturnEmptyWhenSaveIfReservedConditionFails() {
        Order order = Order.builder().orderId("order-1").eventId("event-1").userId("user-1")
                .quantity(2).status(TicketStatus.AVAILABLE)
                .reservedAt(Instant.parse("2026-12-01T20:00:00Z"))
                .expiresAt(Instant.parse("2026-12-01T20:10:00Z")).build();
        CompletableFuture<UpdateItemResponse> failed = new CompletableFuture<>();
        failed.completeExceptionally(ConditionalCheckFailedException.builder().message("condition failed").build());
        when(dynamoDbAsyncClient.updateItem(any(software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest.class)))
                .thenReturn(failed);
        StepVerifier.create(buildAdapter().saveIfReserved(order))
                .verifyComplete();
    }
}
