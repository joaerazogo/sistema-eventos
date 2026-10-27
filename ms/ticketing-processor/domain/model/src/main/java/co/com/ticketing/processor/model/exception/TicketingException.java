package co.com.ticketing.processor.model.exception;

import co.com.ticketing.processor.model.enums.ErrorMessage;
import lombok.Getter;

@Getter
public class TicketingException extends RuntimeException {

    private final ErrorMessage errorMessage;

    public TicketingException(ErrorMessage errorMessage) {
        super(errorMessage.getMessage());
        this.errorMessage = errorMessage;
    }

    public TicketingException(Throwable cause, ErrorMessage errorMessage) {
        super(errorMessage.getMessage(), cause);
        this.errorMessage = errorMessage;
    }
}
