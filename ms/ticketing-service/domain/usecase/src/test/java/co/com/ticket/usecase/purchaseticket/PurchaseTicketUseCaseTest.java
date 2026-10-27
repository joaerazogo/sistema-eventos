package co.com.ticket.usecase.purchaseticket;

import co.com.ticket.model.enums.ErrorMessage;
import co.com.ticket.model.enums.TicketStatus;
import co.com.ticket.model.event.Event;
import co.com.ticket.model.exception.BusinessException;
import co.com.ticket.model.event.gateways.EventRepository;
import co.com.ticket.model.order.Order;
import co.com.ticket.model.order.gateways.OrderPublisher;
import co.com.ticket.model.order.gateways.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class PurchaseTicketUseCaseTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderPublisher orderPublisher;

    private PurchaseTicketUseCase useCase;

    private Event event;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        useCase = new PurchaseTicketUseCase(eventRepository, orderRepository, orderPublisher);

        event = Event.builder()
                .eventId("event-1")
                .name("Concert")
                .totalCapacity(100)
                .availableTickets(50)
                .build();
    }

    @Test
    void shouldCreateReservedOrderAndPublishToSqs() {
        when(eventRepository.updateAvailableTickets("event-1", -2)).thenReturn(Mono.just(event));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        when(orderPublisher.publish(any(Order.class))).thenReturn(Mono.just("msg-id-123"));

        StepVerifier.create(useCase.execute("event-1", "user-1", 2))
                .assertNext(order -> {
                    assertThat(order.getOrderId()).isNotNull();
                    assertThat(order.getStatus()).isEqualTo(TicketStatus.RESERVED);
                    assertThat(order.getEventId()).isEqualTo("event-1");
                    assertThat(order.getUserId()).isEqualTo("user-1");
                    assertThat(order.getQuantity()).isEqualTo(2);
                    assertThat(order.getExpiresAt()).isAfter(order.getReservedAt());
                })
                .verifyComplete();
    }

    @Test
    void shouldFailWhenNoAvailableTickets() {
        when(eventRepository.updateAvailableTickets(anyString(), anyInt()))
                .thenReturn(Mono.error(new IllegalStateException("No available tickets for event event-1")));

        StepVerifier.create(useCase.execute("event-1", "user-1", 2))
                .expectErrorMatches(e -> e instanceof IllegalStateException
                        && e.getMessage().contains("No available tickets"))
                .verify();
    }

    @Test
    void shouldFailWhenQuantityIsZero() {
        StepVerifier.create(useCase.execute("event-1", "user-1", 0))
                .expectErrorMatches(e -> e instanceof BusinessException
                        && ((BusinessException) e).getErrorMessage() == ErrorMessage.INVALID_QUANTITY)
                .verify();
    }

    @Test
    void shouldFailWhenQuantityIsNegative() {
        StepVerifier.create(useCase.execute("event-1", "user-1", -1))
                .expectErrorMatches(e -> e instanceof BusinessException
                        && ((BusinessException) e).getErrorMessage() == ErrorMessage.INVALID_QUANTITY)
                .verify();
    }

    @Test
    void shouldFailWhenPublisherFails() {
        when(eventRepository.updateAvailableTickets("event-1", -2)).thenReturn(Mono.just(event));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        when(orderPublisher.publish(any(Order.class))).thenReturn(Mono.error(new RuntimeException("SQS error")));

        StepVerifier.create(useCase.execute("event-1", "user-1", 2))
                .expectErrorMessage("SQS error")
                .verify();
    }

    @Test
    void shouldHandleConcurrentPurchasesWithOptimisticLocking() {
        int totalTickets = 10;
        int concurrentRequests = 20;
        AtomicInteger availableTickets = new AtomicInteger(totalTickets);

        when(eventRepository.updateAvailableTickets(anyString(), anyInt()))
                .thenAnswer(inv -> {
                    int remaining = availableTickets.addAndGet((int) inv.getArgument(1));
                    if (remaining < 0) {
                        availableTickets.addAndGet(-(int) inv.getArgument(1));
                        return Mono.error(new IllegalStateException("No available tickets"));
                    }
                    return Mono.just(event);
                });
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        when(orderPublisher.publish(any(Order.class))).thenReturn(Mono.just("msg-id"));

        long successCount = Flux.range(0, concurrentRequests)
                .flatMap(i -> useCase.execute("event-1", "user-" + i, 1).onErrorResume(e -> Mono.empty()))
                .collectList()
                .block()
                .size();

        assertThat(successCount).isEqualTo(totalTickets);
        assertThat(availableTickets.get()).isZero();
    }

}
