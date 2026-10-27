package co.com.ticket.usecase.assigncomplimentary;

import co.com.ticket.model.enums.ErrorMessage;
import co.com.ticket.model.enums.TicketStatus;
import co.com.ticket.model.exception.BusinessException;
import co.com.ticket.model.order.Order;
import co.com.ticket.model.order.gateways.OrderRepository;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
public class AssignComplimentaryUseCase {

    private final OrderRepository orderRepository;

    public Mono<Order> execute(String orderId) {
        return orderRepository.findById(orderId)
                .switchIfEmpty(Mono.error(new BusinessException(ErrorMessage.ORDER_NOT_FOUND)))
                .flatMap(order -> {
                    if (order.getStatus() != TicketStatus.RESERVED) {
                        return Mono.error(new BusinessException(ErrorMessage.ORDER_NOT_ASSIGNABLE));
                    }
                    return orderRepository.save(order.toBuilder().status(TicketStatus.COMPLIMENTARY).build());
                });
    }

}
