package com.bankx.paymentorchestrator.outbox.service;

import com.bankx.paymentorchestrator.model.dto.TransferRequest;

import java.util.UUID;

public interface OutboxService {
    public void saveEvent(TransferRequest request, UUID correlationId);
}