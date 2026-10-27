package co.com.ticket.api.event.mapper;

import co.com.ticket.api.event.dto.EventRQ;
import co.com.ticket.api.event.dto.EventRS;
import co.com.ticket.model.event.Event;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface EventMapper {

    EventMapper MAPPER = Mappers.getMapper(EventMapper.class);

    Event toDomain(EventRQ rq);
    EventRS toResponse(Event event);

}
