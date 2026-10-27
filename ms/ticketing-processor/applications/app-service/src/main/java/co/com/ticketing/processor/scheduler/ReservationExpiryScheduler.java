package co.com.ticketing.processor.scheduler;

import co.com.ticketing.processor.model.util.TicketingConstant;
import co.com.ticketing.processor.usecase.releaseexpiredreservations.ReleaseExpiredReservationsUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class ReservationExpiryScheduler {

    private final ReleaseExpiredReservationsUseCase releaseExpiredReservationsUseCase;

    @Scheduled(fixedDelayString = "${scheduler.release-expired.delay-ms:60000}")
    public void releaseExpiredReservations() {
        log.info(TicketingConstant.LOG_EXPIRY_START);
        releaseExpiredReservationsUseCase.execute()
                .doOnNext(order -> log.info(TicketingConstant.LOG_EXPIRY_RELEASED, order.getOrderId()))
                .doOnError(e -> log.error(TicketingConstant.LOG_EXPIRY_ERROR, e))
                .onErrorResume(e -> reactor.core.publisher.Flux.empty())
                .subscribe();
    }
}
