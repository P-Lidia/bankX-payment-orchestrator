package com.bankx.paymentorchestrator.outbox.model;

public enum OutboxStatus {
    NEW,
    PUBLISHED,
    FAILED
}
// todo подумать, нужен ли этот класс?