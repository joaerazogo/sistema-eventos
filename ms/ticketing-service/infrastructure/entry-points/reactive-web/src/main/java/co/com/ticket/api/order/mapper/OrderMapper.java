package co.com.ticket.api.order.mapper;

import co.com.ticket.api.order.dto.OrderRS;
import co.com.ticket.model.order.Order;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface OrderMapper {

    OrderMapper MAPPER = Mappers.getMapper(OrderMapper.class);

    OrderRS toResponse(Order order);

}
