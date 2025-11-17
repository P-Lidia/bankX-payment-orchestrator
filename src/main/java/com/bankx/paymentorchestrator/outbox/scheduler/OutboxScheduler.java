package com.bankx.paymentorchestrator.outbox.scheduler;

import com.bankx.paymentorchestrator.kafka.KafkaPublisher;
import com.bankx.paymentorchestrator.outbox.model.OutboxEvent;
import com.bankx.paymentorchestrator.outbox.repository.OutboxRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class OutboxScheduler {
    private final OutboxRepository outboxRepository;
    private final KafkaPublisher kafkaPublisher;

    private static final int MAX_RETRIES = 3;
    private static final int BATCH_SIZE = 100;

    @Scheduled(fixedDelayString = "${scheduler.outbox.interval}")
    public void publishOutboxEvents() {
    }



    public List<OutboxEvent> findUnpublished() {
        return null;
    }

    public void markAsPublished(Long id) {
    }


    /*
     * Очистка старых обработанных событий (для предотвращения роста БД)
     */
    @Scheduled(cron = "")

    public void cleanupOldProcessedEvents() {}

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

// todo если ретрай > maxRetry -> какие дальше действия? ответ клиенту что операция не прошла? Логирование ошибки?