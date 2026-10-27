package co.com.ticket.sqs.sender;

import co.com.ticket.model.order.Order;
import co.com.ticket.model.order.gateways.OrderPublisher;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@Log4j2
@RequiredArgsConstructor
public class OrderPublisherImpl implements OrderPublisher {

    private final SQSSender sqsSender;
    private final ObjectMapper objectMapper;

    @Override
    public Mono<String> publish(Order order) {
        return Mono.fromCallable(() -> objectMapper.writeValueAsString(order))
                .flatMap(sqsSender::send)
                .doOnNext(msgId -> log.info("Order {} published with messageId {}", order.getOrderId(), msgId))
                .doOnError(e -> log.error("Error publishing order {}", order.getOrderId(), e));
    }

}
