package com.bankx.paymentorchestrator.service.validation;

import com.bankx.paymentorchestrator.model.enums.Currency;
import org.springframework.stereotype.Service;

@Service
public class CurrencyValidationService {
    public boolean isValidCurrency(String currency) {
        if (currency == null || currency.isBlank()) {
            return false;
        }

        try {
            Currency.valueOf(currency.trim().toUpperCase());
            return true;
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }
}
