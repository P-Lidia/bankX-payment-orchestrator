package com.bankx.paymentorchestrator.model.dto;

import lombok.*;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransferRequest {
    @NotBlank(message = "fromAccountId cannot be empty")
    private String fromAccountId;

    @NotBlank(message = "toAccountId cannot be empty")
    private String toAccountId;

    @NotNull(message = "amount cannot be null")
    @DecimalMin(value = "0.01", message = "amount must be more than 0")
    private BigDecimal amount;

    @NotBlank(message = "currency cannot be empty")
    @Size(min = 3, max = 3, message = "currency must be a three-character code")
    private String currency;

    @Builder.Default
    private String requestId = UUID.randomUUID().toString();  // Автоматическая генерация уникального requestId

    public void validate() {
        if (fromAccountId.equals(toAccountId)) {
            throw new IllegalArgumentException("fromAccountId and toAccountId cannot be the same");
        }
    }
}