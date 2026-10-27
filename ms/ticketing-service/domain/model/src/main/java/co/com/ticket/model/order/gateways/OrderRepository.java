package co.com.ticket.model.order.gateways;

import co.com.ticket.model.order.Order;
import reactor.core.publisher.Mono;

public interface OrderRepository {

    Mono<Order> save(Order order);
    Mono<Order> findById(String orderId);

}
