package co.com.ticketing.processor.dynamodb.event.mapper;

import co.com.ticketing.processor.dynamodb.event.model.EventEntity;
import co.com.ticketing.processor.model.event.Event;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

@Mapper(unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface EventMapper {

    EventMapper MAPPER = Mappers.getMapper(EventMapper.class);

    @Mapping(target = "date", expression = "java(entity.getDate() != null ? java.time.Instant.parse(entity.getDate()) : null)")
    Event toDomain(EventEntity entity);
}
