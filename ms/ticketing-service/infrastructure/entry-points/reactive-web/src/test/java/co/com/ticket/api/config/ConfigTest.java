package co.com.ticket.api.config;

import co.com.ticket.api.event.handler.EventHandler;
import co.com.ticket.api.order.handler.OrderHandler;
import co.com.ticket.api.router.RouterRest;
import co.com.ticket.usecase.assigncomplimentary.AssignComplimentaryUseCase;
import co.com.ticket.usecase.createevent.CreateEventUseCase;
import co.com.ticket.usecase.getavailability.GetAvailabilityUseCase;
import co.com.ticket.usecase.purchaseticket.PurchaseTicketUseCase;
import co.com.ticket.usecase.order.OrderUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ContextConfiguration(classes = {RouterRest.class, EventHandler.class, OrderHandler.class})
@WebFluxTest
@Import({CorsConfig.class, SecurityHeadersConfig.class})
class ConfigTest {

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
    void shouldReturnSecurityHeaders() {
        when(getAvailabilityUseCase.execute(anyString())).thenReturn(Mono.empty());

        webTestClient.get()
                .uri("/api/v1/events/any/availability")
                .exchange()
                .expectHeader().valueEquals("Content-Security-Policy",
                        "default-src 'self'; frame-ancestors 'self'; form-action 'self'")
                .expectHeader().valueEquals("Strict-Transport-Security", "max-age=31536000; includeSubDomains; preload")
                .expectHeader().valueEquals("X-Content-Type-Options", "nosniff")
                .expectHeader().doesNotExist("Server")
                .expectHeader().valueEquals("Cache-Control", "no-store")
                .expectHeader().valueEquals("Company", "no-cache")
                .expectHeader().valueEquals("Referrer-Policy", "strict-origin-when-cross-origin");
    }

}
