package com.bankx.paymentorchestrator.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

/**
 * Сервис для отправки событий в Kafka.
 * <p>
 * Принимает объект {@link KafkaEvent} и публикует его в указанный топик.
 * Сериализация объекта происходит автоматически с помощью настроенного JSON-сериализатора.
 * Если отправка завершается ошибкой — исключение пробрасывается дальше,
 * чтобы обработчик Outbox (шедулер) мог изменить статус события и выполнить повторную попытку.
 */

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaPublisher {
    private final KafkaTemplate<String, KafkaEvent> kafkaTemplate;

    @Value("${kafka.topics.sendOutboxEvent}")
    private String sendEventTopic;

    public void send(KafkaEvent event) {
        try {
            kafkaTemplate.send(sendEventTopic, event.getCorrelationId().toString(), event);
            log.debug("Event sent to Kafka, topic={}, correlationId={}", sendEventTopic, event.getCorrelationId());
        } catch (Exception e) {
            log.error("Failed to send event to Kafka, correlationId={}", event.getCorrelationId(), e);
            throw e;
        }
    }
}