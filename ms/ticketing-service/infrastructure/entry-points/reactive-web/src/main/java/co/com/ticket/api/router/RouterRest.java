package co.com.ticket.api.router;

import static org.springdoc.core.fn.builders.apiresponse.Builder.responseBuilder;
import static org.springdoc.core.fn.builders.parameter.Builder.parameterBuilder;
import static org.springdoc.core.fn.builders.requestbody.Builder.requestBodyBuilder;
import static org.springdoc.webflux.core.fn.SpringdocRouteBuilder.route;

import co.com.ticket.api.event.dto.EventRQ;
import co.com.ticket.api.event.dto.EventRS;
import co.com.ticket.api.event.handler.EventHandler;
import co.com.ticket.api.order.dto.OrderRS;
import co.com.ticket.api.order.dto.PurchaseRQ;
import co.com.ticket.api.order.handler.OrderHandler;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.info.Info;
import org.springdoc.core.fn.builders.operation.Builder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

import java.util.function.Consumer;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Ticketing Service API",
                version = "1.0",
                description = "API REST para gestión de venta de tiquetes para eventos"))
public class RouterRest {

    @Bean
    public RouterFunction<ServerResponse> routerFunction(EventHandler eventHandler, OrderHandler orderHandler) {
        return route()
                .POST("/api/v1/events", eventHandler::createEvent, createEvent())
                .GET("/api/v1/events", eventHandler::listEvents, listEvents())
                .GET("/api/v1/events/{eventId}/availability", eventHandler::getAvailability, getAvailability())
                .POST("/api/v1/purchases", orderHandler::purchase, purchase())
                .POST("/api/v1/orders/{orderId}/complimentary", orderHandler::assignComplimentary, assignComplimentary())
                .GET("/api/v1/orders/{orderId}", orderHandler::getOrder, getOrder())
                .build();
    }

    private Consumer<Builder> createEvent() {
        return ops -> ops
                .operationId("createEvent")
                .summary("Crear un evento")
                .tags(new String[]{"Events"})
                .requestBody(requestBodyBuilder().implementation(EventRQ.class))
                .response(responseBuilder().responseCode("200").implementation(EventRS.class))
                .response(responseBuilder().responseCode("500").description("Error interno del servidor"));
    }

    private Consumer<Builder> listEvents() {
        return ops -> ops
                .operationId("listEvents")
                .summary("Listar todos los eventos")
                .tags(new String[]{"Events"})
                .response(responseBuilder().responseCode("200").implementation(EventRS.class))
                .response(responseBuilder().responseCode("500").description("Error interno del servidor"));
    }

    private Consumer<Builder> getAvailability() {
        return ops -> ops
                .operationId("getAvailability")
                .summary("Consultar disponibilidad del evento")
                .tags(new String[]{"Events"})
                .parameter(parameterBuilder().in(ParameterIn.PATH).name("eventId").required(true))
                .response(responseBuilder().responseCode("200").implementation(EventRS.class))
                .response(responseBuilder().responseCode("404").description("Evento no encontrado"))
                .response(responseBuilder().responseCode("500").description("Error interno del servidor"));
    }

    private Consumer<Builder> purchase() {
        return ops -> ops
                .operationId("purchaseTickets")
                .summary("Comprar tiquetes")
                .tags(new String[]{"Orders"})
                .requestBody(requestBodyBuilder().implementation(PurchaseRQ.class))
                .response(responseBuilder().responseCode("200").implementation(OrderRS.class))
                .response(responseBuilder().responseCode("500").description("Error interno del servidor"));
    }

    private Consumer<Builder> assignComplimentary() {
        return ops -> ops
                .operationId("assignComplimentary")
                .summary("Asignar orden como cortesía (RESERVED → COMPLIMENTARY)")
                .tags(new String[]{"Orders"})
                .parameter(parameterBuilder().in(ParameterIn.PATH).name("orderId").required(true))
                .response(responseBuilder().responseCode("200").implementation(OrderRS.class))
                .response(responseBuilder().responseCode("422").description("La orden no está en estado RESERVED"))
                .response(responseBuilder().responseCode("500").description("Error interno del servidor"));
    }

    private Consumer<Builder> getOrder() {
        return ops -> ops
                .operationId("getOrder")
                .summary("Consultar estado de una orden")
                .tags(new String[]{"Orders"})
                .parameter(parameterBuilder().in(ParameterIn.PATH).name("orderId").required(true))
                .response(responseBuilder().responseCode("200").implementation(OrderRS.class))
                .response(responseBuilder().responseCode("404").description("Orden no encontrada"))
                .response(responseBuilder().responseCode("500").description("Error interno del servidor"));
    }

}
