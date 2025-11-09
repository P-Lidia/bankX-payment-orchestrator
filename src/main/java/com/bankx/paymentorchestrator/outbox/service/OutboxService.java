package com.bankx.paymentorchestrator.outbox.service;

import com.bankx.paymentorchestrator.outbox.model.OutboxEvent;

import java.util.List;

public interface OutboxService {
    public void saveEvent();
    public List<OutboxEvent> findUnpublished();
    public void markAsPublished(Long id); // todo маркировка по одному или тоже листом ??
}
