package co.com.ticket.api.event.mapper;

import co.com.ticket.api.event.dto.EventRQ;
import co.com.ticket.api.event.dto.EventRS;
import co.com.ticket.model.event.Event;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class EventMapperTest {

    private final EventMapper mapper = EventMapper.MAPPER;

    @Test
    void shouldMapRqToDomain() {
        EventRQ rq = EventRQ.builder()
                .name("Concert")
                .venue("Stadium")
                .totalCapacity(500)
                .build();

        Event event = mapper.toDomain(rq);

        assertNotNull(event);
        assertEquals("Concert", event.getName());
        assertEquals("Stadium", event.getVenue());
        assertEquals(500, event.getTotalCapacity());
    }

    @Test
    void shouldMapDomainToResponse() {
        Event event = Event.builder()
                .eventId("event-1")
                .name("Concert")
                .venue("Stadium")
                .totalCapacity(500)
                .availableTickets(200)
                .build();

        EventRS rs = mapper.toResponse(event);

        assertNotNull(rs);
        assertEquals("event-1", rs.getEventId());
        assertEquals("Concert", rs.getName());
        assertEquals("Stadium", rs.getVenue());
        assertEquals(500, rs.getTotalCapacity());
        assertEquals(200, rs.getAvailableTickets());
    }

    @Test
    void shouldHandleNullFieldsInRq() {
        Event event = mapper.toDomain(EventRQ.builder().build());
        assertNotNull(event);
        assertNull(event.getName());
    }

    @Test
    void shouldHandleNullFieldsInDomain() {
        EventRS rs = mapper.toResponse(Event.builder().build());
        assertNotNull(rs);
        assertNull(rs.getEventId());
    }
}
