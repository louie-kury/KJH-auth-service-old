package com.louiekury.auth.global.web.exception;

import com.louiekury.auth.global.web.exception.ErrorCodeHttpMapper;
import com.louiekury.auth.shared.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

class SignupDuplicateErrorCodeTest {
    @Test
    @DisplayName("이메일 아이디 닉네임 중복 오류를 충돌 상태로 매핑한다")
    void allThreeDuplicateErrorsExistAndAreMappedToConflict() {
        for (String name : new String[]{"EMAIL_ALREADY_EXISTS", "LOGIN_ID_ALREADY_EXISTS", "NICKNAME_ALREADY_EXISTS"}) {
            ErrorCode code = ErrorCode.valueOf(name);
            assertThat(ErrorCodeHttpMapper.toStatus(code)).as(name).isEqualTo(HttpStatus.CONFLICT);
        }
    }
}
