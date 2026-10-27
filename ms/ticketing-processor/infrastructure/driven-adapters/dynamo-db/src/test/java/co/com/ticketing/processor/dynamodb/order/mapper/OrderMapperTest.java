package co.com.ticketing.processor.dynamodb.order.mapper;

import co.com.ticketing.processor.dynamodb.order.model.OrderEntity;
import co.com.ticketing.processor.model.enums.TicketStatus;
import co.com.ticketing.processor.model.order.Order;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class OrderMapperTest {

    private final OrderMapper mapper = OrderMapper.MAPPER;

    @Test
    void shouldMapAllFieldsToDomain() {
        OrderEntity entity = new OrderEntity();
        entity.setOrderId("order-1");
        entity.setEventId("event-1");
        entity.setUserId("user-1");
        entity.setQuantity(2);
        entity.setStatus("RESERVED");
        entity.setReservedAt("2026-12-01T20:00:00Z");
        entity.setExpiresAt("2026-12-01T20:10:00Z");

        Order result = mapper.toDomain(entity);

        assertEquals("order-1", result.getOrderId());
        assertEquals("event-1", result.getEventId());
        assertEquals("user-1", result.getUserId());
        assertEquals(2, result.getQuantity());
        assertEquals(TicketStatus.RESERVED, result.getStatus());
        assertEquals(Instant.parse("2026-12-01T20:00:00Z"), result.getReservedAt());
        assertEquals(Instant.parse("2026-12-01T20:10:00Z"), result.getExpiresAt());
    }

    @Test
    void shouldMapNullDatesToNull() {
        OrderEntity entity = new OrderEntity();
        entity.setOrderId("order-1");
        entity.setStatus("RESERVED");
        entity.setReservedAt(null);
        entity.setExpiresAt(null);

        Order result = mapper.toDomain(entity);

        assertNull(result.getReservedAt());
        assertNull(result.getExpiresAt());
    }
}
