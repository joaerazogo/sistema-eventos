package co.com.ticket.usecase.assigncomplimentary;

import co.com.ticket.model.enums.ErrorMessage;
import co.com.ticket.model.enums.TicketStatus;
import co.com.ticket.model.exception.BusinessException;
import co.com.ticket.model.order.Order;
import co.com.ticket.model.order.gateways.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class AssignComplimentaryUseCaseTest {

    @Mock
    private OrderRepository orderRepository;

    private AssignComplimentaryUseCase useCase;

    private Order reservedOrder;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        useCase = new AssignComplimentaryUseCase(orderRepository);

        reservedOrder = Order.builder()
                .orderId("order-1")
                .eventId("event-1")
                .userId("user-1")
                .quantity(2)
                .status(TicketStatus.RESERVED)
                .reservedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(600))
                .build();
    }

    @Test
    void shouldTransitionReservedOrderToComplimentary() {
        when(orderRepository.findById("order-1")).thenReturn(Mono.just(reservedOrder));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(useCase.execute("order-1"))
                .assertNext(order -> {
                    assertThat(order.getOrderId()).isEqualTo("order-1");
                    assertThat(order.getStatus()).isEqualTo(TicketStatus.COMPLIMENTARY);
                })
                .verifyComplete();
    }

    @Test
    void shouldFailWhenOrderNotFound() {
        when(orderRepository.findById("order-x")).thenReturn(Mono.empty());

        StepVerifier.create(useCase.execute("order-x"))
                .expectErrorMatches(e -> e instanceof BusinessException
                        && ((BusinessException) e).getErrorMessage() == ErrorMessage.ORDER_NOT_FOUND)
                .verify();
    }

    @Test
    void shouldFailWhenOrderIsAlreadySold() {
        Order soldOrder = reservedOrder.toBuilder().status(TicketStatus.SOLD).build();
        when(orderRepository.findById("order-1")).thenReturn(Mono.just(soldOrder));

        StepVerifier.create(useCase.execute("order-1"))
                .expectErrorMatches(e -> e instanceof BusinessException
                        && ((BusinessException) e).getErrorMessage() == ErrorMessage.ORDER_NOT_ASSIGNABLE)
                .verify();
    }

    @Test
    void shouldFailWhenOrderIsPendingConfirmation() {
        Order pendingOrder = reservedOrder.toBuilder().status(TicketStatus.PENDING_CONFIRMATION).build();
        when(orderRepository.findById("order-1")).thenReturn(Mono.just(pendingOrder));

        StepVerifier.create(useCase.execute("order-1"))
                .expectErrorMatches(e -> e instanceof BusinessException
                        && ((BusinessException) e).getErrorMessage() == ErrorMessage.ORDER_NOT_ASSIGNABLE)
                .verify();
    }

    @Test
    void shouldFailWhenOrderIsAlreadyComplimentary() {
        Order complimentaryOrder = reservedOrder.toBuilder().status(TicketStatus.COMPLIMENTARY).build();
        when(orderRepository.findById("order-1")).thenReturn(Mono.just(complimentaryOrder));

        StepVerifier.create(useCase.execute("order-1"))
                .expectErrorMatches(e -> e instanceof BusinessException
                        && ((BusinessException) e).getErrorMessage() == ErrorMessage.ORDER_NOT_ASSIGNABLE)
                .verify();
    }

}
