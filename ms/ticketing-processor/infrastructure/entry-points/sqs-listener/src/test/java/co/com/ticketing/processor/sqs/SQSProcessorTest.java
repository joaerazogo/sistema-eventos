package co.com.ticketing.processor.sqs;

import co.com.ticketing.processor.model.enums.TicketStatus;
import co.com.ticketing.processor.model.order.Order;
import co.com.ticketing.processor.usecase.processorder.ProcessOrderUseCase;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import software.amazon.awssdk.services.sqs.model.Message;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SQSProcessorTest {

    @Mock private ProcessOrderUseCase processOrderUseCase;

    private SQSProcessor sqsProcessor;

    @BeforeEach
    void setUp() {
        sqsProcessor = new SQSProcessor(processOrderUseCase, new ObjectMapper());
    }

    @Test
    void shouldExtractOrderIdAndProcessOrder() {
        String body = "{\"orderId\":\"order-123\"}";
        Message message = Message.builder().body(body).messageId("msg-1").build();
        Order order = Order.builder()
                .orderId("order-123").eventId("event-1").userId("user-1")
                .quantity(2).status(TicketStatus.SOLD)
                .reservedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(600))
                .build();

        when(processOrderUseCase.execute(eq("order-123"))).thenReturn(Mono.just(order));

        StepVerifier.create(sqsProcessor.apply(message))
                .verifyComplete();
    }

    @Test
    void shouldCompleteEmptyOnInvalidJson() {
        Message message = Message.builder().body("not-json").messageId("msg-2").build();

        StepVerifier.create(sqsProcessor.apply(message))
                .verifyComplete();
    }
}
