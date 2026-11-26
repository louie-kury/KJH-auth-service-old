package com.louiekury.auth.account.adapter.in.web;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Documented
@Constraint(validatedBy = SignupBirthDateValidator.class)
@Target(FIELD)
@Retention(RUNTIME)
public @interface ValidSignupBirthDate {
    String message() default "birthDate는 1000-01-01부터 9999-12-31까지 입력해야 합니다.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
