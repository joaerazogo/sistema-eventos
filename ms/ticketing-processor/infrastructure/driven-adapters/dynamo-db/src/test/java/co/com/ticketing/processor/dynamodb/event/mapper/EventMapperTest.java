package co.com.ticketing.processor.dynamodb.event.mapper;


import co.com.ticketing.processor.dynamodb.event.model.EventEntity;
import co.com.ticketing.processor.model.event.Event;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class EventMapperTest {

    private final EventMapper mapper = EventMapper.MAPPER;

    @Test
    void shouldMapAllFieldsToDomain() {
        EventEntity entity = new EventEntity();
        entity.setEventId("event-1");
        entity.setName("Rock Concert");
        entity.setVenue("National Stadium");
        entity.setDate("2026-12-01T20:00:00Z");
        entity.setTotalCapacity(100);
        entity.setAvailableTickets(80);

        Event result = mapper.toDomain(entity);

        assertEquals("event-1", result.getEventId());
        assertEquals("Rock Concert", result.getName());
        assertEquals("National Stadium", result.getVenue());
        assertEquals(Instant.parse("2026-12-01T20:00:00Z"), result.getDate());
        assertEquals(100, result.getTotalCapacity());
        assertEquals(80, result.getAvailableTickets());
    }

    @Test
    void shouldMapNullDateToNull() {
        EventEntity entity = new EventEntity();
        entity.setEventId("event-1");
        entity.setDate(null);

        Event result = mapper.toDomain(entity);

        assertNull(result.getDate());
    }
}
