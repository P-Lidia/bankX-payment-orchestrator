package com.bankx.paymentorchestrator.outbox.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Модель события для паттерна Outbox.
 * <p>
 * Событие фиксируется в базе данных и используется для надёжной публикации сообщений в Kafka
 * в рамках механизма "транзакция + outbox". Это гарантирует, что событие не будет потеряно
 * даже при сбое приложения или брокера сообщений.
 * </p>
 *
 * <p><b>Основная логика:</b></p>
 * <ul>
 *     <li>После успешного завершения бизнес-операции событие сохраняется в таблицу <code>outbox_events</code>.</li>
 *     <li>Планировщик (scheduler) периодически читает новые события и отправляет их в Kafka.</li>
 *     <li>После успешной отправки статус события обновляется на <code>PUBLISHED</code>.</li>
 * </ul>
 *
 * <p><b>Поля:</b></p>
 * <ul>
 *     <li>{@code id} — уникальный идентификатор события (генерируется при записи в таблицу).</li>
 *     <li>{@code correlationId} — идентификатор корреляции для трассировки распределённых транзакций.</li>
 *     <li>{@code eventType} — тип события (например, {@code TRANSFER_CREATED}); помогает различать виды сообщений.</li>
 *     <li>{@code payload} — сериализованные бизнес-данные (JSON), которые будут отправлены в Kafka.</li>
 *     <li>{@code status} — состояние события (например, {@code NEW}, {@code PUBLISHED}, {@code FAILED}).</li>
 *     <li>{@code createAt} — время создания события.</li>
 *     <li>{@code updateAt} — время последнего обновления записи (например, после повторной попытки отправки).</li>
 *     <li>{@code retryCount} — количество попыток повторной отправки при ошибках Kafka-публикации.</li>
 *     <li>{@code errorMessage} — текст последней ошибки при попытке отправки (используется для отладки).</li>
 * </ul>
 */

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class OutboxEvent {
    private UUID id;
    private UUID correlationId;
    private String eventType;  // поле для расширяемость, пока что всегда будет "TRANSFER_CREATED"
    private String payload;  // бизнес-данные, которые потом пойдут в kafkaEvent.
    private OutboxStatus status;
    private Instant createdAt;
    private Instant updatedAt;
    private int retryCount;
    private String errorMessage;  // Для ошибок при отправке (для дебага)
}
