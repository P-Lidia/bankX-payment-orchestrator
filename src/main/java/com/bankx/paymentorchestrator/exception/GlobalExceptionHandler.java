package com.bankx.paymentorchestrator.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(InvalidCurrencyException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidCurrency(InvalidCurrencyException ex) {

        Map<String, Object> response = new HashMap<>();
        response.put("error", "Invalid currency");
        response.put("message", ex.getMessage());
        response.put("timestamp", Instant.now().toString());

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }
}
