package co.com.ticketing.processor.usecase.processorder;

import co.com.ticketing.processor.model.enums.TicketStatus;
import co.com.ticketing.processor.model.event.gateways.EventRepository;
import co.com.ticketing.processor.model.order.Order;
import co.com.ticketing.processor.model.order.gateways.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProcessOrderUseCaseTest {

    @Mock private OrderRepository orderRepository;
    @Mock private EventRepository eventRepository;

    private ProcessOrderUseCase useCase;

    private static final String ORDER_ID = "order-1";
    private static final String EVENT_ID = "event-1";

    @BeforeEach
    void setUp() {
        useCase = new ProcessOrderUseCase(orderRepository, eventRepository);
    }

    @Test
    void shouldReturnOrderUnchangedWhenNotReserved() {
        Order order = buildOrder(TicketStatus.SOLD);
        when(orderRepository.findById(ORDER_ID)).thenReturn(Mono.just(order));

        StepVerifier.create(useCase.execute(ORDER_ID))
                .expectNextMatches(o -> o.getStatus() == TicketStatus.SOLD)
                .verifyComplete();

        verify(orderRepository, never()).save(any());
        verify(eventRepository, never()).findById(any());
    }

    @Test
    void shouldTransitionReservedThroughPendingConfirmationToSold() {
        Order reserved = buildOrder(TicketStatus.RESERVED);

        when(orderRepository.findById(ORDER_ID)).thenReturn(Mono.just(reserved));
        when(orderRepository.save(any())).thenAnswer(inv -> {
            Order saved = inv.getArgument(0);
            return Mono.just(saved);
        });

        StepVerifier.create(useCase.execute(ORDER_ID))
                .expectNextMatches(o -> o.getStatus() == TicketStatus.SOLD)
                .verifyComplete();

        verify(orderRepository, times(2)).save(any());
        verify(eventRepository, never()).findById(any());
    }

    @Test
    void shouldReleaseInventoryWhenOrderExpired() {
        Order expired = Order.builder()
                .orderId(ORDER_ID).eventId(EVENT_ID).userId("user-1")
                .quantity(2).status(TicketStatus.RESERVED)
                .reservedAt(Instant.now().minusSeconds(700))
                .expiresAt(Instant.now().minusSeconds(100))
                .build();

        when(orderRepository.findById(ORDER_ID)).thenReturn(Mono.just(expired));
        when(orderRepository.saveIfReserved(any())).thenReturn(Mono.just(expired.toBuilder().status(TicketStatus.AVAILABLE).build()));
        when(eventRepository.updateAvailableTickets(eq(EVENT_ID), eq(2))).thenReturn(Mono.empty());

        StepVerifier.create(useCase.execute(ORDER_ID))
                .expectNextMatches(o -> o.getStatus() == TicketStatus.AVAILABLE)
                .verifyComplete();

        verify(orderRepository).saveIfReserved(any());
        verify(eventRepository).updateAvailableTickets(EVENT_ID, 2);
        verify(orderRepository, never()).save(any());
    }

    @Test
    void shouldReturnOrderWhenExpiredButAlreadyProcessed() {
        Order expired = Order.builder()
                .orderId(ORDER_ID).eventId(EVENT_ID).userId("user-1")
                .quantity(2).status(TicketStatus.RESERVED)
                .reservedAt(Instant.now().minusSeconds(700))
                .expiresAt(Instant.now().minusSeconds(100))
                .build();

        when(orderRepository.findById(ORDER_ID)).thenReturn(Mono.just(expired));
        when(orderRepository.saveIfReserved(any())).thenReturn(Mono.empty());

        StepVerifier.create(useCase.execute(ORDER_ID))
                .expectNextMatches(o -> o.getStatus() == TicketStatus.RESERVED)
                .verifyComplete();

        verify(eventRepository, never()).updateAvailableTickets(any(), anyInt());
    }

    private Order buildOrder(TicketStatus status) {
        return Order.builder()
                .orderId(ORDER_ID).eventId(EVENT_ID).userId("user-1")
                .quantity(2).status(status)
                .reservedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(600))
                .build();
    }
}
