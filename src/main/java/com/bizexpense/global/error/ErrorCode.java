package com.bizexpense.global.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // 공통
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "입력값이 올바르지 않습니다."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 데이터를 찾을 수 없습니다."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "지원하지 않는 요청 방식입니다."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다."),

    // 인증 / 권한
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않거나 만료된 토큰입니다."),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "아이디 또는 비밀번호가 올바르지 않습니다."),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),

    // 사용자
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),
    DISABLED_USER(HttpStatus.FORBIDDEN, "사용이 중지된 계정입니다."),

    // 일정
    SCHEDULE_NOT_FOUND(HttpStatus.NOT_FOUND, "일정을 찾을 수 없습니다."),
    INVALID_SCHEDULE_PERIOD(HttpStatus.BAD_REQUEST, "종료 일시는 시작 일시보다 뒤여야 합니다."),
    SCHEDULE_CONFLICT(HttpStatus.CONFLICT, "같은 시간대에 다른 일정이 있습니다."),
    SCHEDULE_OUT_OF_TRIP_PERIOD(HttpStatus.BAD_REQUEST, "출장에 연결한 일정은 출장 기간 안에 있어야 합니다."),

    // 출장
    TRIP_NOT_FOUND(HttpStatus.NOT_FOUND, "출장을 찾을 수 없습니다."),
    INVALID_TRIP_PERIOD(HttpStatus.BAD_REQUEST, "출장 종료일은 시작일과 같거나 뒤여야 합니다."),
    INVALID_TRIP_STATUS(HttpStatus.CONFLICT, "현재 출장 상태에서는 처리할 수 없습니다."),
    TRIP_NOT_LINKABLE(HttpStatus.BAD_REQUEST, "일정을 연결할 수 없는 출장입니다."),
    TRIP_SCHEDULE_OUT_OF_RANGE(HttpStatus.BAD_REQUEST, "변경한 출장 기간을 벗어나는 연결 일정이 있습니다."),

    // 결재
    APPROVAL_NOT_FOUND(HttpStatus.NOT_FOUND, "결재 건을 찾을 수 없습니다."),
    APPROVER_NOT_FOUND(HttpStatus.CONFLICT, "결재할 팀장(또는 관리자)이 없습니다."),
    APPROVAL_ALREADY_PROCESSED(HttpStatus.CONFLICT, "이미 처리된 결재 건입니다."),
    REJECT_COMMENT_REQUIRED(HttpStatus.BAD_REQUEST, "반려 사유를 입력하세요.");

    private final HttpStatus status;
    private final String message;
}
