package com.bankx.paymentorchestrator.controller;

import com.bankx.paymentorchestrator.model.dto.PaymentRequest;
import com.bankx.paymentorchestrator.model.dto.PaymentResponse;
import com.bankx.paymentorchestrator.model.dto.TransferRequest;
import com.bankx.paymentorchestrator.model.enums.PaymentMethod;
import com.bankx.paymentorchestrator.model.enums.PaymentStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/payments")
public class PaymentController {

    // In-memory Map для idempotency: общая для обоих эндпоинтов
    // ключ — requestId (строка), значение — ID транзакции (transferId или paymentId, строка).
    // (ConcurrentHashMap для потокобезопасности)
    private final Map<String, String> requestIdToIdMap = new ConcurrentHashMap<>();

    @PostMapping("/transfer")
    public ResponseEntity<PaymentResponse> transfer(
            @RequestBody TransferRequest request,
            @RequestHeader("Idempotency-Key") String idempotencyKey
    ) {
        // Валидация из модели TransferRequest
        try {
            request.validate();
        } catch (IllegalArgumentException e) {
            // общий фабричный метод в PaymentResponse для FAILED-ответов (например, failedResponse())
            return ResponseEntity.badRequest().body(PaymentResponse.failedResponse(e.getMessage()));
        }

        // Проверка на дубли (idempotency): если requestId уже есть, вернуть существующий статус
        String existingTransferId = requestIdToIdMap.get(request.getRequestId());
        if (existingTransferId != null) {
            return ResponseEntity.ok(PaymentResponse.transferResponse(existingTransferId, PaymentStatus.PENDING, null));
        }

        // Генерация transferId
        String transferId = UUID.randomUUID().toString();

        // Сохранить в Map для idempotency
        requestIdToIdMap.put(request.getRequestId(), transferId);

        // Happy path: успешный ответ с transferId и статусом PENDING
        // Возвращаем успешный ответ (200 OK) с ожидаемым результатом (PENDING) в случае корректного запроса без ошибок.
        // Возвращаем PaymentResponse с заглушкой(status "PENDING", без вызова Transfer Service)
        return ResponseEntity.ok(PaymentResponse.transferResponse(transferId, PaymentStatus.PENDING, null));
    }

    @PostMapping("/pay")
    public ResponseEntity<PaymentResponse> pay(
            @RequestBody PaymentRequest request,
            @RequestHeader("Idempotency-Key") String idempotencyKey
    ) {
        // Валидация из модели PaymentRequest
        try {
            request.validate();
        } catch (IllegalArgumentException e) {
            // общий фабричный метод в PaymentResponse для FAILED-ответов (например, failedResponse())
            return ResponseEntity.badRequest().body(PaymentResponse.failedResponse(e.getMessage()));
        }

        // Проверка на дубли (idempotency): если requestId уже есть, вернуть существующий статус
        String existingPaymentId = requestIdToIdMap.get(request.getRequestId());
        if (existingPaymentId != null) {
            // Проверка на дубли: проверить, есть ли requestId в Map, и если да,вернуть существующий paymentId с PENDING
            // через соответствующий фабричный метод (cardResponse или walletResponse)
            // Выбор cardResponse / walletResponse идет по paymentMethod.
            if (request.getPaymentMethod() == PaymentMethod.CARD) {
                return ResponseEntity.ok(PaymentResponse.cardResponse(existingPaymentId, PaymentStatus.PENDING, null));
            } else if (request.getPaymentMethod() == PaymentMethod.WALLET) {
                return ResponseEntity.ok(PaymentResponse.walletResponse(existingPaymentId, PaymentStatus.PENDING, null));
            } else {
                // Если method null или другой — ошибка (хотя валидация в модели должна предотвратить)
                return ResponseEntity.badRequest().body(PaymentResponse.failedResponse("Invalid payment method"));
            }
        }

        // Генерация paymentId
        String paymentId = UUID.randomUUID().toString();

        // Сохранить в Map для idempotency
        requestIdToIdMap.put(request.getRequestId(), paymentId);

        // Тут идет логика для happy path, тк возвращается успешный ответ с paymentId и PENDING для нового платежа.
        // Сейчас это простая операция обработки запроса, так как это заглушка без реальной транзакции или саги.
        // Операция заканчивается тем, что возвращаем ответ клиенту без запуска транзакции или саги (заглушка).
        // Когда это будет не заглушка, дальше будет запускаться сага (оркестрация транзакции), которая может включать
        // вызовы Feign-клиентов и обработку в State Machine, а не в контроллере, где останется только инициация
        // TODO: В задачах 006/007 добавить вызовы Feign-клиентов PSP (Card/Wallet) вместо заглушки PENDING — выбор метода уже частично реализован здесь
        // В задачах 006/007 (когда добавим Feign-клиенты для PSP Card и Wallet) вместо немедленного возврата статуса
        // "PENDING" (заглушка) нужно будет вызвать соответствующий Feign-клиент (например, для CARD —
        // pspCardClient.pay(request)), получить реальный статус от PSP (APPROVED/DECLINED), и вернуть его в
        // PaymentResponse. Это сделает ответы не заглушкой, а реальными.
        if (request.getPaymentMethod() == PaymentMethod.CARD) {
            return ResponseEntity.ok(PaymentResponse.cardResponse(paymentId, PaymentStatus.PENDING, null));
        } else if (request.getPaymentMethod() == PaymentMethod.WALLET) {
            return ResponseEntity.ok(PaymentResponse.walletResponse(paymentId, PaymentStatus.PENDING, null));
        } else {
            // Если method null или другой — ошибка (хотя валидация в модели должна предотвратить)
            return ResponseEntity.badRequest().body(PaymentResponse.failedResponse("Invalid payment method"));
        }
    }

}
