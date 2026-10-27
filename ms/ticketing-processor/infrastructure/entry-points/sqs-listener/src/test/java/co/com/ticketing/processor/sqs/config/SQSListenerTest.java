package co.com.ticketing.processor.sqs.config;

import co.com.ticketing.processor.sqs.SQSProcessor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.DeleteMessageResponse;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SQSListenerTest {

    @Mock private SqsAsyncClient sqsAsyncClient;
    @Mock private SQSProcessor sqsProcessor;

    private SQSListener listener;

    @BeforeEach
    void setUp() {
        SQSProperties props = new SQSProperties("us-east-1", "http://localhost:4566",
                "http://localhost:4566/000000000000/q", 1, 10, 30, 1);
        listener = new SQSListener(sqsAsyncClient, props, sqsProcessor);
    }

    @Test
    void buildReceiveMessageRequest() {
        ReceiveMessageRequest req = ReflectionTestUtils.invokeMethod(listener, "buildReceiveMessageRequest");
        assertNotNull(req);
        assertEquals("http://localhost:4566/000000000000/q", req.queueUrl());
        assertEquals(1, req.waitTimeSeconds());
        assertEquals(10, req.maxNumberOfMessages());
        assertEquals(30, req.visibilityTimeout());
    }

    @Test
    void buildDeleteMessageRequest() {
        Message message = Message.builder().receiptHandle("rh-123").build();
        DeleteMessageRequest req = ReflectionTestUtils.invokeMethod(listener, "buildDeleteMessageRequest", message);
        assertNotNull(req);
        assertEquals("rh-123", req.receiptHandle());
        assertEquals("http://localhost:4566/000000000000/q", req.queueUrl());
    }

    @Test
    void deleteMessageCompletesSuccessfully() {
        Message message = Message.builder().receiptHandle("rh-1").build();
        when(sqsAsyncClient.deleteMessage(any(DeleteMessageRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(DeleteMessageResponse.builder().build()));

        StepVerifier.create(ReflectionTestUtils.<Mono<Void>>invokeMethod(listener, "deleteMessage", message))
                .verifyComplete();
    }

    @Test
    void processAndDeleteCompletesSuccessfully() {
        Message message = Message.builder().messageId("m1").receiptHandle("rh-1").build();
        when(sqsProcessor.apply(message)).thenReturn(Mono.empty());
        when(sqsAsyncClient.deleteMessage(any(DeleteMessageRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(DeleteMessageResponse.builder().build()));

        StepVerifier.create(ReflectionTestUtils.<Mono<Message>>invokeMethod(listener, "processAndDelete", message))
                .expectNextMatches(m -> m.messageId().equals("m1"))
                .verifyComplete();
    }

    @Test
    void pollReturnsMessagesAndProcessesThem() {
        Message message = Message.builder().messageId("m2").receiptHandle("rh-2").build();
        when(sqsAsyncClient.receiveMessage(any(ReceiveMessageRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(
                        ReceiveMessageResponse.builder().messages(List.of(message)).build()));
        when(sqsProcessor.apply(any())).thenReturn(Mono.empty());
        when(sqsAsyncClient.deleteMessage(any(DeleteMessageRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(DeleteMessageResponse.builder().build()));

        StepVerifier.create(ReflectionTestUtils.<reactor.core.publisher.Flux<Message>>invokeMethod(listener, "poll"))
                .expectNextCount(1)
                .verifyComplete();
    }

    @Test
    void pollResumesOnProcessorError() {
        Message message = Message.builder().messageId("m3").receiptHandle("rh-3").build();
        when(sqsAsyncClient.receiveMessage(any(ReceiveMessageRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(
                        ReceiveMessageResponse.builder().messages(List.of(message)).build()));
        when(sqsProcessor.apply(any())).thenReturn(Mono.error(new RuntimeException("fail")));

        StepVerifier.create(ReflectionTestUtils.<reactor.core.publisher.Flux<Message>>invokeMethod(listener, "poll"))
                .verifyComplete();
    }
}
