package com.bankx.paymentorchestrator.outbox.mapper;

import com.bankx.paymentorchestrator.kafka.KafkaEvent;
import com.bankx.paymentorchestrator.kafka.KafkaEventDescription;
import com.bankx.paymentorchestrator.outbox.model.OutboxEvent;
import com.bankx.paymentorchestrator.outbox.model.TransferPayload;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Класс OutboxMapper отвечает за преобразование событий из таблицы Outbox
 * (OutboxEvent) в объекты KafkaEvent для публикации в Kafka.
 *
 * <p>Основная задача:
 * - десериализовать JSON-поле payload из OutboxEvent в объект TransferPayload;
 * - собрать новый объект KafkaEvent с необходимыми полями.</p>
 *
 * <p>Используется Jackson ObjectMapper для десериализации JSON.</p>
 */

@Component
@RequiredArgsConstructor
public class KafkaEventMapper {

    private final ObjectMapper objectMapper;

    public KafkaEvent toKafkaEvent(OutboxEvent outboxEvent, KafkaEventDescription description) {

        if (outboxEvent.getPayload() == null || outboxEvent.getPayload().isBlank()) {
            throw new IllegalStateException("Payload is null or empty");
            // todo логирование?
        }
        try {
            // десериализует JSON-поле payload из OutboxEvent в объект TransferPayload
            TransferPayload payload = objectMapper.readValue(outboxEvent.getPayload(), TransferPayload.class);

            return KafkaEvent.builder()
                    .correlationId(outboxEvent.getCorrelationId())
                    .eventType(outboxEvent.getEventType())
                    .payload(payload)
                    .createdAt(outboxEvent.getCreatedAt())
                    .description(description)
                    .build();
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to map OutboxEvent to KafkaEvent", e);
        }
    }
}