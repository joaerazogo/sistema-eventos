package co.com.ticket.usecase.createevent;

import co.com.ticket.model.enums.ErrorMessage;
import co.com.ticket.model.enums.TicketStatus;
import co.com.ticket.model.event.Event;
import co.com.ticket.model.event.gateways.EventRepository;
import co.com.ticket.model.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

@RequiredArgsConstructor
public class CreateEventUseCase {

    private final EventRepository eventRepository;

    public Mono<Event> execute(Event event) {
        if (event.getDate() == null || !event.getDate().isAfter(Instant.now())) {
            return Mono.error(new BusinessException(ErrorMessage.EVENT_DATE_IN_PAST));
        }
        return eventRepository.existsByVenueAndDate(event.getVenue(), event.getDate().toString())
                .flatMap(exists -> {
                    if (exists) {
                        return Mono.error(new BusinessException(ErrorMessage.DUPLICATE_EVENT));
                    }
                    Event newEvent = event.toBuilder()
                            .eventId(UUID.randomUUID().toString())
                            .availableTickets(event.getTotalCapacity())
                            .build();
                    return eventRepository.save(newEvent);
                });
    }

}
