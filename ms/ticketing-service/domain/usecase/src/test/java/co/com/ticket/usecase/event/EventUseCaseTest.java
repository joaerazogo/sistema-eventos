package co.com.ticket.usecase.event;

import co.com.ticket.model.event.Event;
import co.com.ticket.model.event.gateways.EventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventUseCaseTest {

    @Mock private EventRepository eventRepository;

    private EventUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new EventUseCase(eventRepository);
    }

    @Test
    void shouldSaveEvent() {
        Event event = Event.builder().eventId("event-1").name("Concert").totalCapacity(100).availableTickets(100).build();
        when(eventRepository.save(any(Event.class))).thenReturn(Mono.just(event));

        StepVerifier.create(useCase.save(event))
                .expectNext(event)
                .verifyComplete();
    }

    @Test
    void shouldPropagateErrorFromRepository() {
        Event event = Event.builder().name("Concert").totalCapacity(50).build();
        when(eventRepository.save(any())).thenReturn(Mono.error(new RuntimeException("DB error")));

        StepVerifier.create(useCase.save(event))
                .expectErrorMessage("DB error")
                .verify();
    }
}
