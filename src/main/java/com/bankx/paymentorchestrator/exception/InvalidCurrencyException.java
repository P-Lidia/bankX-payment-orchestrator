package com.bankx.paymentorchestrator.exception;

public class InvalidCurrencyException extends RuntimeException {
    public InvalidCurrencyException(String currency) {
        super("Invalid currency: " + currency);
    }

}
