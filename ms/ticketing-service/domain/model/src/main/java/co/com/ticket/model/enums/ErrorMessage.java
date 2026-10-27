package co.com.ticket.model.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorMessage {

    INTERNAL_ERROR("TICKET-500", "Internal server error"),
    EVENT_NOT_FOUND("TICKET-001", "Event not found"),
    ORDER_NOT_FOUND("TICKET-002", "Order not found"),
    NO_AVAILABLE_TICKETS("TICKET-003", "No available tickets for the requested quantity"),
    INVALID_QUANTITY("TICKET-004", "Quantity must be greater than zero"),
    EVENT_SAVE_ERROR("TICKET-005", "Error saving event"),
    ORDER_SAVE_ERROR("TICKET-006", "Error saving order"),
    TICKET_UPDATE_ERROR("TICKET-007", "Error updating available tickets"),
    SQS_PUBLISH_ERROR("TICKET-008", "Error publishing order to queue"),
    DUPLICATE_EVENT("TICKET-010", "An event already exists at this venue and date"),
    EVENT_DATE_IN_PAST("TICKET-011", "Event date must be in the future"),
    ORDER_NOT_ASSIGNABLE("TICKET-009", "Order cannot be assigned as complimentary in its current status");

    private final String code;
    private final String message;
}
