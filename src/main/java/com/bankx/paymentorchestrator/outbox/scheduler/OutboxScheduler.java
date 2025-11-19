package com.bankx.paymentorchestrator.outbox.scheduler;

import com.bankx.paymentorchestrator.kafka.KafkaEvent;
import com.bankx.paymentorchestrator.kafka.KafkaEventDescription;
import com.bankx.paymentorchestrator.kafka.KafkaPublisher;
import com.bankx.paymentorchestrator.outbox.mapper.KafkaEventMapper;
import com.bankx.paymentorchestrator.outbox.model.OutboxEvent;
import com.bankx.paymentorchestrator.outbox.repository.OutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.support.SendResult;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxScheduler {
    private final OutboxRepository outboxRepository;
    private final KafkaPublisher kafkaPublisher;
    private final KafkaEventMapper kafkaEventMapper;

    private static final int MAX_RETRIES = 3;
    private static final int BATCH_SIZE = 100;

    @Scheduled(fixedDelayString = "${scheduler.outbox.interval}")
    public void publishOutboxEvents() {
        try {
            List<OutboxEvent> unpublishedEvents = outboxRepository.findUnpublishedNew(BATCH_SIZE);

            if (!unpublishedEvents.isEmpty()) {
                log.debug("Start publish processing for {} new events", unpublishedEvents.size());

                // Обрабатываем каждое событие
                for (OutboxEvent event : unpublishedEvents) {
                    processSingleEvent(event);
                }
            }

        } catch (Exception e) {
            // Если SELECT из БД или что-то глобальное упадет
            log.error("Failed to fetch new outbox events from DB", e);
        }
    }

    private void processSingleEvent(OutboxEvent event) {
        log.debug("Start publish processing for event id={}, status={}", event.getId(), event.getStatus());
        try {
            // Преобразование OutboxEvent в KafkaEvent для отправки
            KafkaEvent kafkaEvent = kafkaEventMapper.toKafkaEvent(event, KafkaEventDescription.INTERNAL_TRANSFER);

            log.debug("Sending Kafka event for outbox id={}, correlationId={}",
                    event.getId(),
                    event.getCorrelationId());

            // Отправляем событие в Kafka и получаем CompletableFuture для отслеживания результата
            CompletableFuture<SendResult<String, KafkaEvent>> future = kafkaPublisher.send(kafkaEvent);

            // Обрабатываем результат отправки (меняем статус)
            handleSendResult(future, event);

        } catch (Exception e) {
            log.error("Fail publish processing for event id={}, correlationId={}, status={}",
                    event.getId(),
                    event.getCorrelationId(),
                    event.getStatus(),
                    e);
            outboxRepository.markAsFailed(event.getId(), e.getMessage());
        }
    }

    private void handleSendResult(CompletableFuture<SendResult<String, KafkaEvent>> future, OutboxEvent event) {
        future.whenComplete((result, exception) -> {

            // если отправка завершилась ошибкой
            if (exception != null) {
                try {
                    // меняем статус в БД на Failed
                    outboxRepository.markAsFailed(event.getId(), exception.getMessage());
                    log.error("Failed to send outbox event id={}, correlationId={}, status={}",
                            event.getId(),
                            event.getCorrelationId(),
                            event.getStatus());
                } catch (Exception ex) {
                    // Если update в БД упадет
                    log.error("Failed to update status to FAILED for outbox event id={}, correlationId={}",
                            event.getId(),
                            event.getCorrelationId(),
                            ex);
                }
            } else {
                try {
                    // меняем статус в БД на Published
                    outboxRepository.markAsPublished(event.getId());
                    log.debug(
                            "Successfully send outbox event id={}, topic={}, partition={}, offset={}",
                            event.getId(),
                            result.getRecordMetadata().topic(),
                            result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset()
                    );
                } catch (Exception ex) {
                    log.error("Failed to update status to PUBLISHED for outbox event id={}, correlationId={}",
                            event.getId(),
                            event.getCorrelationId(),
                            ex);
                }
            }
        });
    }



    /*
     * Очистка старых обработанных событий (для предотвращения роста БД)
     */
    @Scheduled(cron = "")

    public void cleanupOldProcessedEvents() {
    }

}

/* todo:

Retriable publishing:
Нужен механизм повторной отправки (status = FAILED → NEW через n секунд).
Можно сделать отдельный scheduler для retry.

Трассировка / observability:
Добавь логи и метрики (published, failed, latency).

Оптимизация:
Чтобы не забирать слишком много записей, используй лимит (например, 100 за раз).

Transactional outbox:
Сохранение события в таблицу в рамках транзакции бизнес-операции (очень важно для гарантии доставки). */
