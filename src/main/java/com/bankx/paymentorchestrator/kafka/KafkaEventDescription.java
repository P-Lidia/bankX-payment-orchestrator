package com.bankx.paymentorchestrator.kafka;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Перечисление стандартных описаний событий для Kafka.
 * <p>
 * Используется для унификации текстовых сообщений, сопровождающих событие.
 */

@RequiredArgsConstructor
@Getter
public enum KafkaEventDescription {

    /**
     * Перевод между внутренними счетами.
     */
    INTERNAL_TRANSFER("Internal transfer between accounts"),

    /**
     * Внутренний транш, для внешнего перевода (через PSP).
     */
    INTERNAL_TRANSFER_FOR_EXTERNAL_PAYMENT("Internal leg of an external payment transaction");

    private final String message;
}
