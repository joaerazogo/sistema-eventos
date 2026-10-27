package co.com.ticket.dynamodb.event.gateway;

import co.com.ticket.dynamodb.adapter.DynamoDBTemplateAdapter;
import co.com.ticket.model.enums.ErrorMessage;
import co.com.ticket.model.event.Event;
import co.com.ticket.model.event.gateways.EventRepository;
import co.com.ticket.model.exception.BusinessException;
import co.com.ticket.model.exception.TechnicalException;
import co.com.ticket.model.util.TicketingConstant;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;

@Service
@AllArgsConstructor
@Slf4j
public class EventRepositoryImpl implements EventRepository {

    private final DynamoDBTemplateAdapter adapter;

    private static final Retry RETRY = Retry.backoff(3, Duration.ofMillis(100))
            .filter(e -> !(e instanceof TechnicalException) && !(e instanceof BusinessException));

    @Override
    public Mono<Event> save(Event event) {
        return adapter.save(event)
                .retryWhen(RETRY)
                .doOnError(e -> log.error(TicketingConstant.LOG_SAVE_EVENT_ERROR, e))
                .onErrorMap(e -> !(e instanceof TechnicalException),
                        e -> new TechnicalException(e, ErrorMessage.EVENT_SAVE_ERROR));
    }

    @Override
    public Mono<Event> findById(String eventId) {
        return adapter.getById(eventId)
                .retryWhen(RETRY)
                .doOnError(e -> log.error(TicketingConstant.LOG_FIND_EVENT_ERROR, eventId, e))
                .onErrorMap(e -> !(e instanceof TechnicalException),
                        e -> new TechnicalException(e, ErrorMessage.EVENT_NOT_FOUND));
    }

    @Override
    public Flux<Event> findAll() {
        return adapter.findAll()
                .retryWhen(RETRY)
                .doOnError(e -> log.error(TicketingConstant.LOG_FIND_ALL_EVENTS_ERROR, e))
                .onErrorMap(e -> !(e instanceof TechnicalException),
                        e -> new TechnicalException(e, ErrorMessage.INTERNAL_ERROR));
    }

    @Override
    public Mono<Boolean> existsByVenueAndDate(String venue, String date) {
        return adapter.existsByVenueAndDate(venue, date)
                .retryWhen(RETRY)
                .doOnError(e -> log.error(TicketingConstant.LOG_FIND_EVENT_ERROR, venue, e))
                .onErrorMap(e -> !(e instanceof TechnicalException),
                        e -> new TechnicalException(e, ErrorMessage.INTERNAL_ERROR));
    }

    @Override
    public Mono<Event> updateAvailableTickets(String eventId, int delta) {
        return adapter.updateAvailableTickets(eventId, delta)
                .doOnError(e -> log.error(TicketingConstant.LOG_UPDATE_TICKETS_ERROR, eventId, e))
                .onErrorMap(IllegalStateException.class, e -> new BusinessException(ErrorMessage.NO_AVAILABLE_TICKETS))
                .onErrorMap(e -> !(e instanceof TechnicalException) && !(e instanceof BusinessException),
                        e -> new TechnicalException(e, ErrorMessage.TICKET_UPDATE_ERROR));
    }
}
