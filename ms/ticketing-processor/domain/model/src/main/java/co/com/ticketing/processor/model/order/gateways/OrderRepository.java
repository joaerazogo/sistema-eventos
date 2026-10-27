package co.com.ticketing.processor.model.order.gateways;

import co.com.ticketing.processor.model.order.Order;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;

public interface OrderRepository {

    Mono<Order> findById(String orderId);
    Mono<Order> save(Order order);
    Mono<Order> saveIfReserved(Order order);
    Flux<Order> findExpiredReservations(String status, Instant expiresAt);

}
