package co.com.ticketing.processor.sqs;

import co.com.ticketing.processor.model.exception.TechnicalException;
import co.com.ticketing.processor.model.exception.TicketingException;
import co.com.ticketing.processor.model.util.TicketingConstant;
import co.com.ticketing.processor.usecase.processorder.ProcessOrderUseCase;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;
import software.amazon.awssdk.services.sqs.model.Message;

import java.time.Duration;
import java.util.function.Function;

@Service
@Slf4j
@RequiredArgsConstructor
public class SQSProcessor implements Function<Message, Mono<Void>> {

    private final ProcessOrderUseCase processOrderUseCase;
    private final ObjectMapper objectMapper;

    @Override
    public Mono<Void> apply(Message message) {
        log.info(TicketingConstant.LOG_SQS_MESSAGE_RECEIVED, message.messageId());
        return Mono.fromCallable(() -> extractOrderId(message.body()))
                .flatMap(processOrderUseCase::execute)
                .doOnNext(order -> log.info(TicketingConstant.LOG_SQS_ORDER_PROCESSED,
                        order.getOrderId(), order.getStatus()))
                .doOnError(TicketingException.class,
                        e -> log.error(TicketingConstant.LOG_SQS_MESSAGE_ERROR, message.messageId(), e))
                .doOnError(e -> !(e instanceof TicketingException),
                        e -> log.error(TicketingConstant.LOG_SQS_MESSAGE_ERROR, message.messageId(), e))
                .retryWhen(Retry.backoff(3, Duration.ofMillis(200))
                        .filter(e -> e instanceof TechnicalException)
                        .onRetryExhaustedThrow((spec, signal) -> signal.failure()))
                .onErrorResume(e -> !(e instanceof TechnicalException), e -> Mono.empty())
                .then();
    }

    private String extractOrderId(String body) throws Exception {
        JsonNode node = objectMapper.readTree(body);
        return node.get("orderId").asText();
    }
}
