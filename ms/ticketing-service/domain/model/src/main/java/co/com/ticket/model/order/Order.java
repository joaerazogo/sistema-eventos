package co.com.ticket.model.order;

import co.com.ticket.model.enums.TicketStatus;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;

@Value
@Builder(toBuilder = true)
public class Order {

    String orderId;
    String eventId;
    String userId;
    int quantity;
    TicketStatus status;
    Instant reservedAt;
    Instant expiresAt;

}
