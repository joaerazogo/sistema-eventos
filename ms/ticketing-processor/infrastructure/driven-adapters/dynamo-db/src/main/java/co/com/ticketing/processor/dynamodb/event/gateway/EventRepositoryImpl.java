package co.com.ticketing.processor.dynamodb.event.gateway;

import co.com.ticketing.processor.dynamodb.adapter.EventDynamoDBAdapter;
import co.com.ticketing.processor.model.enums.ErrorMessage;
import co.com.ticketing.processor.model.event.Event;
import co.com.ticketing.processor.model.event.gateways.EventRepository;
import co.com.ticketing.processor.model.exception.TechnicalException;
import co.com.ticketing.processor.model.util.TicketingConstant;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;

@Service
@AllArgsConstructor
@Slf4j
public class EventRepositoryImpl implements EventRepository {

    private final EventDynamoDBAdapter adapter;

    private static final Retry RETRY = Retry.backoff(3, Duration.ofMillis(100))
            .filter(e -> !(e instanceof TechnicalException));

    @Override
    public Mono<Event> findById(String eventId) {
        return adapter.findById(eventId)
                .retryWhen(RETRY)
                .doOnError(e -> log.error(TicketingConstant.LOG_FIND_EVENT_ERROR, eventId, e))
                .onErrorMap(e -> !(e instanceof TechnicalException),
                        e -> new TechnicalException(e, ErrorMessage.EVENT_NOT_FOUND));
    }

    @Override
    public Mono<Event> updateAvailableTickets(String eventId, int delta) {
        return adapter.updateAvailableTickets(eventId, delta)
                .doOnError(e -> log.error(TicketingConstant.LOG_UPDATE_TICKETS_ERROR, eventId, e))
                .onErrorMap(e -> !(e instanceof TechnicalException),
                        e -> new TechnicalException(e, ErrorMessage.TICKET_UPDATE_ERROR));
    }
}
