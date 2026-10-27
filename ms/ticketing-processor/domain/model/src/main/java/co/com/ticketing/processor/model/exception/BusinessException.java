package co.com.ticketing.processor.model.exception;

import co.com.ticketing.processor.model.enums.ErrorMessage;

public class BusinessException extends TicketingException {

    public BusinessException(ErrorMessage errorMessage) {
        super(errorMessage);
    }
}
