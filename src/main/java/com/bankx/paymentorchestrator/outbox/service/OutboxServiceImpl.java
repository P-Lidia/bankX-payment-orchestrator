package com.bankx.paymentorchestrator.outbox.service;

import com.bankx.paymentorchestrator.model.dto.TransferRequest;
import com.bankx.paymentorchestrator.outbox.mapper.TransferMapper;
import com.bankx.paymentorchestrator.outbox.model.OutboxEvent;
import com.bankx.paymentorchestrator.outbox.model.OutboxStatus;
import com.bankx.paymentorchestrator.outbox.model.TransferPayload;
import com.bankx.paymentorchestrator.outbox.repository.OutboxRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

/**
 * Сервис для работы с Outbox-событиями.
 * <p>
 * Основная задача класса — сохранять события переводов в таблицу {@code outbox_events},
 * чтобы они потом могли быть отправлены в Kafka или обработаны другими системами.
 * </p>
 *
 * <p>
 * Принцип работы класса:
 * <ol>
 *     <li>Получает объект {@link TransferRequest} от контроллера.</li>
 *     <li>Проверяет и преобразует его в {@link TransferPayload} — DTO с минимально необходимыми данными для события.</li>
 *     <li>Сериализует {@link TransferPayload} в JSON-строку, чтобы сохранить в БД.</li>
 *     <li>Создаёт {@link OutboxEvent} с необходимыми полями: payload, статус, время создания, идентификатор и т.д.</li>
 *     <li>Сохраняет событие через {@link OutboxRepository} в таблицу {@code outbox_events}.</li>
 * </ol>
 * </p>
 *
 * <p>
 * Использование транзакции (@Transactional) гарантирует, что запись события
 * будет атомарной: либо полностью сохранится, либо не сохранится при ошибке.
 * </p>
 *
 * <p>
 * В случае ошибки сериализации payload выбрасывается {@link IllegalStateException},
 * чтобы гарантировать, что некорректные данные не попадут в Outbox.
 * </p>
 *
 * <p>
 * Поля и зависимости:
 * <ul>
 *     <li>{@link OutboxRepository} — для работы с таблицей outbox_events в базе данных.</li>
 *     <li>{@link ObjectMapper} — для преобразования объектов в JSON.</li>
 *     <li>{@link TransferMapper} — для преобразования {@link TransferRequest} в {@link TransferPayload}.</li>
 * </ul>
 * </p>
 *
 * <p>
 * Логирование:
 * При успешном сохранении события выводится идентификатор события и статус NEW.
 * </p>
 */

@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxServiceImpl implements OutboxService {
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;
    private final TransferMapper transferMapper;

    public void saveEvent(TransferRequest request, UUID correlationId) {
        // сериализация объекта TransferPayload в JSON
        String payloadJson = convertToJson(transferMapper.toTransferPayload(request));

        OutboxEvent outboxEvent = OutboxEvent.builder()
                .correlationId(correlationId)
                .payload(payloadJson)
                .status(OutboxStatus.NEW)
                .createAt(Instant.now())
                .updateAt(Instant.now())
                .retryCount(0)
                .build();

        outboxRepository.save(outboxEvent);

        log.info("Outbox event {} saved with status NEW", outboxEvent.getId());
    }

    /**
     * Преобразует объект {@link TransferPayload} в JSON-строку для сохранения в поле {@code payload} таблицы Outbox.
     * <p>
     * Используется, чтобы сериализовать данные перевода в формат, который удобно хранить в БД
     * и потом отправлять в Kafka.
     * </p>
     *
     * @param payload объект с данными перевода (счет отправителя, счет получателя, сумма, валюта)
     * @return JSON-строка, представляющая данные перевода
     * @throws IllegalStateException если сериализация не удалась
     */
    private String convertToJson(TransferPayload payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            log.error("Error serializing Outbox payload", e);
            throw new IllegalStateException("Failed to serialize payload to JSON", e);
        }
    }
}