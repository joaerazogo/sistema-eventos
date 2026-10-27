package co.com.ticketing.processor.model.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class TicketingConstant {

    public static final String LOG_FIND_ORDER_ERROR            = "Error finding order orderId={}";
    public static final String LOG_SAVE_ORDER_ERROR            = "Error saving order orderId={}";
    public static final String LOG_FIND_EXPIRED_ERROR          = "Error finding expired reservations";
    public static final String LOG_FIND_EVENT_ERROR            = "Error finding event eventId={}";
    public static final String LOG_UPDATE_TICKETS_ERROR        = "Error updating tickets eventId={}";

    public static final String LOG_SQS_MESSAGE_RECEIVED        = "SQS message received messageId={}";
    public static final String LOG_SQS_ORDER_PROCESSED         = "Order {} processed with status {}";
    public static final String LOG_SQS_MESSAGE_ERROR           = "Error processing message messageId={}";
    public static final String LOG_SQS_PARSE_ERROR             = "Error parsing SQS message body";

    public static final String LOG_EXPIRY_START                = "Running expiry scheduler";
    public static final String LOG_EXPIRY_RELEASED             = "Released expired reservation orderId={}";
    public static final String LOG_EXPIRY_ERROR                = "Error releasing expired reservations";
}
