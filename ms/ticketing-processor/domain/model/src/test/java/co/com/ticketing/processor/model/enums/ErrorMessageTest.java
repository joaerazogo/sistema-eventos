package co.com.ticketing.processor.model.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ErrorMessageTest {

    @Test
    void shouldHaveAllCodesAndMessages() {
        assertEquals("PROC-500", ErrorMessage.INTERNAL_ERROR.getCode());
        assertEquals("Internal server error", ErrorMessage.INTERNAL_ERROR.getMessage());

        assertEquals("PROC-001", ErrorMessage.ORDER_NOT_FOUND.getCode());
        assertEquals("PROC-002", ErrorMessage.EVENT_NOT_FOUND.getCode());
        assertEquals("PROC-003", ErrorMessage.ORDER_SAVE_ERROR.getCode());
        assertEquals("PROC-004", ErrorMessage.TICKET_UPDATE_ERROR.getCode());
        assertEquals("PROC-005", ErrorMessage.EXPIRED_RESERVATIONS_ERROR.getCode());
        assertEquals("PROC-006", ErrorMessage.INVALID_ORDER_STATUS.getCode());
        assertEquals("PROC-007", ErrorMessage.SQS_PARSE_ERROR.getCode());
    }
}
