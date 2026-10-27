package co.com.ticket.dynamodb.order.mapper;

import co.com.ticket.dynamodb.order.model.OrderEntity;
import co.com.ticket.model.order.Order;
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
