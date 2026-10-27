package co.com.ticketing.processor.usecase.processorder;

import co.com.ticketing.processor.model.enums.TicketStatus;
import co.com.ticketing.processor.model.event.gateways.EventRepository;
import co.com.ticketing.processor.model.order.Order;
import co.com.ticketing.processor.model.order.gateways.OrderRepository;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import java.time.Instant;

@RequiredArgsConstructor
public class ProcessOrderUseCase {

    private final OrderRepository orderRepository;
    private final EventRepository eventRepository;

    public Mono<Order> execute(String orderId) {
        return orderRepository.findById(orderId)
                .flatMap(this::processOrder);
    }

    private Mono<Order> processOrder(Order order) {
        if (order.getStatus() != TicketStatus.RESERVED) {
            return Mono.just(order);
        }
        if (order.getExpiresAt() != null && Instant.now().isAfter(order.getExpiresAt())) {
            Order expired = order.toBuilder().status(TicketStatus.AVAILABLE).build();
            return orderRepository.saveIfReserved(expired)
                    .flatMap(saved -> eventRepository.updateAvailableTickets(order.getEventId(), order.getQuantity())
                            .thenReturn(saved))
                    .switchIfEmpty(Mono.just(order));
        }
        return markAsPendingConfirmation(order)
                .flatMap(this::confirmOrder);
    }

    private Mono<Order> markAsPendingConfirmation(Order order) {
        return orderRepository.save(order.toBuilder()
                .status(TicketStatus.PENDING_CONFIRMATION)
                .build());
    }

    private Mono<Order> confirmOrder(Order order) {
        return orderRepository.save(order.toBuilder()
                .status(TicketStatus.SOLD)
                .build());
    }

}
