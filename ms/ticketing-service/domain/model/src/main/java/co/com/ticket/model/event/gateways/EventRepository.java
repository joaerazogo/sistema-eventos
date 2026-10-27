package co.com.ticket.model.event.gateways;

import co.com.ticket.model.event.Event;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface EventRepository {

    Mono<Event> save(Event event);
    Mono<Event> findById(String eventId);
    Flux<Event> findAll();
    Mono<Event> updateAvailableTickets(String eventId, int delta);
    Mono<Boolean> existsByVenueAndDate(String venue, String date);

}
