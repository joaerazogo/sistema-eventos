package co.com.ticketing.processor.dynamodb;

import co.com.ticketing.processor.dynamodb.adapter.EventDynamoDBAdapter;
import co.com.ticketing.processor.dynamodb.adapter.OrderDynamoDBAdapter;

import co.com.ticketing.processor.dynamodb.event.gateway.EventRepositoryImpl;
import co.com.ticketing.processor.dynamodb.event.model.EventEntity;
import co.com.ticketing.processor.dynamodb.order.gateway.OrderRepositoryImpl;
import co.com.ticketing.processor.dynamodb.order.model.OrderEntity;
import co.com.ticketing.processor.model.enums.ErrorMessage;
import co.com.ticketing.processor.model.enums.TicketStatus;
import co.com.ticketing.processor.model.event.Event;
import co.com.ticketing.processor.model.exception.TechnicalException;
import co.com.ticketing.processor.model.order.Order;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.reactivecommons.utils.ObjectMapper;
import org.reactivestreams.Subscription;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncIndex;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemResponse;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DynamoDBOrderTest {

    @Mock private DynamoDbEnhancedAsyncClient connectionFactory;
    @Mock private DynamoDbAsyncClient dynamoDbAsyncClient;
    @Mock private ObjectMapper mapper;
    @Mock private DynamoDbAsyncTable<EventEntity> eventTable;
    @Mock private DynamoDbAsyncTable<OrderEntity> orderTable;
    @Mock private DynamoDbAsyncIndex<OrderEntity> orderIndex;
    @Mock private EventDynamoDBAdapter eventAdapter;
    @Mock private OrderDynamoDBAdapter orderAdapter;

    private EventRepositoryImpl eventRepository;
    private OrderRepositoryImpl orderRepository;
    private Event event;
    private Order order;

    @BeforeEach
    void setUp() {
        eventRepository = new EventRepositoryImpl(eventAdapter);
        orderRepository = new OrderRepositoryImpl(orderAdapter);
        event = Event.builder().eventId("event-1").name("Concert").totalCapacity(100).availableTickets(50).build();
        Instant reservedAt = Instant.parse("2026-12-01T20:00:00Z");
        Instant expiresAt = Instant.parse("2026-12-01T20:10:00Z");
        order = Order.builder().orderId("order-1").eventId("event-1").userId("user-1")
                .quantity(2).status(TicketStatus.RESERVED)
                .reservedAt(reservedAt).expiresAt(expiresAt).build();
    }

    // ── Adapter instantiation ─────────────────────────────────────────────────

    @Test
    void shouldInstantiateEventDynamoDBAdapter() {
        when(connectionFactory.table("events-dev", TableSchema.fromBean(EventEntity.class))).thenReturn(eventTable);
        assertNotNull(new EventDynamoDBAdapter(connectionFactory, dynamoDbAsyncClient, mapper, "events-dev"));
    }

    @Test
    void shouldInstantiateOrderDynamoDBAdapter() {
        when(connectionFactory.table("orders-dev", TableSchema.fromBean(OrderEntity.class))).thenReturn(orderTable);
        when(orderTable.index("status-expiresAt-index")).thenReturn(orderIndex);
        assertNotNull(new OrderDynamoDBAdapter(connectionFactory, mapper, dynamoDbAsyncClient, "orders-dev"));
    }

    // ── EventDynamoDBAdapter methods ──────────────────────────────────────────

    @Test
    void shouldFindEventById() {
        when(connectionFactory.table("events-dev", TableSchema.fromBean(EventEntity.class))).thenReturn(eventTable);
        EventEntity entity = new EventEntity();
        entity.setEventId("event-1");
        entity.setName("Concert");
        entity.setTotalCapacity(100);
        entity.setAvailableTickets(50);
        when(eventTable.getItem(any(Key.class))).thenReturn(CompletableFuture.completedFuture(entity));
        StepVerifier.create(new EventDynamoDBAdapter(connectionFactory, dynamoDbAsyncClient, mapper, "events-dev").findById("event-1"))
                .expectNext(event).verifyComplete();
    }

    @Test
    void shouldUpdateAvailableTicketsSuccess() {
        when(connectionFactory.table("events-dev", TableSchema.fromBean(EventEntity.class))).thenReturn(eventTable);
        EventEntity entity = new EventEntity();
        entity.setEventId("event-1");
        entity.setName("Concert");
        entity.setTotalCapacity(100);
        entity.setAvailableTickets(50);
        when(dynamoDbAsyncClient.updateItem(any(UpdateItemRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(UpdateItemResponse.builder().build()));
        when(eventTable.getItem(any(Key.class))).thenReturn(CompletableFuture.completedFuture(entity));
        StepVerifier.create(new EventDynamoDBAdapter(connectionFactory, dynamoDbAsyncClient, mapper, "events-dev").updateAvailableTickets("event-1", -2))
                .expectNext(event).verifyComplete();
    }

    @Test
    void shouldUpdateAvailableTicketsFailsOnConditionalCheck() {
        when(connectionFactory.table("events-dev", TableSchema.fromBean(EventEntity.class))).thenReturn(eventTable);
        when(dynamoDbAsyncClient.updateItem(any(UpdateItemRequest.class)))
                .thenReturn(CompletableFuture.failedFuture(
                        ConditionalCheckFailedException.builder().message("check failed").build()));
        StepVerifier.create(new EventDynamoDBAdapter(connectionFactory, dynamoDbAsyncClient, mapper, "events-dev").updateAvailableTickets("event-1", -5))
                .expectErrorMatches(e -> e instanceof IllegalStateException).verify();
    }

    // ── OrderDynamoDBAdapter methods ──────────────────────────────────────────

    @Test
    void shouldFindExpiredReservations() {
        when(connectionFactory.table("orders-dev", TableSchema.fromBean(OrderEntity.class))).thenReturn(orderTable);
        when(orderTable.index("status-expiresAt-index")).thenReturn(orderIndex);
        OrderEntity entity = new OrderEntity();
        entity.setOrderId("order-1");
        entity.setEventId("event-1");
        entity.setUserId("user-1");
        entity.setQuantity(2);
        entity.setStatus("RESERVED");
        entity.setReservedAt(order.getReservedAt().toString());
        entity.setExpiresAt(order.getExpiresAt().toString());
        Page<OrderEntity> page = Page.create(List.of(entity));
        SdkPublisher<Page<OrderEntity>> sdkPublisher = subscriber -> subscriber.onSubscribe(new Subscription() {
            public void request(long n) { subscriber.onNext(page); subscriber.onComplete(); }
            public void cancel() {}
        });
        when(orderIndex.query(any(QueryEnhancedRequest.class))).thenReturn(sdkPublisher);
        StepVerifier.create(new OrderDynamoDBAdapter(connectionFactory, mapper, dynamoDbAsyncClient, "orders-dev").findExpiredReservations("RESERVED", Instant.now()))
                .expectNext(order).verifyComplete();
    }

    @Test
    void shouldFindOrderById() {
        when(connectionFactory.table("orders-dev", TableSchema.fromBean(OrderEntity.class))).thenReturn(orderTable);
        when(orderTable.index("status-expiresAt-index")).thenReturn(orderIndex);
        OrderEntity entity = new OrderEntity();
        entity.setOrderId("order-1");
        entity.setEventId("event-1");
        entity.setUserId("user-1");
        entity.setQuantity(2);
        entity.setStatus("RESERVED");
        entity.setReservedAt(order.getReservedAt().toString());
        entity.setExpiresAt(order.getExpiresAt().toString());
        when(orderTable.getItem(any(Key.class))).thenReturn(CompletableFuture.completedFuture(entity));
        StepVerifier.create(new OrderDynamoDBAdapter(connectionFactory, mapper, dynamoDbAsyncClient, "orders-dev").findById("order-1"))
                .expectNext(order).verifyComplete();
    }

    // ── EventRepositoryImpl (gateway) ─────────────────────────────────────────

    @Test
    void shouldFindEventByIdViaRepository() {
        when(eventAdapter.findById("event-1")).thenReturn(Mono.just(event));
        StepVerifier.create(eventRepository.findById("event-1")).expectNext(event).verifyComplete();
    }

    @Test
    void shouldUpdateTicketsViaRepository() {
        when(eventAdapter.updateAvailableTickets(anyString(), anyInt())).thenReturn(Mono.just(event));
        StepVerifier.create(eventRepository.updateAvailableTickets("event-1", -2)).expectNext(event).verifyComplete();
    }

    @Test
    void shouldWrapErrorOnFindEventById() {
        when(eventAdapter.findById(anyString())).thenReturn(Mono.error(new RuntimeException("DB error")));
        StepVerifier.create(eventRepository.findById("event-1"))
                .expectErrorMatches(e -> e instanceof TechnicalException).verify();
    }

    @Test
    void shouldNotDoubleWrapOnFindEventById() {
        TechnicalException original = new TechnicalException(ErrorMessage.EVENT_NOT_FOUND);
        when(eventAdapter.findById(anyString())).thenReturn(Mono.error(original));
        StepVerifier.create(eventRepository.findById("event-1"))
                .expectErrorMatches(e -> e == original).verify();
    }

    @Test
    void shouldWrapErrorOnUpdateTickets() {
        when(eventAdapter.updateAvailableTickets(anyString(), anyInt()))
                .thenReturn(Mono.error(new RuntimeException("DB error")));
        StepVerifier.create(eventRepository.updateAvailableTickets("event-1", -2))
                .expectErrorMatches(e -> e instanceof TechnicalException).verify();
    }

    // ── OrderRepositoryImpl (gateway) ─────────────────────────────────────────

    @Test
    void shouldFindOrderByIdViaRepository() {
        when(orderAdapter.findById("order-1")).thenReturn(Mono.just(order));
        StepVerifier.create(orderRepository.findById("order-1")).expectNext(order).verifyComplete();
    }

    @Test
    void shouldSaveOrderViaRepository() {
        when(orderAdapter.save(any(Order.class))).thenReturn(Mono.just(order));
        StepVerifier.create(orderRepository.save(order)).expectNext(order).verifyComplete();
    }

    @Test
    void shouldFindExpiredReservationsViaRepository() {
        when(orderAdapter.findExpiredReservations(anyString(), any(Instant.class))).thenReturn(Flux.just(order));
        StepVerifier.create(orderRepository.findExpiredReservations("RESERVED", Instant.now()))
                .expectNext(order).verifyComplete();
    }

    @Test
    void shouldWrapErrorOnFindOrderById() {
        when(orderAdapter.findById(anyString())).thenReturn(Mono.error(new RuntimeException("DB error")));
        StepVerifier.create(orderRepository.findById("order-1"))
                .expectErrorMatches(e -> e instanceof TechnicalException).verify();
    }

    @Test
    void shouldWrapErrorOnSaveOrder() {
        when(orderAdapter.save(any(Order.class))).thenReturn(Mono.error(new RuntimeException("DB error")));
        StepVerifier.create(orderRepository.save(order))
                .expectErrorMatches(e -> e instanceof TechnicalException).verify();
    }

    @Test
    void shouldWrapErrorOnFindExpiredReservations() {
        when(orderAdapter.findExpiredReservations(anyString(), any(Instant.class)))
                .thenReturn(Flux.error(new RuntimeException("DB error")));
        StepVerifier.create(orderRepository.findExpiredReservations("RESERVED", Instant.now()))
                .expectErrorMatches(e -> e instanceof TechnicalException).verify();
    }

    @Test
    void shouldNotDoubleWrapOnSaveOrder() {
        TechnicalException original = new TechnicalException(ErrorMessage.ORDER_SAVE_ERROR);
        when(orderAdapter.save(any(Order.class))).thenReturn(Mono.error(original));
        StepVerifier.create(orderRepository.save(order))
                .expectErrorMatches(e -> e == original).verify();
    }
}
