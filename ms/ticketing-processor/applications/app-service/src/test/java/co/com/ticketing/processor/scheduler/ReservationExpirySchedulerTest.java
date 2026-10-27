package co.com.ticketing.processor.scheduler;

import co.com.ticketing.processor.model.enums.TicketStatus;
import co.com.ticketing.processor.model.order.Order;
import co.com.ticketing.processor.usecase.releaseexpiredreservations.ReleaseExpiredReservationsUseCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;

import java.time.Instant;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservationExpirySchedulerTest {

    @Mock private ReleaseExpiredReservationsUseCase releaseExpiredReservationsUseCase;
    @InjectMocks private ReservationExpiryScheduler scheduler;

    @Test
    void shouldCallUseCaseOnSchedule() {
        Order released = Order.builder()
                .orderId("order-1").eventId("event-1").userId("user-1")
                .quantity(2).status(TicketStatus.AVAILABLE)
                .reservedAt(Instant.now()).expiresAt(Instant.now())
                .build();

        when(releaseExpiredReservationsUseCase.execute()).thenReturn(Flux.just(released));

        scheduler.releaseExpiredReservations();

        verify(releaseExpiredReservationsUseCase).execute();
    }

    @Test
    void shouldHandleErrorGracefully() {
        when(releaseExpiredReservationsUseCase.execute()).thenReturn(Flux.error(new RuntimeException("DB error")));

        scheduler.releaseExpiredReservations();

        verify(releaseExpiredReservationsUseCase).execute();
    }
}
