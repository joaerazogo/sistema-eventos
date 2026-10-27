package co.com.ticket.model.exception;

import co.com.ticket.model.enums.ErrorMessage;

public class BusinessException extends TicketingException {

    public BusinessException(ErrorMessage errorMessage) {
        super(errorMessage);
    }
}
