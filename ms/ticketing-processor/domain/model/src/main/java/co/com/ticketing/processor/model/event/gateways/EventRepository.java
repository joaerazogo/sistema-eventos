package co.com.ticketing.processor.model.event.gateways;

import co.com.ticketing.processor.model.event.Event;
import reactor.core.publisher.Mono;

public interface EventRepository {

    Mono<Event> findById(String eventId);
    Mono<Event> updateAvailableTickets(String eventId, int delta);

}
