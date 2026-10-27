package co.com.ticket.api.event.dto;

import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

@Data
@Jacksonized
@Builder(toBuilder = true)
public class ErrorDetail {

    String code;
    String message;
}
