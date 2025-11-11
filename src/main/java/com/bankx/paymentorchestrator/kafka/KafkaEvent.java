package com.bankx.paymentorchestrator.kafka;

import com.bankx.paymentorchestrator.outbox.model.TransferPayload;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO для публикации события в Kafka из Outbox.
 * <p>
 * Содержит основную метаинформацию о переводе и вложенный объект {@link TransferPayload},
 * описывающий детали перевода (счета, сумма, валюта и т. д.).
 * <p>
 * При сериализации в JSON структура выглядит как единый объект с вложенным JSON:
 * <pre>{@code
 * {
 *   "correlationId": "e4a0915d-1c27-404e-904e",
 *   "eventType": "TRANSFER_CREATED",
 *   "payload": {
 *     "fromAccountId": "A1",
 *     "toAccountId": "B1",
 *     "amount": 100.0,
 *     "currency": "USD"
 *   },
 *   "createdAt": "2025-11-10T14:23:00Z"
 * }
 * }</pre>
 */

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class KafkaEvent {
    private UUID correlationId;
    private String eventType;
    private TransferPayload payload;
    private Instant createdAt;
    private KafkaEventDescription description;
}