package com.bankx.paymentorchestrator.service.validation;

import com.bankx.paymentorchestrator.exception.InvalidCurrencyException;
import com.bankx.paymentorchestrator.model.enums.Currency;
import org.springframework.stereotype.Service;

@Service
public class CurrencyValidationService {
    public boolean isValidCurrency(String currency) {
        if (currency == null || currency.isBlank()) {
            throw new InvalidCurrencyException(currency);
        }

        try {
            Currency.valueOf(currency.trim().toUpperCase());
            return true;
        } catch (IllegalArgumentException ex) {
            throw new InvalidCurrencyException(currency);
        }
    }
}
