package com.bankx.paymentorchestrator.model.dto;

import com.bankx.paymentorchestrator.model.enums.PaymentMethod;
import com.bankx.paymentorchestrator.model.enums.PaymentStatus;
import lombok.*;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {
    private String transferId;
    private String cardPaymentId;
    private String walletPaymentId;

    @NotNull
    private PaymentMethod paymentMethod;

    @NotNull(message = "status cannot be null")
    @Pattern(regexp = "PENDING|APPROVED|DECLINED|FAILED|COMPLETED",
            message = "status must be: PENDING, APPROVED, DECLINED, FAILED, COMPLETED")
    private PaymentStatus status;

    private String message;

//    public boolean isTransfer() {
//        return transferId != null && !transferId.trim().isEmpty();
//    }

    public boolean isCardPayment() {
        return PaymentMethod.CARD.equals(paymentMethod);
    }

    public boolean isWalletPayment() {
        return PaymentMethod.WALLET.equals(paymentMethod);
    }


    public void validate() {
        if (paymentMethod == null) {
            throw new IllegalArgumentException("paymentMethod cannot be null");
        }

        if (status == null) {
            throw new IllegalArgumentException("status cannot be null");
        }

        validateMethodAndIdConsistency();
    }

    private void validateMethodAndIdConsistency() {
        switch (paymentMethod) {
            case CARD:
                if (cardPaymentId == null) {
                    throw new IllegalArgumentException("cardPaymentId is mandatory for card payments");
                }
                break;
            case WALLET:
                if (walletPaymentId == null) {
                    throw new IllegalArgumentException("walletId is mandatory for wallet payments");
                }
                break;
        }
    }



    public static PaymentResponse cardResponse(String cardPaymentId, PaymentStatus status, String message) {
        return PaymentResponse.builder()
                .cardPaymentId(cardPaymentId)
                .paymentMethod(PaymentMethod.CARD)
                .status(status)
                .message(message)
                .build();
    }

    public static PaymentResponse walletResponse(String walletPaymentId, PaymentStatus status, String message) {
        return PaymentResponse.builder()
                .walletPaymentId(walletPaymentId)
                .paymentMethod(PaymentMethod.WALLET)
                .status(status)
                .message(message)
                .build();
    }

    public static PaymentResponse transferResponse(String transferId, PaymentStatus status, String message) {
        return PaymentResponse.builder()
                .transferId(transferId)
                .paymentMethod(null) //можно в инам добавить трансфер тоже
                .status(status)
                .message(message)
                .build();
    }
}