package com.bankx.paymentorchestrator.outbox.repository;

import com.bankx.paymentorchestrator.outbox.model.OutboxEvent;
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
                created_at,
                updated_at,
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
                event.getCreatedAt(),
                event.getUpdatedAt(),
                event.getRetryCount(),
                event.getErrorMessage()
        );
    }

    /**
     * Находит события со статусом NEW.
     * <p>Используется для получения списка событий, которые нужно отправить.
     *
     * @param batchSize максимальное количество событий в выборке
     * @return список событий, отсортированных по времени создания (ASC)
     */
    public List<OutboxEvent> findByStatusNew(int batchSize) {
        return jdbcTemplate.query(
                "SELECT * FROM outbox_events WHERE status='NEW' ORDER BY created_at ASC LIMIT ?",
                new OutboxEventRowMapper(),
                batchSize);
    }

    /**
     * Находит события, ранее завершившиеся ошибкой, но ещё не достигшие лимита повторных попыток.
     * <p>Используется шедулером для ретраев.
     *
     * @param maxRetries максимальное допустимое количество попыток
     * @param batchSize  максимальное количество записей в выборке
     * @return список событий со статусом {@code FAILED} и {@code retry_count < maxRetries},
     * отсортированных по времени последнего обновления
     */
    public List<OutboxEvent> findByStatusFailed(int maxRetries, int batchSize) {
        return jdbcTemplate.query("""
                        SELECT * FROM outbox_events
                        WHERE status='FAILED' AND retry_count < ?
                        ORDER BY updated_at ASC
                        LIMIT ?
                        """,
                new OutboxEventRowMapper(),
                maxRetries,
                batchSize);
    }

    /**
     * Обновляет статус события на {@code PUBLISHED}.
     * <p>
     * * <p>Также очищает {@code error_message} (если ранее были неудачные попытки отправки)
     * и обновляет поле {@code updated_at}.
     *
     * @param id идентификатор события
     */
    public void markAsPublished(UUID id) {
        jdbcTemplate.update("""
                        UPDATE outbox_events
                        SET status='PUBLISHED', error_message=NULL, updated_at=NOW()
                        WHERE id=?
                        """,
                id
        );
    }

    /**
     * Обновляет статус события на {@code FAILED}, сохраняет сообщение об ошибке
     * и увеличивает значение {@code retry_count} на 1
     * <p>Также обновляет поле {@code updated_at}.
     *
     * @param id           идентификатор события
     * @param errorMessage описание ошибки
     */
    public void markAsFailed(UUID id, String errorMessage) {
        jdbcTemplate.update("""
                        UPDATE outbox_events
                        SET status='FAILED', error_message=?, retry_count=retry_count + 1, updated_at=NOW()
                        WHERE id=?
                        """,
                errorMessage,
                id
        );
    }

    /**
     * Помечает события как {@code DEAD}, если они достигли или превысили максимальное число повторных попыток.
     * <p>Используется для формирования "мёртвых" сообщений (dead-letter).
     *
     * @param maxRetries допустимый предел повторных попыток
     */
    public void markAsDead(int maxRetries) {
        jdbcTemplate.update("""
                        UPDATE outbox_events
                        SET status='DEAD', updated_at=NOW()
                        WHERE status='FAILED' AND retry_count >= ?
                        """,
                maxRetries
        );
    }

    /**
     * Удаляет события со статусом PUBLISHED старше указанного времени.
     * <p>
     * Метод возвращает количество удалённых записей для логирования.
     *
     * @param retentionDays период, указывающий, какие события считать старыми
     * @return количество удалённых строк
     */
    public int deleteOldEvents(Duration retentionDays) {
        Instant cutoff = Instant.now().minus(retentionDays);
        return jdbcTemplate.update("""
                        DELETE FROM outbox_events
                        WHERE status = 'PUBLISHED'
                        AND created_at < ?
                        """,
                Timestamp.from(cutoff));
    }
}