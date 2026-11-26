package com.louiekury.auth.shared.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    VALIDATION_ERROR("요청 값이 올바르지 않습니다."),
    INVALID_CREDENTIALS("아이디 또는 비밀번호가 올바르지 않습니다."),
    ACCOUNT_UNAVAILABLE("사용할 수 없는 계정입니다."),
    FORBIDDEN("접근 권한이 없습니다."),
    NOT_FOUND("대상을 찾을 수 없습니다."),
    EMAIL_ALREADY_EXISTS("이미 가입된 이메일입니다."),
    LOGIN_ID_ALREADY_EXISTS("이미 사용 중인 아이디입니다."),
    NICKNAME_ALREADY_EXISTS("이미 사용 중인 닉네임입니다."),
    INVALID_EMAIL_VERIFICATION("이메일 인증 증명이 올바르지 않습니다."),
    ACCOUNT_CONFLICT("이미 사용 중인 가입 정보가 있습니다."),
    SIGNUP_PENDING("가입 요청을 처리할 수 없습니다."),
    INVALID_VERIFICATION_CODE("인증번호가 올바르지 않습니다."),
    VERIFICATION_EXPIRED("인증번호가 만료되었습니다."),
    VERIFICATION_UNAVAILABLE("인증 확인을 처리할 수 없습니다."),
    RATE_LIMITED("요청 횟수 제한을 초과했습니다."),
    VERIFICATION_DELIVERY_FAILED("인증 메일을 발송할 수 없습니다."),
    INTERNAL_SERVER_ERROR("서버 내부 오류가 발생했습니다.");

    private final String message;
}
