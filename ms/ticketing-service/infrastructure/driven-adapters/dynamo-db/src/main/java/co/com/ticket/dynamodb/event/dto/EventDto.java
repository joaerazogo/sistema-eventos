package co.com.ticket.dynamodb.event.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class EventDto {

    private String orderId;
    private String eventId;
    private String status;
    private String reservedAt;

}
