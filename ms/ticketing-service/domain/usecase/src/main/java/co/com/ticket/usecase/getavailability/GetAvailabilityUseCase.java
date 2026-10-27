package co.com.ticket.usecase.getavailability;

import co.com.ticket.model.event.Event;
import co.com.ticket.model.event.gateways.EventRepository;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
public class GetAvailabilityUseCase {

    private final EventRepository eventRepository;

    public Mono<Event> execute(String eventId) {
        return eventRepository.findById(eventId);
    }

    public Flux<Event> executeAll() {
        return eventRepository.findAll();
    }

}
