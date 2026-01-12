package com.bankx.paymentorchestrator.outbox.mapper;

import com.bankx.paymentorchestrator.model.dto.TransferRequest;
import com.bankx.paymentorchestrator.outbox.model.TransferPayload;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

/**
 * Маппер для преобразования входящего запроса {@link TransferRequest}
 * в объект {@link TransferPayload}, который используется в механизме Outbox и при публикации событий в Kafka.
 * <p>
 * Основная цель — разделить ответственность между:
 * <ul>
 *   <li>{@code TransferRequest} — DTO, получаемое от внешнего API, содержащее данные клиента и валидацию;</li>
 *   <li>{@code TransferPayload} — DTO, описывающее полезную нагрузку события, сохраняемого в Outbox
 *   и публикуемого в Kafka.</li>
 * </ul>
 * Такой подход обеспечивает чистую архитектуру, упрощает сериализацию
 * и позволяет независимо развивать внутренние и внешние модели данных.
 */

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface TransferMapper {

    TransferPayload toTransferPayload(TransferRequest request);
}