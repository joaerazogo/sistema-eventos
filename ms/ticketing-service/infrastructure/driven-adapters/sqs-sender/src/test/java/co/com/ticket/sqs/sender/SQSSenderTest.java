package co.com.ticket.sqs.sender;

import co.com.ticket.model.enums.TicketStatus;
import co.com.ticket.model.order.Order;
import co.com.ticket.sqs.sender.config.SQSSenderProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.test.StepVerifier;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SQSSenderTest {

    @Mock private SqsAsyncClient sqsAsyncClient;
    @Mock private ObjectMapper objectMapper;

    private SQSSender sqsSender;
    private OrderPublisherImpl orderPublisher;

    @BeforeEach
    void setUp() {
        SQSSenderProperties properties = new SQSSenderProperties("us-east-1", "http://localhost:4566/000000000000/test-queue", "http://localhost:4566");
        sqsSender = new SQSSender(properties, sqsAsyncClient);
        orderPublisher = new OrderPublisherImpl(sqsSender, objectMapper);
    }

    @Test
    void shouldSendMessageAndReturnMessageId() {
        when(sqsAsyncClient.sendMessage(any(SendMessageRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(
                        SendMessageResponse.builder().messageId("msg-123").build()));

        StepVerifier.create(sqsSender.send("test-body"))
                .expectNext("msg-123")
                .verifyComplete();
    }

    @Test
    void shouldPublishOrderAndReturnMessageId() throws Exception {
        Order order = Order.builder()
                .orderId("order-1").eventId("event-1").userId("user-1")
                .quantity(2).status(TicketStatus.RESERVED)
                .reservedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(600))
                .build();

        when(objectMapper.writeValueAsString(order)).thenReturn("{\"orderId\":\"order-1\"}");
        when(sqsAsyncClient.sendMessage(any(SendMessageRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(
                        SendMessageResponse.builder().messageId("msg-456").build()));

        StepVerifier.create(orderPublisher.publish(order))
                .expectNext("msg-456")
                .verifyComplete();
    }

    @Test
    void shouldPropagateErrorWhenSqsFails() {
        when(sqsAsyncClient.sendMessage(any(SendMessageRequest.class)))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("SQS unavailable")));

        StepVerifier.create(sqsSender.send("test-body"))
                .expectErrorMessage("SQS unavailable")
                .verify();
    }
}
