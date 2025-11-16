package com.bankx.paymentorchestrator.validator;

import com.bankx.paymentorchestrator.service.validation.CurrencyValidationService;
import com.bankx.paymentorchestrator.validator.annotation.ValidCurrency;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CurrencyConstraintValidator implements ConstraintValidator<ValidCurrency, String> {

    private final CurrencyValidationService validationService;

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return validationService.isValidCurrency(value);
    }
}
