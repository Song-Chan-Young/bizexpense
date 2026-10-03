package com.bizexpense.global.error;

import lombok.Getter;

/**
 * 업무 규칙 위반 시 서비스 계층에서 던지는 예외.
 * GlobalExceptionHandler 가 ErrorCode 에 맞는 HTTP 상태와 공통 응답으로 변환한다.
 */
@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
