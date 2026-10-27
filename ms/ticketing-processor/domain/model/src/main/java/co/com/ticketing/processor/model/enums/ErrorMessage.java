package co.com.ticketing.processor.model.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorMessage {

    INTERNAL_ERROR("PROC-500", "Internal server error"),
    ORDER_NOT_FOUND("PROC-001", "Order not found"),
    EVENT_NOT_FOUND("PROC-002", "Event not found"),
    ORDER_SAVE_ERROR("PROC-003", "Error saving order"),
    TICKET_UPDATE_ERROR("PROC-004", "Error updating available tickets"),
    EXPIRED_RESERVATIONS_ERROR("PROC-005", "Error querying expired reservations"),
    INVALID_ORDER_STATUS("PROC-006", "Order has an unexpected status"),
    SQS_PARSE_ERROR("PROC-007", "Error parsing SQS message");

    private final String code;
    private final String message;
}
