package co.com.ticket.api.order.dto;

import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

@Data
@Builder
@Jacksonized
public class PurchaseRQ {

    private String eventId;
    private String userId;
    private int quantity;

}
