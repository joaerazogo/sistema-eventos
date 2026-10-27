package co.com.ticket.api.order.handler;

import co.com.ticket.api.event.dto.ErrorDetail;
import co.com.ticket.api.order.dto.PurchaseRQ;
import co.com.ticket.api.order.mapper.OrderMapper;
import co.com.ticket.model.exception.BusinessException;
import co.com.ticket.model.exception.TechnicalException;
import co.com.ticket.model.exception.TicketingException;
import co.com.ticket.model.util.TicketingConstant;
import co.com.ticket.usecase.assigncomplimentary.AssignComplimentaryUseCase;
import co.com.ticket.usecase.purchaseticket.PurchaseTicketUseCase;
import co.com.ticket.usecase.order.OrderUseCase;
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
public class OrderHandler {

    private final PurchaseTicketUseCase purchaseTicketUseCase;
    private final OrderUseCase orderUseCase;
    private final AssignComplimentaryUseCase assignComplimentaryUseCase;

    public Mono<ServerResponse> purchase(ServerRequest request) {
        return request.bodyToMono(PurchaseRQ.class)
                .doOnNext(rq -> log.info(TicketingConstant.LOG_PURCHASE_REQUEST,
                        rq.getEventId(), rq.getUserId(), rq.getQuantity()))
                .flatMap(rq -> purchaseTicketUseCase.execute(rq.getEventId(), rq.getUserId(), rq.getQuantity()))
                .map(OrderMapper.MAPPER::toResponse)
                .doOnNext(rs -> log.info(TicketingConstant.LOG_PURCHASE_RESPONSE, rs.getOrderId()))
                .flatMap(rs -> ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue(rs))
                .onErrorResume(BusinessException.class, e -> {
                    log.error(TicketingConstant.LOG_PURCHASE_ERROR, e);
                    return ServerResponse.status(422).bodyValue(toError(e));
                })
                .onErrorResume(TechnicalException.class, e -> {
                    log.error(TicketingConstant.LOG_PURCHASE_ERROR, e);
                    return ServerResponse.status(500).bodyValue(toError(e));
                })
                .onErrorResume(Exception.class, e -> {
                    log.error(TicketingConstant.LOG_PURCHASE_ERROR, e);
                    return ServerResponse.status(500).bodyValue(ErrorDetail.builder()
                            .code(TicketingConstant.HTTP_500).message(e.getMessage()).build());
                });
    }

    public Mono<ServerResponse> assignComplimentary(ServerRequest request) {
        String orderId = request.pathVariable("orderId");
        log.info(TicketingConstant.LOG_ASSIGN_COMPLIMENTARY_REQUEST, orderId);
        return assignComplimentaryUseCase.execute(orderId)
                .map(OrderMapper.MAPPER::toResponse)
                .doOnNext(rs -> log.info(TicketingConstant.LOG_ASSIGN_COMPLIMENTARY_RESPONSE, rs.getOrderId()))
                .flatMap(rs -> ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue(rs))
                .onErrorResume(TicketingException.class, e -> {
                    log.error(TicketingConstant.LOG_ASSIGN_COMPLIMENTARY_ERROR, orderId, e);
                    return ServerResponse.status(422).bodyValue(toError(e));
                })
                .onErrorResume(Exception.class, e -> {
                    log.error(TicketingConstant.LOG_ASSIGN_COMPLIMENTARY_ERROR, orderId, e);
                    return ServerResponse.status(500).bodyValue(ErrorDetail.builder()
                            .code(TicketingConstant.HTTP_500).message(e.getMessage()).build());
                });
    }

    public Mono<ServerResponse> getOrder(ServerRequest request) {
        String orderId = request.pathVariable("orderId");
        log.info(TicketingConstant.LOG_GET_ORDER_REQUEST, orderId);
        return orderUseCase.execute(orderId)
                .map(OrderMapper.MAPPER::toResponse)
                .doOnNext(rs -> log.info(TicketingConstant.LOG_GET_ORDER_RESPONSE, rs.getOrderId(), rs.getStatus()))
                .flatMap(rs -> ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue(rs))
                .switchIfEmpty(ServerResponse.notFound().build())
                .onErrorResume(BusinessException.class, e -> {
                    log.error(TicketingConstant.LOG_GET_ORDER_ERROR, orderId, e);
                    return ServerResponse.status(422).bodyValue(toError(e));
                })
                .onErrorResume(TechnicalException.class, e -> {
                    log.error(TicketingConstant.LOG_GET_ORDER_ERROR, orderId, e);
                    return ServerResponse.status(500).bodyValue(toError(e));
                })
                .onErrorResume(Exception.class, e -> {
                    log.error(TicketingConstant.LOG_GET_ORDER_ERROR, orderId, e);
                    return ServerResponse.status(500).bodyValue(ErrorDetail.builder()
                            .code(TicketingConstant.HTTP_500).message(e.getMessage()).build());
                });
    }

    private ErrorDetail toError(TicketingException e) {
        return ErrorDetail.builder()
                .code(e.getErrorMessage().getCode())
                .message(e.getErrorMessage().getMessage())
                .build();
    }
}
