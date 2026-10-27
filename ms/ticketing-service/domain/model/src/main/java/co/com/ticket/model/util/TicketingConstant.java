package co.com.ticket.model.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class TicketingConstant {

    public static final String HTTP_200 = "200";
    public static final String HTTP_400 = "400";
    public static final String HTTP_404 = "404";
    public static final String HTTP_500 = "500";

    public static final String LOG_CREATE_EVENT_REQUEST   = "Request createEvent eventId={}";
    public static final String LOG_CREATE_EVENT_RESPONSE  = "Response createEvent eventId={}";
    public static final String LOG_CREATE_EVENT_ERROR     = "Error createEvent";
    public static final String LOG_GET_AVAILABILITY_REQUEST  = "Request getAvailability eventId={}";
    public static final String LOG_GET_AVAILABILITY_RESPONSE = "Response getAvailability eventId={}";
    public static final String LOG_GET_AVAILABILITY_ERROR    = "Error getAvailability eventId={}";
    public static final String LOG_PURCHASE_REQUEST   = "Request purchase eventId={} userId={} qty={}";
    public static final String LOG_PURCHASE_RESPONSE  = "Response purchase orderId={}";
    public static final String LOG_PURCHASE_ERROR     = "Error purchase";
    public static final String LOG_GET_ORDER_REQUEST  = "Request getOrder orderId={}";
    public static final String LOG_GET_ORDER_RESPONSE = "Response getOrder orderId={} status={}";
    public static final String LOG_GET_ORDER_ERROR    = "Error getOrder orderId={}";
    public static final String LOG_ASSIGN_COMPLIMENTARY_REQUEST  = "Request assignComplimentary orderId={}";
    public static final String LOG_ASSIGN_COMPLIMENTARY_RESPONSE = "Response assignComplimentary orderId={}";
    public static final String LOG_ASSIGN_COMPLIMENTARY_ERROR    = "Error assignComplimentary orderId={}";

    public static final String LOG_SAVE_EVENT_ERROR          = "Error saving event";
    public static final String LOG_FIND_EVENT_ERROR          = "Error finding event eventId={}";
    public static final String LOG_FIND_ALL_EVENTS_ERROR     = "Error listing events";
    public static final String LOG_UPDATE_TICKETS_ERROR      = "Error updating tickets eventId={}";
    public static final String LOG_SAVE_ORDER_ERROR          = "Error saving order";
    public static final String LOG_FIND_ORDER_ERROR          = "Error finding order orderId={}";
    public static final String LOG_SQS_PUBLISH_ERROR         = "Error publishing order orderId={}";
}
