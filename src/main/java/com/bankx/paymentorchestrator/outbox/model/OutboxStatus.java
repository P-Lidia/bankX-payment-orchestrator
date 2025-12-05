package com.bankx.paymentorchestrator.outbox.model;

public enum OutboxStatus {
    NEW,
    PUBLISHED,
    FAILED,
    DEAD  // todo статус для событий, которые так и не отправились
}