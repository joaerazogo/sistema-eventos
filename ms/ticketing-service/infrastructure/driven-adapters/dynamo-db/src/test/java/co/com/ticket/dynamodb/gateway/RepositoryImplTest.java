package co.com.ticket.dynamodb.gateway;

import co.com.ticket.dynamodb.adapter.DynamoDBTemplateAdapter;
import co.com.ticket.dynamodb.event.gateway.EventRepositoryImpl;
import co.com.ticket.dynamodb.adapter.OrderDynamoDBAdapter;
import co.com.ticket.dynamodb.order.gateway.OrderRepositoryImpl;
import co.com.ticket.model.enums.TicketStatus;
import co.com.ticket.model.event.Event;
import co.com.ticket.model.exception.BusinessException;
import co.com.ticket.model.exception.TechnicalException;
import co.com.ticket.model.order.Order;
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

    @Mock private DynamoDBTemplateAdapter eventAdapter;
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
    void shouldSaveEvent() {
        when(eventAdapter.save(any(Event.class))).thenReturn(Mono.just(event));
        StepVerifier.create(eventRepository.save(event)).expectNext(event).verifyComplete();
    }

    @Test
    void shouldFindEventById() {
        when(eventAdapter.getById("event-1")).thenReturn(Mono.just(event));
        StepVerifier.create(eventRepository.findById("event-1")).expectNext(event).verifyComplete();
    }

    @Test
    void shouldFindAllEvents() {
        when(eventAdapter.findAll()).thenReturn(Flux.just(event));
        StepVerifier.create(eventRepository.findAll()).expectNext(event).verifyComplete();
    }

    @Test
    void shouldUpdateAvailableTickets() {
        when(eventAdapter.updateAvailableTickets(anyString(), anyInt())).thenReturn(Mono.just(event));
        StepVerifier.create(eventRepository.updateAvailableTickets("event-1", -2)).expectNext(event).verifyComplete();
    }

    @Test
    void shouldSaveOrder() {
        when(orderAdapter.save(any(Order.class))).thenReturn(Mono.just(order));
        StepVerifier.create(orderRepository.save(order)).expectNext(order).verifyComplete();
    }

    @Test
    void shouldFindOrderById() {
        when(orderAdapter.getById("order-1")).thenReturn(Mono.just(order));
        StepVerifier.create(orderRepository.findById("order-1")).expectNext(order).verifyComplete();
    }

    @Test
    void shouldPropagateErrorOnSaveEvent() {
        when(eventAdapter.save(any())).thenReturn(Mono.error(new RuntimeException("DB error")));
        StepVerifier.create(eventRepository.save(event))
                .expectErrorMatches(e -> e instanceof TechnicalException)
                .verify();
    }

    @Test
    void shouldPropagateErrorOnFindOrder() {
        when(orderAdapter.getById(anyString())).thenReturn(Mono.error(new RuntimeException("DB error")));
        StepVerifier.create(orderRepository.findById("order-1"))
                .expectErrorMatches(e -> e instanceof TechnicalException)
                .verify();
    }

    @Test
    void shouldFindEventByIdReturnsEmpty() {
        when(eventAdapter.getById("nonexistent")).thenReturn(Mono.empty());
        StepVerifier.create(eventRepository.findById("nonexistent")).verifyComplete();
    }

    @Test
    void shouldPropagateErrorOnUpdateTickets() {
        when(eventAdapter.updateAvailableTickets(anyString(), anyInt()))
                .thenReturn(Mono.error(new IllegalStateException("No available tickets")));
        StepVerifier.create(eventRepository.updateAvailableTickets("event-1", -10))
                .expectErrorMatches(e -> e instanceof BusinessException)
                .verify();
    }

    @Test
    void shouldPropagateErrorOnFindAll() {
        when(eventAdapter.findAll()).thenReturn(Flux.error(new RuntimeException("DB error")));
        StepVerifier.create(eventRepository.findAll())
                .expectErrorMatches(e -> e instanceof TechnicalException)
                .verify();
    }

    @Test
    void shouldNotDoubleWrapTechnicalExceptionOnSaveEvent() {
        TechnicalException original = new TechnicalException(co.com.ticket.model.enums.ErrorMessage.EVENT_SAVE_ERROR);
        when(eventAdapter.save(any())).thenReturn(Mono.error(original));
        StepVerifier.create(eventRepository.save(event))
                .expectErrorMatches(e -> e == original)
                .verify();
    }

    @Test
    void shouldNotDoubleWrapTechnicalExceptionOnFindOrder() {
        TechnicalException original = new TechnicalException(co.com.ticket.model.enums.ErrorMessage.ORDER_NOT_FOUND);
        when(orderAdapter.getById(anyString())).thenReturn(Mono.error(original));
        StepVerifier.create(orderRepository.findById("order-1"))
                .expectErrorMatches(e -> e == original)
                .verify();
    }

    @Test
    void shouldNotDoubleWrapTechnicalExceptionOnUpdateTickets() {
        TechnicalException original = new TechnicalException(co.com.ticket.model.enums.ErrorMessage.TICKET_UPDATE_ERROR);
        when(eventAdapter.updateAvailableTickets(anyString(), anyInt())).thenReturn(Mono.error(original));
        StepVerifier.create(eventRepository.updateAvailableTickets("event-1", -2))
                .expectErrorMatches(e -> e == original)
                .verify();
    }

    @Test
    void shouldNotDoubleWrapTechnicalExceptionOnFindAll() {
        TechnicalException original = new TechnicalException(co.com.ticket.model.enums.ErrorMessage.INTERNAL_ERROR);
        when(eventAdapter.findAll()).thenReturn(Flux.error(original));
        StepVerifier.create(eventRepository.findAll())
                .expectErrorMatches(e -> e == original)
                .verify();
    }

    @Test
    void shouldExistsByVenueAndDate() {
        when(eventAdapter.existsByVenueAndDate("venue", "2026-12-01")).thenReturn(Mono.just(true));
        StepVerifier.create(eventRepository.existsByVenueAndDate("venue", "2026-12-01"))
                .expectNext(true).verifyComplete();
    }

    @Test
    void shouldPropagateErrorOnExistsByVenueAndDate() {
        when(eventAdapter.existsByVenueAndDate(anyString(), anyString()))
                .thenReturn(Mono.error(new RuntimeException("DB error")));
        StepVerifier.create(eventRepository.existsByVenueAndDate("venue", "2026-12-01"))
                .expectErrorMatches(e -> e instanceof TechnicalException).verify();
    }

    @Test
    void shouldNotDoubleWrapTechnicalExceptionOnExistsByVenueAndDate() {
        TechnicalException original = new TechnicalException(co.com.ticket.model.enums.ErrorMessage.INTERNAL_ERROR);
        when(eventAdapter.existsByVenueAndDate(anyString(), anyString())).thenReturn(Mono.error(original));
        StepVerifier.create(eventRepository.existsByVenueAndDate("venue", "2026-12-01"))
                .expectErrorMatches(e -> e == original).verify();
    }

    @Test
    void shouldNotDoubleWrapTechnicalExceptionOnFindEventById() {
        TechnicalException original = new TechnicalException(co.com.ticket.model.enums.ErrorMessage.EVENT_NOT_FOUND);
        when(eventAdapter.getById(anyString())).thenReturn(Mono.error(original));
        StepVerifier.create(eventRepository.findById("event-1"))
                .expectErrorMatches(e -> e == original).verify();
    }

    @Test
    void shouldNotDoubleWrapTechnicalExceptionOnSaveOrder() {
        TechnicalException original = new TechnicalException(co.com.ticket.model.enums.ErrorMessage.ORDER_SAVE_ERROR);
        when(orderAdapter.save(any(Order.class))).thenReturn(Mono.error(original));
        StepVerifier.create(orderRepository.save(order))
                .expectErrorMatches(e -> e == original).verify();
    }

    @Test
    void shouldNotDoubleWrapTechnicalExceptionOnFindOrderById() {
        TechnicalException original = new TechnicalException(co.com.ticket.model.enums.ErrorMessage.ORDER_NOT_FOUND);
        when(orderAdapter.getById(anyString())).thenReturn(Mono.error(original));
        StepVerifier.create(orderRepository.findById("order-1"))
                .expectErrorMatches(e -> e == original).verify();
    }
}
