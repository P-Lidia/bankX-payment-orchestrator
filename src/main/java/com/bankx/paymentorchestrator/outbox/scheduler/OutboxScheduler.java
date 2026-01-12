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

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * Планировщик задач для обработки событий Outbox.
 *
 * <p>Основные задачи класса:
 * <ul>
 *     <li>Публикация новых событий Outbox в Kafka.</li>
 *     <li>Повторная отправка событий со статусом FAILED с ограничением по количеству попыток.</li>
 *     <li>Очистка старых обработанных событий для предотвращения роста базы данных.</li>
 * </ul>
 *
 * <p>Особенности реализации:
 * <ul>
 *     <li>Обработка событий пакетами (batch) для новых и неудачных событий.</li>
 *     <li>Асинхронная отправка событий в Kafka с использованием CompletableFuture.</li>
 *     <li>Автоматическое обновление статуса события в базе: {@code PUBLISHED} или {@code FAILED}.</li>
 *     <li>Очистка старых событий с учетом периода хранения (RETENTION_DAYS).</li>
 * </ul>
 *
 * <p>Зависимости:
 * <ul>
 *     <li>{@link OutboxRepository} — работа с таблицей outbox_events в БД.</li>
 *     <li>{@link KafkaPublisher} — отправка событий в Kafka.</li>
 *     <li>{@link KafkaEventMapper} — преобразование OutboxEvent в KafkaEvent.</li>
 * </ul>
 *
 * <p>Планируемые задачи:
 * <ul>
 *     <li>{@link #publishNewOutboxEvents()} — публикация новых событий.</li>
 *     <li>{@link #publishFailedOutboxEvents()} — повторная отправка неудачных событий.</li>
 *     <li>{@link #cleanupOldPublishedEvents()} — удаление старых событий раз в неделю.</li>
 * </ul>
 *
 * <p>Логирование:
 * <ul>
 *     <li>DEBUG — начало и успешная обработка событий.</li>
 *     <li>ERROR — ошибки при выборке событий, отправке в Kafka или обновлении статусов в БД.</li>
 * </ul>
 */

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxScheduler {
    private final OutboxRepository outboxRepository;
    private final KafkaPublisher kafkaPublisher;
    private final KafkaEventMapper kafkaEventMapper;

    private static final int MAX_RETRIES = 3;
    private static final int BATCH_SIZE = 100;
    private static final Duration RETENTION_DAYS = Duration.ofDays(30); // период хранения событий в БД

    /**
     * Публикация новых событий Outbox в Kafka.
     * <p>Выбирает события со статусом NEW пакетами по BATCH_SIZE,
     * и отправляет каждое событие через {@link #processSingleEvent(OutboxEvent)}.
     * Запускается с интервалом, заданным в application scheduler.outbox.interval.
     */
    @Scheduled(fixedDelayString = "${scheduler.outbox.interval}")
    public void publishNewOutboxEvents() {
        processPublishEvents(
                () -> outboxRepository.findByStatusNew(BATCH_SIZE),
                "Start publish processing for {} NEW events"
        );
    }  // в логе после for {} указывается количество обрабатываемых событий


    /**
     * Повторная отправка событий со статусом FAILED.
     * <p>Выбирает события с количеством повторных попыток меньше MAX_RETRIES и отправляет каждое
     * через {@link #processSingleEvent(OutboxEvent)}.
     * Запускается с интервалом scheduler.outbox.interval с задержкой initialDelay для предотвращения
     * совпадения с публикацией событий со статусом NEW.
     */
    // initialDelay - сдвигает запуск задачи относительно первичного времени старта(fixedDelayString)
    @Scheduled(fixedDelayString = "${scheduler.outbox.interval}", initialDelay = 2000)
    public void publishFailedOutboxEvents() {
        processPublishEvents(
                () -> outboxRepository.findByStatusFailed(MAX_RETRIES, BATCH_SIZE),
                "Start retry publish processing for {} FAILED events"
        );
    }

    /**
     * Универсальный метод обработки событий Outbox.
     * <p>Принимает {@link Supplier} для получения списка событий и лог-сообщение для дебага.
     * Обрабатывает каждое событие через {@link #processSingleEvent(OutboxEvent)}.
     *
     * @param fetchEvents Supplier, возвращающий список событий для обработки.
     * @param logMessage  Шаблон лог-сообщения, используется в log.debug.
     */
    private void processPublishEvents(Supplier<List<OutboxEvent>> fetchEvents, String logMessage) {
        try {
            // делаем выборку неопубликованных событий из БД
            List<OutboxEvent> unpublishedEvents = fetchEvents.get();

            if (!unpublishedEvents.isEmpty()) {
                log.debug(logMessage, unpublishedEvents.size());

                // Обрабатываем каждое событие
                for (OutboxEvent event : unpublishedEvents) {
                    processSingleEvent(event);
                }
            }
        } catch (Exception e) {
            log.error("Failed to fetch outbox events from DB", e);
        }
    }

    /**
     * Обработка одного события Outbox.
     * <p>Преобразует OutboxEvent в KafkaEvent через {@link KafkaEventMapper},
     * отправляет событие в Kafka через {@link KafkaPublisher#send(KafkaEvent)},
     * и обрабатывает результат асинхронно через {@link #handleSendResult(CompletableFuture, OutboxEvent)}.
     *
     * @param event событие Outbox для отправки
     */
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
            log.error("Failed to process publish for event  id={}, correlationId={}, status={}",
                    event.getId(),
                    event.getCorrelationId(),
                    event.getStatus(),
                    e);
            outboxRepository.markAsFailed(event.getId(), e.getMessage());
        }
    }

    /**
     * Обработка результата отправки события в Kafka.
     * <p>Использует {@link CompletableFuture#whenComplete} для отслеживаниясуспешной отправки или ошибки.
     * В зависимости от результата обновляет статус события в БД.
     *
     * @param future CompletableFuture с результатом отправки события
     * @param event исходное событие Outbox
     */
    private void handleSendResult(CompletableFuture<SendResult<String, KafkaEvent>> future, OutboxEvent event) {
        future.whenComplete((result, exception) -> {

            // если отправка завершилась ошибкой
            if (exception != null) {
                try {
                    // меняем статус в БД на Failed
                    outboxRepository.markAsFailed(event.getId(), exception.getMessage());
                    log.error("Failed to send outbox event (status={}) id={}, correlationId={}",
                            event.getStatus(),
                            event.getId(),
                            event.getCorrelationId());

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
                            "Successfully sent outbox event id={}, topic={}, partition={}, offset={}",
                            event.getId(),
                            result.getRecordMetadata().topic(),
                            result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset()  // порядковый номер внутри партиции
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

    /**
     * Очистка старых обработанных событий PUBLISHED для предотвращения роста базы данных.
     * <p>Выбирает события старше RETENTION_DAYS и удаляет их. Запускается раз в неделю по cron.
     * <p>cron = "0 0 3 ? * MON" → каждый понедельник в 03:00
     */
    @Scheduled(cron = "0 0 3 ? * MON")
    public void cleanupOldPublishedEvents() {
        try {
            // получаем количество удаленных записей
            int deleted = outboxRepository.deleteOldEvents(RETENTION_DAYS);
            log.info("Outbox cleanup completed: deleted {} PUBLISHED events older than {} days",
                    deleted, RETENTION_DAYS.toDays());
        } catch (Exception e) {
            log.error("Failed to clean up old PUBLISHED outbox events", e);
        }
    }
}

/* todo:
    Трассировка / observability: добавь логи и метрики (published, failed, latency).
    В методе cleanupOldPublishedEvents() - можно также реализовать удаление батчами или асинхронное удаление
 */

