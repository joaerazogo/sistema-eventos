package co.com.ticket.api;

import co.com.ticket.api.event.handler.EventHandler;
import co.com.ticket.api.order.handler.OrderHandler;
import co.com.ticket.api.router.RouterRest;
import co.com.ticket.usecase.assigncomplimentary.AssignComplimentaryUseCase;
import co.com.ticket.usecase.createevent.CreateEventUseCase;
import co.com.ticket.usecase.getavailability.GetAvailabilityUseCase;
import co.com.ticket.usecase.purchaseticket.PurchaseTicketUseCase;
import co.com.ticket.usecase.order.OrderUseCase;
import co.com.ticket.model.enums.ErrorMessage;
import co.com.ticket.model.enums.TicketStatus;
import co.com.ticket.model.exception.BusinessException;
import co.com.ticket.model.exception.TechnicalException;
import co.com.ticket.model.event.Event;
import co.com.ticket.model.order.Order;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ContextConfiguration(classes = {RouterRest.class, EventHandler.class, OrderHandler.class})
@WebFluxTest
class RouterRestTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private CreateEventUseCase createEventUseCase;

    @MockitoBean
    private GetAvailabilityUseCase getAvailabilityUseCase;

    @MockitoBean
    private PurchaseTicketUseCase purchaseTicketUseCase;

    @MockitoBean
    private OrderUseCase orderUseCase;

    @MockitoBean
    private AssignComplimentaryUseCase assignComplimentaryUseCase;

    @Test
    void shouldCreateEvent() {
        Event event = Event.builder()
                .eventId("event-1")
                .name("Concert")
                .venue("Stadium")
                .totalCapacity(100)
                .availableTickets(100)
                .build();

        when(createEventUseCase.execute(any(Event.class))).thenReturn(Mono.just(event));

        webTestClient.post()
                .uri("/api/v1/events")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"name\":\"Concert\",\"venue\":\"Stadium\",\"totalCapacity\":100}")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.eventId").isEqualTo("event-1")
                .jsonPath("$.name").isEqualTo("Concert");
    }

    @Test
    void shouldListEvents() {
        Event event = Event.builder().eventId("event-1").name("Concert").availableTickets(50).build();

        when(getAvailabilityUseCase.executeAll()).thenReturn(Flux.just(event));

        webTestClient.get()
                .uri("/api/v1/events")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void shouldGetEventAvailability() {
        Event event = Event.builder().eventId("event-1").name("Concert").availableTickets(50).build();

        when(getAvailabilityUseCase.execute("event-1")).thenReturn(Mono.just(event));

        webTestClient.get()
                .uri("/api/v1/events/event-1/availability")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.eventId").isEqualTo("event-1");
    }

    @Test
    void shouldReturn404WhenEventNotFound() {
        when(getAvailabilityUseCase.execute("nonexistent")).thenReturn(Mono.empty());

        webTestClient.get()
                .uri("/api/v1/events/nonexistent/availability")
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void shouldPurchaseTicket() {
        Order order = Order.builder()
                .orderId("order-1")
                .eventId("event-1")
                .userId("user-1")
                .quantity(2)
                .status(TicketStatus.RESERVED)
                .reservedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(600))
                .build();

        when(purchaseTicketUseCase.execute(anyString(), anyString(), anyInt())).thenReturn(Mono.just(order));

        webTestClient.post()
                .uri("/api/v1/purchases")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"eventId\":\"event-1\",\"userId\":\"user-1\",\"quantity\":2}")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.orderId").isEqualTo("order-1")
                .jsonPath("$.status").isEqualTo("RESERVED");
    }

    @Test
    void shouldGetOrderById() {
        Order order = Order.builder()
                .orderId("order-1")
                .status(TicketStatus.SOLD)
                .build();

        when(orderUseCase.execute("order-1")).thenReturn(Mono.just(order));

        webTestClient.get()
                .uri("/api/v1/orders/order-1")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.orderId").isEqualTo("order-1")
                .jsonPath("$.status").isEqualTo("SOLD");
    }

    @Test
    void shouldReturn404WhenOrderNotFound() {
        when(orderUseCase.execute("nonexistent")).thenReturn(Mono.empty());

        webTestClient.get()
                .uri("/api/v1/orders/nonexistent")
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void shouldReturn500WhenCreateEventFails() {
        when(createEventUseCase.execute(any(Event.class))).thenReturn(Mono.error(new RuntimeException("DB error")));

        webTestClient.post()
                .uri("/api/v1/events")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"name\":\"Concert\",\"totalCapacity\":100}")
                .exchange()
                .expectStatus().is5xxServerError();
    }

    @Test
    void shouldReturn500WhenGetAvailabilityFails() {
        when(getAvailabilityUseCase.execute("event-1")).thenReturn(Mono.error(new RuntimeException("DB error")));

        webTestClient.get()
                .uri("/api/v1/events/event-1/availability")
                .exchange()
                .expectStatus().is5xxServerError();
    }

    @Test
    void shouldReturn500WhenPurchaseFails() {
        when(purchaseTicketUseCase.execute(anyString(), anyString(), anyInt()))
                .thenReturn(Mono.error(new RuntimeException("SQS error")));

        webTestClient.post()
                .uri("/api/v1/purchases")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"eventId\":\"event-1\",\"userId\":\"user-1\",\"quantity\":2}")
                .exchange()
                .expectStatus().is5xxServerError();
    }

    @Test
    void shouldReturn500WhenGetOrderFails() {
        when(orderUseCase.execute("order-1")).thenReturn(Mono.error(new RuntimeException("DB error")));

        webTestClient.get()
                .uri("/api/v1/orders/order-1")
                .exchange()
                .expectStatus().is5xxServerError();
    }

    @Test
    void shouldReturn500WithTicketingExceptionCodeWhenCreateEventFails() {
        when(createEventUseCase.execute(any(Event.class)))
                .thenReturn(Mono.error(new TechnicalException(ErrorMessage.EVENT_SAVE_ERROR)));

        webTestClient.post()
                .uri("/api/v1/events")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"name\":\"Concert\",\"totalCapacity\":100}")
                .exchange()
                .expectStatus().is5xxServerError()
                .expectBody()
                .jsonPath("$.code").isEqualTo("TICKET-005");
    }

    @Test
    void shouldReturn500WithTicketingExceptionCodeWhenPurchaseFails() {
        when(purchaseTicketUseCase.execute(anyString(), anyString(), anyInt()))
                .thenReturn(Mono.error(new BusinessException(ErrorMessage.INVALID_QUANTITY)));

        webTestClient.post()
                .uri("/api/v1/purchases")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"eventId\":\"event-1\",\"userId\":\"user-1\",\"quantity\":2}")
                .exchange()
                .expectStatus().is4xxClientError()
                .expectBody()
                .jsonPath("$.code").isEqualTo("TICKET-004");
    }

    @Test
    void shouldReturn500WithTicketingExceptionCodeWhenGetAvailabilityFails() {
        when(getAvailabilityUseCase.execute("event-1"))
                .thenReturn(Mono.error(new TechnicalException(ErrorMessage.EVENT_NOT_FOUND)));

        webTestClient.get()
                .uri("/api/v1/events/event-1/availability")
                .exchange()
                .expectStatus().is5xxServerError()
                .expectBody()
                .jsonPath("$.code").isEqualTo("TICKET-001");
    }

    @Test
    void shouldAssignComplimentary() {
        Order order = Order.builder()
                .orderId("order-1")
                .status(TicketStatus.COMPLIMENTARY)
                .build();

        when(assignComplimentaryUseCase.execute("order-1")).thenReturn(Mono.just(order));

        webTestClient.post()
                .uri("/api/v1/orders/order-1/complimentary")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.orderId").isEqualTo("order-1")
                .jsonPath("$.status").isEqualTo("COMPLIMENTARY");
    }

    @Test
    void shouldReturn422WhenOrderNotAssignable() {
        when(assignComplimentaryUseCase.execute("order-1"))
                .thenReturn(Mono.error(new BusinessException(ErrorMessage.ORDER_NOT_ASSIGNABLE)));

        webTestClient.post()
                .uri("/api/v1/orders/order-1/complimentary")
                .exchange()
                .expectStatus().isEqualTo(422)
                .expectBody()
                .jsonPath("$.code").isEqualTo("TICKET-009");
    }

    @Test
    void shouldReturn500WithTicketingExceptionCodeWhenGetOrderFails() {
        when(orderUseCase.execute("order-1"))
                .thenReturn(Mono.error(new TechnicalException(ErrorMessage.ORDER_NOT_FOUND)));

        webTestClient.get()
                .uri("/api/v1/orders/order-1")
                .exchange()
                .expectStatus().is5xxServerError()
                .expectBody()
                .jsonPath("$.code").isEqualTo("TICKET-002");
    }

}
