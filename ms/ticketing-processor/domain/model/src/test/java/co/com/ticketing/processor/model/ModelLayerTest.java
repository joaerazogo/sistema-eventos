package co.com.ticketing.processor.model;

import co.com.ticketing.processor.model.enums.ErrorMessage;
import co.com.ticketing.processor.model.exception.BusinessException;
import co.com.ticketing.processor.model.exception.TechnicalException;
import co.com.ticketing.processor.model.exception.TicketingException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ModelLayerTest {

    @Test
    void shouldCreateTicketingExceptionWithMessage() {
        TicketingException ex = new TechnicalException(ErrorMessage.INTERNAL_ERROR);
        assertEquals(ErrorMessage.INTERNAL_ERROR, ex.getErrorMessage());
        assertEquals("Internal server error", ex.getMessage());
    }

    @Test
    void shouldCreateTicketingExceptionWithCause() {
        RuntimeException cause = new RuntimeException("root");
        TicketingException ex = new TechnicalException(cause, ErrorMessage.ORDER_NOT_FOUND);
        assertEquals(cause, ex.getCause());
        assertEquals(ErrorMessage.ORDER_NOT_FOUND, ex.getErrorMessage());
    }

    @Test
    void shouldCreateBusinessException() {
        BusinessException ex = new BusinessException(ErrorMessage.INVALID_ORDER_STATUS);
        assertEquals(ErrorMessage.INVALID_ORDER_STATUS, ex.getErrorMessage());
        assertInstanceOf(TicketingException.class, ex);
    }

    @Test
    void shouldCreateTechnicalException() {
        TechnicalException ex = new TechnicalException(ErrorMessage.ORDER_SAVE_ERROR);
        assertEquals(ErrorMessage.ORDER_SAVE_ERROR, ex.getErrorMessage());
        assertInstanceOf(TicketingException.class, ex);
    }

    @Test
    void shouldHaveAllErrorMessageCodes() {
        assertEquals("PROC-500", ErrorMessage.INTERNAL_ERROR.getCode());
        assertEquals("PROC-001", ErrorMessage.ORDER_NOT_FOUND.getCode());
        assertEquals("PROC-002", ErrorMessage.EVENT_NOT_FOUND.getCode());
        assertEquals("PROC-003", ErrorMessage.ORDER_SAVE_ERROR.getCode());
        assertEquals("PROC-004", ErrorMessage.TICKET_UPDATE_ERROR.getCode());
        assertEquals("PROC-005", ErrorMessage.EXPIRED_RESERVATIONS_ERROR.getCode());
        assertEquals("PROC-006", ErrorMessage.INVALID_ORDER_STATUS.getCode());
        assertEquals("PROC-007", ErrorMessage.SQS_PARSE_ERROR.getCode());
    }
}
