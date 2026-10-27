package co.com.ticket.model.event;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;

@Value
@Builder(toBuilder = true)
public class Event {

    String eventId;
    String name;
    String venue;
    Instant date;
    int totalCapacity;
    int availableTickets;

}
