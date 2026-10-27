package co.com.ticketing.processor.dynamodb.order.gateway;

import co.com.ticketing.processor.dynamodb.adapter.OrderDynamoDBAdapter;
import co.com.ticketing.processor.model.enums.ErrorMessage;
import co.com.ticketing.processor.model.exception.TechnicalException;
import co.com.ticketing.processor.model.order.Order;
import co.com.ticketing.processor.model.order.gateways.OrderRepository;
import co.com.ticketing.processor.model.util.TicketingConstant;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.time.Instant;

@Service
@AllArgsConstructor
@Slf4j
public class OrderRepositoryImpl implements OrderRepository {

    private final OrderDynamoDBAdapter adapter;

    private static final Retry RETRY = Retry.backoff(3, Duration.ofMillis(100))
            .filter(e -> !(e instanceof TechnicalException));

    @Override
    public Mono<Order> findById(String orderId) {
        return adapter.findById(orderId)
                .retryWhen(RETRY)
                .doOnError(e -> log.error(TicketingConstant.LOG_FIND_ORDER_ERROR, orderId, e))
                .onErrorMap(e -> !(e instanceof TechnicalException),
                        e -> new TechnicalException(e, ErrorMessage.ORDER_NOT_FOUND));
    }

    @Override
    public Mono<Order> save(Order order) {
        return adapter.save(order)
                .retryWhen(RETRY)
                .doOnNext(saved -> log.info("Order {} saved with status {}", saved.getOrderId(), saved.getStatus()))
                .doOnError(e -> log.error(TicketingConstant.LOG_SAVE_ORDER_ERROR, order.getOrderId(), e))
                .onErrorMap(e -> !(e instanceof TechnicalException),
                        e -> new TechnicalException(e, ErrorMessage.ORDER_SAVE_ERROR));
    }

    @Override
    public Mono<Order> saveIfReserved(Order order) {
        return adapter.saveIfReserved(order)
                .retryWhen(RETRY)
                .doOnError(e -> log.error(TicketingConstant.LOG_SAVE_ORDER_ERROR, order.getOrderId(), e))
                .onErrorMap(e -> !(e instanceof TechnicalException),
                        e -> new TechnicalException(e, ErrorMessage.ORDER_SAVE_ERROR));
    }

    @Override
    public Flux<Order> findExpiredReservations(String status, Instant expiresAt) {
        return adapter.findExpiredReservations(status, expiresAt)
                .retryWhen(RETRY)
                .doOnError(e -> log.error(TicketingConstant.LOG_FIND_EXPIRED_ERROR, e))
                .onErrorMap(e -> !(e instanceof TechnicalException),
                        e -> new TechnicalException(e, ErrorMessage.EXPIRED_RESERVATIONS_ERROR));
    }
}
