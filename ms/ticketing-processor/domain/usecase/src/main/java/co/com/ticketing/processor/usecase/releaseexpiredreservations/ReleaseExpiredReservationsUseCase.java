package co.com.ticketing.processor.usecase.releaseexpiredreservations;

import co.com.ticketing.processor.model.enums.TicketStatus;
import co.com.ticketing.processor.model.event.gateways.EventRepository;
import co.com.ticketing.processor.model.order.Order;
import co.com.ticketing.processor.model.order.gateways.OrderRepository;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;

@RequiredArgsConstructor
public class ReleaseExpiredReservationsUseCase {

    private final OrderRepository orderRepository;
    private final EventRepository eventRepository;

    public Flux<Order> execute() {
        return orderRepository.findExpiredReservations(TicketStatus.RESERVED.name(), Instant.now())
                .flatMap(this::releaseReservation);
    }

    private Mono<Order> releaseReservation(Order order) {
        Order released = order.toBuilder().status(TicketStatus.AVAILABLE).build();
        return orderRepository.saveIfReserved(released)
                .flatMap(saved -> eventRepository.updateAvailableTickets(saved.getEventId(), saved.getQuantity())
                        .thenReturn(saved));
    }

}
