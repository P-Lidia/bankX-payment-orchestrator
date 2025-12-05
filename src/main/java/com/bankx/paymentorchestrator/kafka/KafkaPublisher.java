package com.bankx.paymentorchestrator.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * Сервис для отправки событий в Kafka.
 * <p>
 * Принимает объект {@link KafkaEvent} и публикует его в указанный топик.
 * Сериализация объекта происходит автоматически с помощью настроенного JSON-сериализатора.
 */

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaPublisher {
    private final KafkaTemplate<String, KafkaEvent> kafkaTemplate;

    @Value("${kafka.topics.send-outbox-event}")
    private String sendEventTopic;

    /**
     * Отправляет событие в Kafka и возвращает CompletableFuture, чтобы шедулер мог обработать результат отправки.
     *
     * @param event событие для отправки
     * @return CompletableFuture, который завершится при успешной отправке или с ошибкой
     */
    public CompletableFuture<SendResult<String, KafkaEvent>> send(KafkaEvent event) {

        // Создаём пустой CompletableFuture, который мы будем завершать позже
        CompletableFuture<SendResult<String, KafkaEvent>> future = new CompletableFuture<>();

        try {
            kafkaTemplate.send(sendEventTopic, event.getCorrelationId().toString(), event)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("Failed to send event to Kafka, correlationId={}", event.getCorrelationId(), ex);

                            // Завершаем CompletableFuture с ошибкой.
                            future.completeExceptionally(ex);

                        } else {
                            log.debug("Event sent to Kafka, topic={}, correlationId={}",
                                    sendEventTopic,
                                    event.getCorrelationId());

                            // Завершаем CompletableFuture успешно
                            future.complete(result);
                        }
                    });
        } catch (Exception e) {
            log.error("Exception while sending event to Kafka, correlationId={}", event.getCorrelationId(), e);
            future.completeExceptionally(e);
        }
        return future;
    }
}

