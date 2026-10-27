package co.com.ticketing.processor.usecase.releaseexpiredreservations;

import co.com.ticketing.processor.model.enums.TicketStatus;
import co.com.ticketing.processor.model.event.gateways.EventRepository;
import co.com.ticketing.processor.model.order.Order;
import co.com.ticketing.processor.model.order.gateways.OrderRepository;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReleaseExpiredReservationsUseCaseTest {

    @Mock private OrderRepository orderRepository;
    @Mock private EventRepository eventRepository;

    private ReleaseExpiredReservationsUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ReleaseExpiredReservationsUseCase(orderRepository, eventRepository);
    }

    @Test
    void shouldReleaseExpiredReservationsAndRestoreTickets() {
        Order expired = Order.builder()
                .orderId("order-1").eventId("event-1").userId("user-1")
                .quantity(3).status(TicketStatus.RESERVED)
                .reservedAt(Instant.now().minusSeconds(700))
                .expiresAt(Instant.now().minusSeconds(100))
                .build();
        Order released = expired.toBuilder().status(TicketStatus.AVAILABLE).build();

        when(orderRepository.findExpiredReservations(eq(TicketStatus.RESERVED.name()), any(Instant.class)))
                .thenReturn(Flux.just(expired));
        when(orderRepository.saveIfReserved(any())).thenReturn(Mono.just(released));
        when(eventRepository.updateAvailableTickets(eq("event-1"), eq(3))).thenReturn(Mono.empty());

        StepVerifier.create(useCase.execute())
                .expectNextMatches(o -> o.getStatus() == TicketStatus.AVAILABLE && o.getOrderId().equals("order-1"))
                .verifyComplete();

        verify(eventRepository).updateAvailableTickets("event-1", 3);
    }

    @Test
    void shouldSkipReleaseWhenOrderAlreadyProcessed() {
        Order expired = Order.builder()
                .orderId("order-1").eventId("event-1").userId("user-1")
                .quantity(3).status(TicketStatus.RESERVED)
                .reservedAt(Instant.now().minusSeconds(700))
                .expiresAt(Instant.now().minusSeconds(100))
                .build();

        when(orderRepository.findExpiredReservations(eq(TicketStatus.RESERVED.name()), any(Instant.class)))
                .thenReturn(Flux.just(expired));
        when(orderRepository.saveIfReserved(any())).thenReturn(Mono.empty());

        StepVerifier.create(useCase.execute())
                .verifyComplete();

        verify(eventRepository, never()).updateAvailableTickets(any(), anyInt());
    }

    @Test
    void shouldReturnEmptyWhenNoExpiredReservations() {
        when(orderRepository.findExpiredReservations(any(), any())).thenReturn(Flux.empty());

        StepVerifier.create(useCase.execute())
                .verifyComplete();

        verify(orderRepository, never()).save(any());
    }
}
