package co.com.ticketing.processor.dynamodb.gateway;

import co.com.ticketing.processor.dynamodb.adapter.EventDynamoDBAdapter;
import co.com.ticketing.processor.dynamodb.adapter.OrderDynamoDBAdapter;
import co.com.ticketing.processor.dynamodb.event.gateway.EventRepositoryImpl;
import co.com.ticketing.processor.dynamodb.order.gateway.OrderRepositoryImpl;
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
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RepositoryImplTest {

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
        order = Order.builder().orderId("order-1").eventId("event-1").userId("user-1")
                .quantity(2).status(TicketStatus.RESERVED)
                .reservedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(600)).build();
    }

    @Test
    void shouldFindEventById() {
        when(eventAdapter.findById("event-1")).thenReturn(Mono.just(event));
        StepVerifier.create(eventRepository.findById("event-1")).expectNext(event).verifyComplete();
    }

    @Test
    void shouldUpdateAvailableTickets() {
        when(eventAdapter.updateAvailableTickets(anyString(), anyInt())).thenReturn(Mono.just(event));
        StepVerifier.create(eventRepository.updateAvailableTickets("event-1", -2)).expectNext(event).verifyComplete();
    }

    @Test
    void shouldFindOrderById() {
        when(orderAdapter.findById("order-1")).thenReturn(Mono.just(order));
        StepVerifier.create(orderRepository.findById("order-1")).expectNext(order).verifyComplete();
    }

    @Test
    void shouldSaveOrder() {
        when(orderAdapter.save(any(Order.class))).thenReturn(Mono.just(order));
        StepVerifier.create(orderRepository.save(order)).expectNext(order).verifyComplete();
    }

    @Test
    void shouldFindExpiredReservations() {
        when(orderAdapter.findExpiredReservations(anyString(), any(Instant.class))).thenReturn(Flux.just(order));
        StepVerifier.create(orderRepository.findExpiredReservations("RESERVED", Instant.now()))
                .expectNext(order).verifyComplete();
    }

    @Test
    void shouldPropagateErrorOnFindEventById() {
        when(eventAdapter.findById(anyString())).thenReturn(Mono.error(new RuntimeException("DB error")));
        StepVerifier.create(eventRepository.findById("event-1"))
                .expectErrorMatches(e -> e instanceof TechnicalException).verify();
    }

    @Test
    void shouldPropagateErrorOnUpdateTickets() {
        when(eventAdapter.updateAvailableTickets(anyString(), anyInt()))
                .thenReturn(Mono.error(new RuntimeException("DB error")));
        StepVerifier.create(eventRepository.updateAvailableTickets("event-1", -2))
                .expectErrorMatches(e -> e instanceof TechnicalException).verify();
    }

    @Test
    void shouldPropagateErrorOnFindOrderById() {
        when(orderAdapter.findById(anyString())).thenReturn(Mono.error(new RuntimeException("DB error")));
        StepVerifier.create(orderRepository.findById("order-1"))
                .expectErrorMatches(e -> e instanceof TechnicalException).verify();
    }

    @Test
    void shouldPropagateErrorOnSaveOrder() {
        when(orderAdapter.save(any(Order.class))).thenReturn(Mono.error(new RuntimeException("DB error")));
        StepVerifier.create(orderRepository.save(order))
                .expectErrorMatches(e -> e instanceof TechnicalException).verify();
    }

    @Test
    void shouldPropagateErrorOnFindExpiredReservations() {
        when(orderAdapter.findExpiredReservations(anyString(), any(Instant.class)))
                .thenReturn(Flux.error(new RuntimeException("DB error")));
        StepVerifier.create(orderRepository.findExpiredReservations("RESERVED", Instant.now()))
                .expectErrorMatches(e -> e instanceof TechnicalException).verify();
    }

    @Test
    void shouldNotDoubleWrapTechnicalExceptionOnFindEventById() {
        TechnicalException original = new TechnicalException(ErrorMessage.EVENT_NOT_FOUND);
        when(eventAdapter.findById(anyString())).thenReturn(Mono.error(original));
        StepVerifier.create(eventRepository.findById("event-1"))
                .expectErrorMatches(e -> e == original).verify();
    }

    @Test
    void shouldNotDoubleWrapTechnicalExceptionOnSaveOrder() {
        TechnicalException original = new TechnicalException(ErrorMessage.ORDER_SAVE_ERROR);
        when(orderAdapter.save(any(Order.class))).thenReturn(Mono.error(original));
        StepVerifier.create(orderRepository.save(order))
                .expectErrorMatches(e -> e == original).verify();
    }

    @Test
    void shouldNotDoubleWrapTechnicalExceptionOnFindExpiredReservations() {
        TechnicalException original = new TechnicalException(ErrorMessage.EXPIRED_RESERVATIONS_ERROR);
        when(orderAdapter.findExpiredReservations(anyString(), any(Instant.class))).thenReturn(Flux.error(original));
        StepVerifier.create(orderRepository.findExpiredReservations("RESERVED", Instant.now()))
                .expectErrorMatches(e -> e == original).verify();
    }

    @Test
    void shouldSaveIfReserved() {
        when(orderAdapter.saveIfReserved(any(Order.class))).thenReturn(Mono.just(order));
        StepVerifier.create(orderRepository.saveIfReserved(order)).expectNext(order).verifyComplete();
    }

    @Test
    void shouldPropagateErrorOnSaveIfReserved() {
        when(orderAdapter.saveIfReserved(any(Order.class))).thenReturn(Mono.error(new RuntimeException("DB error")));
        StepVerifier.create(orderRepository.saveIfReserved(order))
                .expectErrorMatches(e -> e instanceof TechnicalException).verify();
    }

    @Test
    void shouldNotDoubleWrapTechnicalExceptionOnSaveIfReserved() {
        TechnicalException original = new TechnicalException(ErrorMessage.ORDER_SAVE_ERROR);
        when(orderAdapter.saveIfReserved(any(Order.class))).thenReturn(Mono.error(original));
        StepVerifier.create(orderRepository.saveIfReserved(order))
                .expectErrorMatches(e -> e == original).verify();
    }

    @Test
    void shouldNotDoubleWrapTechnicalExceptionOnFindOrderById() {
        TechnicalException original = new TechnicalException(ErrorMessage.ORDER_NOT_FOUND);
        when(orderAdapter.findById(anyString())).thenReturn(Mono.error(original));
        StepVerifier.create(orderRepository.findById("order-1"))
                .expectErrorMatches(e -> e == original).verify();
    }

    @Test
    void shouldNotDoubleWrapTechnicalExceptionOnUpdateTickets() {
        TechnicalException original = new TechnicalException(ErrorMessage.TICKET_UPDATE_ERROR);
        when(eventAdapter.updateAvailableTickets(anyString(), anyInt())).thenReturn(Mono.error(original));
        StepVerifier.create(eventRepository.updateAvailableTickets("event-1", -2))
                .expectErrorMatches(e -> e == original).verify();
    }
}
