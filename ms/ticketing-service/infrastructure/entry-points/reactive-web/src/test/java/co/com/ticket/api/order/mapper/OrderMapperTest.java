package co.com.ticket.api.order.mapper;

import co.com.ticket.api.order.dto.OrderRS;
import co.com.ticket.model.enums.TicketStatus;
import co.com.ticket.model.order.Order;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class OrderMapperTest {

    private final OrderMapper mapper = OrderMapper.MAPPER;

    @Test
    void shouldMapOrderToResponse() {
        Instant now = Instant.parse("2024-01-01T00:00:00Z");
        Order order = Order.builder()
                .orderId("order-1")
                .eventId("event-1")
                .userId("user-1")
                .quantity(2)
                .status(TicketStatus.RESERVED)
                .reservedAt(now)
                .expiresAt(now.plusSeconds(600))
                .build();

        OrderRS rs = mapper.toResponse(order);

        assertNotNull(rs);
        assertEquals("order-1", rs.getOrderId());
        assertEquals("event-1", rs.getEventId());
        assertEquals("user-1", rs.getUserId());
        assertEquals(2, rs.getQuantity());
        assertEquals("RESERVED", rs.getStatus());
    }

    @Test
    void shouldHandleNullFieldsInOrder() {
        OrderRS rs = mapper.toResponse(Order.builder().build());
        assertNotNull(rs);
    }
}
