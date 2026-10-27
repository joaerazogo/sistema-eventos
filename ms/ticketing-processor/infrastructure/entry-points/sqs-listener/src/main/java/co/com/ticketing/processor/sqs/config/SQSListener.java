package co.com.ticketing.processor.sqs.config;

import co.com.ticketing.processor.sqs.SQSProcessor;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;

import java.util.concurrent.atomic.AtomicBoolean;

@Component
@Log4j2
@RequiredArgsConstructor
public class SQSListener {

    private final SqsAsyncClient sqsAsyncClient;
    private final SQSProperties properties;
    private final SQSProcessor sqsProcessor;
    private final AtomicBoolean running = new AtomicBoolean(false);

    @PostConstruct
    public void start() {
        running.set(true);
        Flux.<Integer>generate(sink -> sink.next(1))
                .flatMap(i -> poll(), properties.numberOfThreads())
                .takeWhile(msg -> running.get())
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe();
        log.info("SQS Listener started on queue: {}", properties.queueUrl());
    }

    @PreDestroy
    public void stop() {
        running.set(false);
        log.info("SQS Listener stopped");
    }

    private Flux<Message> poll() {
        return Mono.fromFuture(() -> sqsAsyncClient.receiveMessage(buildReceiveMessageRequest()))
                .map(ReceiveMessageResponse::messages)
                .flatMapMany(Flux::fromIterable)
                .flatMap(message -> processAndDelete(message)
                        .onErrorResume(e -> {
                            log.error("Failed to process message {}", message.messageId(), e);
                            return Mono.empty();
                        }));
    }

    private Mono<Message> processAndDelete(Message message) {
        return sqsProcessor.apply(message)
                .then(deleteMessage(message))
                .thenReturn(message);
    }

    private Mono<Void> deleteMessage(Message message) {
        return Mono.fromFuture(() -> sqsAsyncClient.deleteMessage(buildDeleteMessageRequest(message)))
                .then();
    }

    private ReceiveMessageRequest buildReceiveMessageRequest() {
        return ReceiveMessageRequest.builder()
                .queueUrl(properties.queueUrl())
                .waitTimeSeconds(properties.waitTimeSeconds())
                .maxNumberOfMessages(properties.maxNumberOfMessages())
                .visibilityTimeout(properties.visibilityTimeoutSeconds())
                .build();
    }

    private DeleteMessageRequest buildDeleteMessageRequest(Message message) {
        return DeleteMessageRequest.builder()
                .queueUrl(properties.queueUrl())
                .receiptHandle(message.receiptHandle())
                .build();
    }
}
