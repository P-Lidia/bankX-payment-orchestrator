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

    // todo в сервисе платежа надо будет добавить String payload = objectMapper.writeValueAsString(request); - сериализация в JSON
    /* payload - это те данные, которые потом пойдут в kafkaEvent. */

}