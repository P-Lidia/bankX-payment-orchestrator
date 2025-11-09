package com.bankx.paymentorchestrator.outbox.repository;

import com.bankx.paymentorchestrator.outbox.model.OutboxEvent;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class OutboxEventMapper implements RowMapper<OutboxEvent> {

    @Override
    public OutboxEvent mapRow(ResultSet rs, int rowNum) throws SQLException {
        return null;  // todo  превращает сырые строки из sql-запросов в OutboxEvent
    }

    // todo отдельный слой EventMapper или EventConverter, чтобы OutboxEvent переводить в KafkaEvent - ??
}
