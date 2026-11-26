package com.louiekury.auth.account.adapter.in.web;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.time.LocalDate;

public class SignupBirthDateValidator implements ConstraintValidator<ValidSignupBirthDate, LocalDate> {
    @Override
    public boolean isValid(LocalDate date, ConstraintValidatorContext context) {
        return date == null || (date.getYear() >= 1000 && date.getYear() <= 9999);
    }
}
