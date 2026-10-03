package com.bizexpense.global.common;

import com.bizexpense.global.error.ErrorCode;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 모든 API의 공통 응답 형식.
 * 성공: { "success": true, "data": {...} }
 * 실패: { "success": false, "error": { "code": "...", "message": "..." } }
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(boolean success, T data, ErrorBody error) {

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null);
    }

    public static ApiResponse<Void> ok() {
        return new ApiResponse<>(true, null, null);
    }

    public static ApiResponse<Void> fail(ErrorCode errorCode) {
        return fail(errorCode, errorCode.getMessage());
    }

    public static ApiResponse<Void> fail(ErrorCode errorCode, String message) {
        return new ApiResponse<>(false, null, new ErrorBody(errorCode.name(), message));
    }

    public record ErrorBody(String code, String message) {
    }
}
