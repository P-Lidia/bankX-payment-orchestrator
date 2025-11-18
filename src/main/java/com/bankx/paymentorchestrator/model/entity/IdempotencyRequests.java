package com.bankx.paymentorchestrator.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name="idempotency_requests")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IdempotencyRequests {

    public enum PaymentStatusType{
        PENDING, CREATED, COMPLETED, FAILED
    }

    @Id
    @Column(name = "idempotency_key", nullable = false, length = 255)
    private String idempotencyKey;

    @Column(name = "request_id", nullable = false, length = 255, unique = true)
    private String requestId;

    @Column(name = "payment_status", nullable = false, length = 255)
    @Enumerated(EnumType.STRING)
    private PaymentStatusType status;

    @Column(name = "response_body", nullable = false,columnDefinition = "jsonb")
    private String responseBody;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
