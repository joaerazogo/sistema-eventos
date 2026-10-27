package co.com.ticket.usecase.purchaseticket;

import co.com.ticket.model.enums.ErrorMessage;
import co.com.ticket.model.enums.TicketStatus;
import co.com.ticket.model.event.gateways.EventRepository;
import co.com.ticket.model.exception.BusinessException;
import co.com.ticket.model.order.Order;
import co.com.ticket.model.order.gateways.OrderPublisher;
import co.com.ticket.model.order.gateways.OrderRepository;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@RequiredArgsConstructor
public class PurchaseTicketUseCase {

    private final EventRepository eventRepository;
    private final OrderRepository orderRepository;
    private final OrderPublisher orderPublisher;

    public Mono<Order> execute(String eventId, String userId, int quantity) {
        if (quantity <= 0) {
            return Mono.error(new BusinessException(ErrorMessage.INVALID_QUANTITY));
        }
        return eventRepository.updateAvailableTickets(eventId, -quantity)
                .flatMap(event -> {
                    Instant now = Instant.now();
                    Order order = Order.builder()
                            .orderId(UUID.randomUUID().toString())
                            .eventId(eventId)
                            .userId(userId)
                            .quantity(quantity)
                            .status(TicketStatus.RESERVED)
                            .reservedAt(now)
                            .expiresAt(now.plus(10, ChronoUnit.MINUTES))
                            .build();
                    return orderRepository.save(order);
                })
                .flatMap(order -> orderPublisher.publish(order).thenReturn(order));
    }

}
