package com.bankx.paymentorchestrator.model.dto;

import com.bankx.paymentorchestrator.model.enums.PaymentMethod;
import com.bankx.paymentorchestrator.validator.annotation.ValidCurrency;
import lombok.*;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRequest {
    @NotBlank(message = "sourceAccountId cannot be empty")
    private String sourceAccountId;

    @Pattern(regexp = "CARD|WALLET", message = "method can be 'CARD' or 'WALLET' only")
    private PaymentMethod paymentMethod;

    @NotNull(message = "amount cannot be null")
    @DecimalMin(value = "0.01", message = "amount must be more than 0")
    private BigDecimal amount;

    @NotBlank(message = "currency cannot be empty")
    @Pattern(
            regexp = "^[A-Z]{3}$",
            message = "currency must contain exactly three uppercase Latin letters"
    )
    @ValidCurrency
    private String currency;

    private Map<String, Object> pspData;

    @Builder.Default
    private String requestId = UUID.randomUUID().toString();  // Автоматическая генерация уникального requestId

    public void validate() {
        if (pspData == null && (paymentMethod == PaymentMethod.CARD || paymentMethod == PaymentMethod.WALLET)) {
            throw new IllegalArgumentException("pspData is required for " + paymentMethod + " method");
        }
    }
}