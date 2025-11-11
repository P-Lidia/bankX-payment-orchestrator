package com.bankx.paymentorchestrator.outbox.repository;

import com.bankx.paymentorchestrator.outbox.model.OutboxEvent;
import com.bankx.paymentorchestrator.outbox.model.OutboxStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Репозиторий для работы с таблицей {@code outbox_events}.
 * <p>Основная задача класса — работа с событиями outbox: сохранение новых событий,
 * выборка по статусу, обновление статуса и счетчика повторных попыток, а также удаление старых записей.
 *
 * <p>Использует {@link JdbcTemplate} для выполнения SQL-запросов без ORM.
 */

@Repository
@RequiredArgsConstructor
public class OutboxRepository {

    private final JdbcTemplate jdbcTemplate;

    /**
     * Сохраняет новое событие в таблицу outbox_events.
     *
     * @param event объект OutboxEvent, содержащий все необходимые поля для записи в БД
     */
    public void save(OutboxEvent event) {
        final String sql = """
                INSERT INTO outbox_events (
                id,
                correlation_id,
                event_type,
                payload,
                status,
                create_at,
                update_at,
                retry_count,
                error_message
                ) VALUES (?, ?, ?, ?::jsonb, ?, ?, ?, ?, ?)
                """;
        jdbcTemplate.update(
                sql,
                event.getId(),
                event.getCorrelationId(),
                event.getEventType(),
                event.getPayload(),
                event.getStatus().name(),
                event.getCreateAt(),
                event.getUpdateAt(),
                event.getRetryCount(),
                event.getErrorMessage()
        );
    }

    /**
     * Находит события по указанному статусу.
     * <p>Используется для получения списка событий, которые нужно обработать или отправить.
     *
     * @param status статус события для фильтрации (например, NEW или FAILED)
     * @return список событий с указанным статусом
     */
    public List<OutboxEvent> findByStatus(OutboxStatus status) {
        return jdbcTemplate.query(
                "SELECT * FROM outbox_events WHERE status=? ORDER BY create_at ASC",
                new OutboxEventRowMapper(),
                status.name());
    }

    /**
     * Обновляет статус события на {@code PUBLISHED}.
     *
     * @param id идентификатор события
     */
    public void markAsPublished(UUID id) {
        jdbcTemplate.update("UPDATE outbox_events SET status='PUBLISHED' WHERE id=?", id);
    }

    /**
     * Обновляет статус события на {@code FAILED} и сохраняет сообщение об ошибке.
     *
     * @param id           идентификатор события
     * @param errorMessage описание ошибки
     */
    public void markAsFailed(UUID id, String errorMessage) {
        jdbcTemplate.update(
                "UPDATE outbox_events SET status='FAILED', error_message=? WHERE id=?", errorMessage, id
        );
    }

    /**
     * Увеличивает значение {@code retry_count} для события.
     * <p>Используется при повторных попытках отправки события для ограничения количества попыток
     *
     * @param id         идентификатор события
     * @param retryCount новое значение счетчика повторных попыток
     */
    public void incrementRetryCount(UUID id, int retryCount) {
        jdbcTemplate.update("UPDATE outbox_events SET retry_count=? WHERE id=?", retryCount, id);
    }

    /**
     * Удаляет события старше указанного времени.
     * <p>
     * Метод возвращает количество удалённых записей для логирования.
     *
     * @param olderThan период, указывающий, какие события считать старыми
     * @return количество удалённых строк
     */
    public int deleteOldEvents(Duration olderThan) {
        Instant cutoff = Instant.now().minus(olderThan);
        return jdbcTemplate.update("DELETE FROM outbox_events WHERE create_at < ?", Timestamp.from(cutoff));
    }
}