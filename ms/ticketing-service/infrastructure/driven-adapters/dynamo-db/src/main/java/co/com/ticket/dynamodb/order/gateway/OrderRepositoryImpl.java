package co.com.ticket.dynamodb.order.gateway;

import co.com.ticket.dynamodb.adapter.OrderDynamoDBAdapter;
import co.com.ticket.model.enums.ErrorMessage;
import co.com.ticket.model.exception.TechnicalException;
import co.com.ticket.model.order.Order;
import co.com.ticket.model.order.gateways.OrderRepository;
import co.com.ticket.model.util.TicketingConstant;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;

@Service
@AllArgsConstructor
@Slf4j
public class OrderRepositoryImpl implements OrderRepository {

    private final OrderDynamoDBAdapter adapter;

    private static final Retry RETRY = Retry.backoff(3, Duration.ofMillis(100))
            .filter(e -> !(e instanceof TechnicalException));

    @Override
    public Mono<Order> save(Order order) {
        return adapter.save(order)
                .retryWhen(RETRY)
                .doOnError(e -> log.error(TicketingConstant.LOG_SAVE_ORDER_ERROR, e))
                .onErrorMap(e -> !(e instanceof TechnicalException),
                        e -> new TechnicalException(e, ErrorMessage.ORDER_SAVE_ERROR));
    }

    @Override
    public Mono<Order> findById(String orderId) {
        return adapter.getById(orderId)
                .retryWhen(RETRY)
                .doOnError(e -> log.error(TicketingConstant.LOG_FIND_ORDER_ERROR, orderId, e))
                .onErrorMap(e -> !(e instanceof TechnicalException),
                        e -> new TechnicalException(e, ErrorMessage.ORDER_NOT_FOUND));
    }
}
