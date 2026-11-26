package com.louiekury.auth.account.adapter.in.web.docs;

import com.louiekury.auth.account.adapter.in.web.dto.response.AvailabilityResponseDto;
import com.louiekury.auth.global.web.response.ApiResponse;
import com.louiekury.auth.global.web.response.OpenApiError;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;

public interface AvailabilityApiDocumentation {
    @Operation(summary = "아이디 중복 확인",
        description = "아이디의 사용 가능 여부를 확인합니다. 필수 값이며 공백만 입력할 수 없고 최대 100 코드 포인트입니다. "
            + "확인 결과는 아이디를 예약하지 않습니다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회에 성공했으며 available에 사용 가능 여부를 반환합니다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "400", description = "VALIDATION_ERROR: 필수 값이 없거나 공백뿐이거나 최대 길이를 초과했습니다.",
        content = @Content(schema = @Schema(implementation = OpenApiError.class)))
    ApiResponse<AvailabilityResponseDto> getLoginIdAvailability(String loginId);

    @Operation(summary = "닉네임 중복 확인",
        description = "닉네임의 사용 가능 여부를 확인합니다. 필수 값이며 공백만 입력할 수 없고 최대 30 코드 포인트입니다. "
            + "확인 결과는 닉네임을 예약하지 않습니다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회에 성공했으며 available에 사용 가능 여부를 반환합니다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "400", description = "VALIDATION_ERROR: 필수 값이 없거나 공백뿐이거나 최대 길이를 초과했습니다.",
        content = @Content(schema = @Schema(implementation = OpenApiError.class)))
    ApiResponse<AvailabilityResponseDto> getNicknameAvailability(String nickname);

}
