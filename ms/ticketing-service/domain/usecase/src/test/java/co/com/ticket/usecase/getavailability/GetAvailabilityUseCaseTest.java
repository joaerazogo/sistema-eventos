package co.com.ticket.usecase.getavailability;

import co.com.ticket.model.event.Event;
import co.com.ticket.model.event.gateways.EventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.Mockito.when;

class GetAvailabilityUseCaseTest {

    @Mock
    private EventRepository eventRepository;

    private GetAvailabilityUseCase useCase;

    private Event event;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        useCase = new GetAvailabilityUseCase(eventRepository);

        event = Event.builder()
                .eventId("event-1")
                .name("Concert")
                .availableTickets(50)
                .totalCapacity(100)
                .build();
    }

    @Test
    void shouldReturnEventById() {
        when(eventRepository.findById("event-1")).thenReturn(Mono.just(event));

        StepVerifier.create(useCase.execute("event-1"))
                .expectNext(event)
                .verifyComplete();
    }

    @Test
    void shouldReturnEmptyWhenEventNotFound() {
        when(eventRepository.findById("nonexistent")).thenReturn(Mono.empty());

        StepVerifier.create(useCase.execute("nonexistent"))
                .verifyComplete();
    }

    @Test
    void shouldReturnAllEvents() {
        Event event2 = Event.builder().eventId("event-2").name("Theater").availableTickets(20).build();

        when(eventRepository.findAll()).thenReturn(Flux.just(event, event2));

        StepVerifier.create(useCase.executeAll())
                .expectNext(event, event2)
                .verifyComplete();
    }

}
