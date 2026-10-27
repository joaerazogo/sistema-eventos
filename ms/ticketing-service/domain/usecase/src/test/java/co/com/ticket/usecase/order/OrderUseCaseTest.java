package co.com.ticket.usecase.order;

import co.com.ticket.model.enums.TicketStatus;
import co.com.ticket.model.order.Order;
import co.com.ticket.model.order.gateways.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;

import static org.mockito.Mockito.when;

class OrderUseCaseTest {

    @Mock
    private OrderRepository orderRepository;

    private OrderUseCase useCase;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        useCase = new OrderUseCase(orderRepository);
    }

    @Test
    void shouldReturnOrderById() {
        Order order = Order.builder()
                .orderId("order-1")
                .eventId("event-1")
                .status(TicketStatus.RESERVED)
                .reservedAt(Instant.now())
                .build();

        when(orderRepository.findById("order-1")).thenReturn(Mono.just(order));

        StepVerifier.create(useCase.execute("order-1"))
                .expectNext(order)
                .verifyComplete();
    }

    @Test
    void shouldReturnEmptyWhenOrderNotFound() {
        when(orderRepository.findById("nonexistent")).thenReturn(Mono.empty());

        StepVerifier.create(useCase.execute("nonexistent"))
                .verifyComplete();
    }

}
