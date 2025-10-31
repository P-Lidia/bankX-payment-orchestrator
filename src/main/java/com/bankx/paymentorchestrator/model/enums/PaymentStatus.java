package com.bankx.paymentorchestrator.model.enums;

import lombok.*;

//@AllArgsConstructor
//@NoArgsConstructor // Можно убрать, если не нужен
@Getter
public enum PaymentStatus {
    PENDING("Pending"),
    APPROVED("Approved"),
    DECLINED("Declined"),
    FAILED("Failed"),
    COMPLETED("Completed");

    private final String description;

    PaymentStatus(String description) {
        this.description = description;
    }
}
