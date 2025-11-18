package com.bankx.paymentorchestrator.outbox.model;

public enum OutboxStatus {
    NEW,
    PUBLISHED,
    FAILED,
    DEAD  // статус для событий, которые так и не отправились
}