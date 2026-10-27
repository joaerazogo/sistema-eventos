package co.com.ticket.api.event.dto;

import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

@Data
@Builder
@Jacksonized
public class EventRS {

    private String eventId;
    private String name;
    private String venue;
    private String date;
    private int totalCapacity;
    private int availableTickets;

}
