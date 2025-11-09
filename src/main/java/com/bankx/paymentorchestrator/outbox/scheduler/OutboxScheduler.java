package com.bankx.paymentorchestrator.outbox.scheduler;

import com.bankx.paymentorchestrator.kafka.KafkaPublisher;
import com.bankx.paymentorchestrator.outbox.repository.OutboxRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OutboxScheduler {
    private final OutboxRepository outboxRepository;
    private final KafkaPublisher kafkaPublisher;

    @Scheduled(fixedDelayString = "${scheduler.outbox.interval:5000}")
    public void publishOutboxEvents() {
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