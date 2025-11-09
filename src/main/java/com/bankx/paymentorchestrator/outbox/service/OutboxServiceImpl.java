package com.bankx.paymentorchestrator.outbox.service;

import com.bankx.paymentorchestrator.outbox.model.OutboxEvent;
import com.bankx.paymentorchestrator.outbox.repository.OutboxRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OutboxServiceImpl implements OutboxService {
    private final OutboxRepository outboxRepository;

    public void saveEvent() {
    }

    public List<OutboxEvent> findUnpublished() {
        return null;
    }

    public void markAsPublished(Long id) {
    }

    // todo отдельный слой EventMapper или EventConverter, чтобы OutboxEvent переводить в KafkaEvent - ??
}