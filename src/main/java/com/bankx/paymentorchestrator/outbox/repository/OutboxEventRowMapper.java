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

    public static final String ID = "id" ;
    public static final String CORRELATION_ID = "correlation_id" ;
    public static final String EVENT_TYPE = "event_type" ;
    public static final String PAYLOAD = "payload" ;
    public static final String STATUS = "status" ;
    public static final String CREATED_AT = "create_at" ;
    public static final String UPDATED_AT = "update_at" ;
    public static final String RETRY_COUNT = "retry_count" ;
    public static final String ERROR_MESSAGE = "error_message" ;

    @Override
    public OutboxEvent mapRow(ResultSet rs, int rowNum) throws SQLException {
        return OutboxEvent.builder()
                .id(rs.getObject(ID, UUID.class))
                .correlationId(rs.getObject(CORRELATION_ID, UUID.class))
                .eventType(rs.getString(EVENT_TYPE))
                .payload(rs.getString(PAYLOAD))
                .status(OutboxStatus.valueOf(rs.getString(STATUS)))
                .createdAt(rs.getTimestamp(CREATED_AT).toInstant())
                .updatedAt(rs.getTimestamp(UPDATED_AT).toInstant())
                .retryCount(rs.getInt(RETRY_COUNT))
                .errorMessage(rs.getString(ERROR_MESSAGE))
                .build();
    }
}
