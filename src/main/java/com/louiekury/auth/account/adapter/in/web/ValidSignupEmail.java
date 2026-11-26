package com.louiekury.auth.account.adapter.in.web;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Documented
@Constraint(validatedBy = SignupEmailValidator.class)
@Target(FIELD)
@Retention(RUNTIME)
public @interface ValidSignupEmail {
    String message() default "email은 올바른 ASCII 이메일 형식이어야 합니다.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
