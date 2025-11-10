package com.bankx.paymentorchestrator.outbox.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class OutboxEvent {
    private UUID id;
    private String requestId;
    private String eventType;  // для расширяемость, пока что всегда будет "TRANSFER_CREATED"
    private String payload;
    private OutboxStatus status;
    private Instant createAt;
    private Instant updateAt;
    private int retryCount;
    private String errorMessage;  // Для ошибок при отправке (для дебага)

    /* todo в сервисе/маппинге надо будет добавить
        String payload = objectMapper.writeValueAsString(request);  - сериализация в JSON
        outboxRepository.save(new OutboxEvent(..., payload, ...));
            или добавить:
        TransferPayload payload = objectMapper.readValue(json, TransferPayload.class);  - десериализация
          для перевода String payload в TransferPayload;
     */
    // payload - это те бизнес-данные, которые потом пойдут в kafkaEvent.
    // todo возможно надо будет вынести сериализацию/десериализацию в отдельный класс в util.

}