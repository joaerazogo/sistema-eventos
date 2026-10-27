package co.com.ticketing.processor.dynamodb.entity;

import co.com.ticketing.processor.dynamodb.order.model.OrderEntity;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OrderEntityTest {

    @Test
    void shouldGetAndSetAllFields() {
        OrderEntity entity = new OrderEntity();
        entity.setOrderId("order-1");
        entity.setEventId("event-1");
        entity.setUserId("user-1");
        entity.setQuantity(2);
        entity.setStatus("RESERVED");
        entity.setReservedAt("2024-01-01T00:00:00Z");
        entity.setExpiresAt("2024-01-01T00:10:00Z");

        assertEquals("order-1", entity.getOrderId());
        assertEquals("event-1", entity.getEventId());
        assertEquals("user-1", entity.getUserId());
        assertEquals(2, entity.getQuantity());
        assertEquals("RESERVED", entity.getStatus());
        assertEquals("2024-01-01T00:00:00Z", entity.getReservedAt());
        assertEquals("2024-01-01T00:10:00Z", entity.getExpiresAt());
    }

    @Test
    void shouldReturnNullForUnsetFields() {
        OrderEntity entity = new OrderEntity();
        assertNull(entity.getOrderId());
        assertNull(entity.getStatus());
    }
}
