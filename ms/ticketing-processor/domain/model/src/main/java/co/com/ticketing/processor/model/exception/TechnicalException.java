package co.com.ticketing.processor.model.exception;

import co.com.ticketing.processor.model.enums.ErrorMessage;

public class TechnicalException extends TicketingException {

    public TechnicalException(ErrorMessage errorMessage) {
        super(errorMessage);
    }

    public TechnicalException(Throwable cause, ErrorMessage errorMessage) {
        super(cause, errorMessage);
    }
}
