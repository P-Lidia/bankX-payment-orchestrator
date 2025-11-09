package com.bankx.paymentorchestrator.outbox.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class OutboxRepository {

    private final JdbcTemplate jdbcTemplate;

    // todo в методах прописывают SQL-запросы через jdbcTemplate.query()

    /* примерные методы:
    save
    findUnpublished
    markAsPublished
     */

    // todo повторная отправка, при неудаче и статусе  FAILED

}
