package co.com.ticket.api.event.handler;

import co.com.ticket.api.event.dto.ErrorDetail;
import co.com.ticket.api.event.dto.EventRQ;
import co.com.ticket.api.event.mapper.EventMapper;
import co.com.ticket.model.exception.BusinessException;
import co.com.ticket.model.exception.TechnicalException;
import co.com.ticket.model.exception.TicketingException;
import co.com.ticket.model.util.TicketingConstant;
import co.com.ticket.usecase.createevent.CreateEventUseCase;
import co.com.ticket.usecase.getavailability.GetAvailabilityUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
@Slf4j
public class EventHandler {

    private final CreateEventUseCase createEventUseCase;
    private final GetAvailabilityUseCase getAvailabilityUseCase;

    public Mono<ServerResponse> createEvent(ServerRequest request) {
        return request.bodyToMono(EventRQ.class)
                .doOnNext(rq -> log.info(TicketingConstant.LOG_CREATE_EVENT_REQUEST, rq.getName()))
                .flatMap(rq -> createEventUseCase.execute(EventMapper.MAPPER.toDomain(rq)))
                .map(EventMapper.MAPPER::toResponse)
                .doOnNext(rs -> log.info(TicketingConstant.LOG_CREATE_EVENT_RESPONSE, rs.getEventId()))
                .flatMap(rs -> ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue(rs))
                .onErrorResume(BusinessException.class, e -> {
                    log.error(TicketingConstant.LOG_CREATE_EVENT_ERROR, e);
                    return ServerResponse.status(422).bodyValue(toError(e));
                })
                .onErrorResume(TechnicalException.class, e -> {
                    log.error(TicketingConstant.LOG_CREATE_EVENT_ERROR, e);
                    return ServerResponse.status(500).bodyValue(toError(e));
                })
                .onErrorResume(Exception.class, e -> {
                    log.error(TicketingConstant.LOG_CREATE_EVENT_ERROR, e);
                    return ServerResponse.status(500).bodyValue(ErrorDetail.builder()
                            .code(TicketingConstant.HTTP_500).message(e.getMessage()).build());
                });
    }

    public Mono<ServerResponse> getAvailability(ServerRequest request) {
        String eventId = request.pathVariable("eventId");
        log.info(TicketingConstant.LOG_GET_AVAILABILITY_REQUEST, eventId);
        return getAvailabilityUseCase.execute(eventId)
                .map(EventMapper.MAPPER::toResponse)
                .doOnNext(rs -> log.info(TicketingConstant.LOG_GET_AVAILABILITY_RESPONSE, rs.getEventId()))
                .flatMap(rs -> ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue(rs))
                .switchIfEmpty(ServerResponse.notFound().build())
                .onErrorResume(BusinessException.class, e -> {
                    log.error(TicketingConstant.LOG_GET_AVAILABILITY_ERROR, eventId, e);
                    return ServerResponse.status(422).bodyValue(toError(e));
                })
                .onErrorResume(TechnicalException.class, e -> {
                    log.error(TicketingConstant.LOG_GET_AVAILABILITY_ERROR, eventId, e);
                    return ServerResponse.status(500).bodyValue(toError(e));
                })
                .onErrorResume(Exception.class, e -> {
                    log.error(TicketingConstant.LOG_GET_AVAILABILITY_ERROR, eventId, e);
                    return ServerResponse.status(500).bodyValue(ErrorDetail.builder()
                            .code(TicketingConstant.HTTP_500).message(e.getMessage()).build());
                });
    }

    public Mono<ServerResponse> listEvents(ServerRequest request) {
        return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON)
                .body(getAvailabilityUseCase.executeAll().map(EventMapper.MAPPER::toResponse),
                        co.com.ticket.api.event.dto.EventRS.class);
    }

    private ErrorDetail toError(TicketingException e) {
        return ErrorDetail.builder()
                .code(e.getErrorMessage().getCode())
                .message(e.getErrorMessage().getMessage())
                .build();
    }
}
