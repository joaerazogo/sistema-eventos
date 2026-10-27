package co.com.ticketing.processor.dynamodb.order.mapper;

import co.com.ticketing.processor.dynamodb.order.model.OrderEntity;
import co.com.ticketing.processor.model.order.Order;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

@Mapper(unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface OrderMapper {

    OrderMapper MAPPER = Mappers.getMapper(OrderMapper.class);

    @Mapping(target = "reservedAt", expression = "java(entity.getReservedAt() != null ? java.time.Instant.parse(entity.getReservedAt()) : null)")
    @Mapping(target = "expiresAt", expression = "java(entity.getExpiresAt() != null ? java.time.Instant.parse(entity.getExpiresAt()) : null)")
    Order toDomain(OrderEntity entity);
}
