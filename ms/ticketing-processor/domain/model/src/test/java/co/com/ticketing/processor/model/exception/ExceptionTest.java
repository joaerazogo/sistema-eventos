package co.com.ticketing.processor.model.exception;

import co.com.ticketing.processor.model.enums.ErrorMessage;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ExceptionTest {

    @Test
    void shouldCreateTechnicalExceptionWithErrorMessage() {
        TechnicalException ex = new TechnicalException(ErrorMessage.INTERNAL_ERROR);
        assertEquals(ErrorMessage.INTERNAL_ERROR, ex.getErrorMessage());
        assertEquals("Internal server error", ex.getMessage());
        assertInstanceOf(TicketingException.class, ex);
    }

    @Test
    void shouldCreateTechnicalExceptionWithCause() {
        RuntimeException cause = new RuntimeException("root");
        TechnicalException ex = new TechnicalException(cause, ErrorMessage.ORDER_NOT_FOUND);
        assertEquals(cause, ex.getCause());
        assertEquals(ErrorMessage.ORDER_NOT_FOUND, ex.getErrorMessage());
    }

    @Test
    void shouldCreateBusinessException() {
        BusinessException ex = new BusinessException(ErrorMessage.INVALID_ORDER_STATUS);
        assertEquals(ErrorMessage.INVALID_ORDER_STATUS, ex.getErrorMessage());
        assertEquals("Order has an unexpected status", ex.getMessage());
        assertInstanceOf(TicketingException.class, ex);
    }
}
