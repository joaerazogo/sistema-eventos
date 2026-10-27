package co.com.ticket.usecase.order;

import co.com.ticket.model.order.Order;
import co.com.ticket.model.order.gateways.OrderRepository;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
public class OrderUseCase {

    private final OrderRepository orderRepository;

    public Mono<Order> execute(String orderId) {
        return orderRepository.findById(orderId);
    }

}
