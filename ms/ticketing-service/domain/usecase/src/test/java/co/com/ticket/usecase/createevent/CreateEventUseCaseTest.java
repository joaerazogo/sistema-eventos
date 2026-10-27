package co.com.ticket.usecase.createevent;

import co.com.ticket.model.enums.ErrorMessage;
import co.com.ticket.model.exception.BusinessException;
import co.com.ticket.model.event.Event;
import co.com.ticket.model.event.gateways.EventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class CreateEventUseCaseTest {

    @Mock
    private EventRepository eventRepository;

    private CreateEventUseCase useCase;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        useCase = new CreateEventUseCase(eventRepository);
    }

    @Test
    void shouldCreateEventWithGeneratedIdAndAvailableTickets() {
        Event input = Event.builder()
                .name("Concert")
                .venue("Stadium")
                .date(Instant.now().plus(1, ChronoUnit.DAYS))
                .totalCapacity(100)
                .build();

        when(eventRepository.existsByVenueAndDate(anyString(), anyString())).thenReturn(Mono.just(false));
        when(eventRepository.save(any(Event.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(useCase.execute(input))
                .assertNext(event -> {
                    assertThat(event.getEventId()).isNotNull();
                    assertThat(event.getAvailableTickets()).isEqualTo(100);
                    assertThat(event.getName()).isEqualTo("Concert");
                })
                .verifyComplete();
    }

    @Test
    void shouldPropagateErrorFromRepository() {
        Event input = Event.builder()
                .name("Concert")
                .venue("Stadium")
                .date(Instant.now().plus(1, ChronoUnit.DAYS))
                .totalCapacity(50)
                .build();

        when(eventRepository.existsByVenueAndDate(anyString(), anyString())).thenReturn(Mono.just(false));
        when(eventRepository.save(any(Event.class))).thenReturn(Mono.error(new RuntimeException("DB error")));

        StepVerifier.create(useCase.execute(input))
                .expectErrorMessage("DB error")
                .verify();
    }

    @Test
    void shouldRejectEventWithPastDate() {
        Event input = Event.builder()
                .name("Concert")
                .venue("Stadium")
                .date(Instant.now().minus(1, ChronoUnit.DAYS))
                .totalCapacity(100)
                .build();

        StepVerifier.create(useCase.execute(input))
                .expectErrorMatches(e -> e instanceof BusinessException &&
                        ((BusinessException) e).getErrorMessage() == ErrorMessage.EVENT_DATE_IN_PAST)
                .verify();
    }

    @Test
    void shouldRejectDuplicateEvent() {
        Event input = Event.builder()
                .name("Concert")
                .venue("Stadium")
                .date(Instant.now().plus(1, ChronoUnit.DAYS))
                .totalCapacity(100)
                .build();

        when(eventRepository.existsByVenueAndDate(anyString(), anyString())).thenReturn(Mono.just(true));

        StepVerifier.create(useCase.execute(input))
                .expectErrorMatches(e -> e instanceof BusinessException &&
                        ((BusinessException) e).getErrorMessage() == ErrorMessage.DUPLICATE_EVENT)
                .verify();
    }

}
