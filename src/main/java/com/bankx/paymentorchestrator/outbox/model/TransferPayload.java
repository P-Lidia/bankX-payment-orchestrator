package com.bankx.paymentorchestrator.outbox.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO с информацией о переводе для публикации события в Kafka через Outbox.
 * <p>
 * Содержит все необходимые данные о переводе между счетами:
 * </p>
 * <ul>
 *     <li>{@code fromAccountId} - ID счета, с которого списываются средства.</li>
 *     <li>{@code toAccountId} - ID счета, на который зачисляются средства.</li>
 *     <li>{@code amount} - сумма перевода.</li>
 *     <li>{@code currency} - код валюты перевода (ISO 4217, например "USD").</li>
 * </ul>
 * <p>
 * Объект сериализуется в JSON при сохранении в таблице Outbox и при отправке в Kafka.
 * </p>
 *
 * @author P-Lidia
 */

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransferPayload {
    private String fromAccountId;
    private String toAccountId;
    private BigDecimal amount;
    private String currency;
}