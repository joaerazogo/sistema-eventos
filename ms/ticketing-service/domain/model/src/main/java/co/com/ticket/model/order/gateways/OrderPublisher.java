package co.com.ticket.model.order.gateways;

import co.com.ticket.model.order.Order;
import reactor.core.publisher.Mono;

public interface OrderPublisher {

    Mono<String> publish(Order order);

}
