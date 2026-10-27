package co.com.ticket.usecase.event;

import co.com.ticket.model.event.Event;
import co.com.ticket.model.event.gateways.EventRepository;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
public class EventUseCase {

    private final EventRepository eventRepository;

    public Mono<Event> save(Event event) {
        return eventRepository.save(event);
    }


}
