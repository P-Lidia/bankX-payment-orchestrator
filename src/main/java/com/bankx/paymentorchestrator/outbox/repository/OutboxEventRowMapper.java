package com.bankx.paymentorchestrator.outbox.repository;

import com.bankx.paymentorchestrator.outbox.model.OutboxEvent;
import com.bankx.paymentorchestrator.outbox.model.OutboxStatus;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

/**
 * Преобразует строки результата SQL-запроса из таблицы outbox_events
 * в объекты OutboxEvent.
 *
 * <p>Используется JdbcTemplate: для каждой строки ResultSet вызывается метод mapRow,
 * который извлекает значения колонок и собирает объект OutboxEvent.</p>
 */

public class OutboxEventRowMapper implements RowMapper<OutboxEvent> {

    @Override
    public OutboxEvent mapRow(ResultSet rs, int rowNum) throws SQLException {
        return OutboxEvent.builder()
                .id(rs.getObject("id", UUID.class))
                .correlationId(rs.getObject("correlation_id", UUID.class))
                .eventType(rs.getString("event_type"))
                .payload(rs.getString("payload"))
                .status(OutboxStatus.valueOf(rs.getString("status")))
                .createAt(rs.getTimestamp("create_at").toInstant())
                .updateAt(rs.getTimestamp("update_at").toInstant())
                .retryCount(rs.getInt("retry_count"))
                .errorMessage(rs.getString("error_message"))
                .build();
    }
}
