package com.bizexpense.domain.audit;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 감사 로그에 남기는 작업. 대상 종류(targetType)는 화면에서 묶어 보기 위한 값이다. */
@Getter
@RequiredArgsConstructor
public enum AuditAction {

    LOGIN("로그인", "AUTH"),
    LOGOUT("로그아웃", "AUTH"),

    SCHEDULE_CREATE("일정 등록", "SCHEDULE"),
    SCHEDULE_UPDATE("일정 수정", "SCHEDULE"),
    SCHEDULE_DELETE("일정 삭제", "SCHEDULE"),

    TRIP_CREATE("출장 등록", "TRIP"),
    TRIP_UPDATE("출장 수정", "TRIP"),
    TRIP_DELETE("출장 삭제", "TRIP"),
    TRIP_REQUEST("출장 결재 신청", "TRIP"),
    TRIP_CANCEL("출장 취소", "TRIP"),
    TRIP_START("출장 시작", "TRIP"),
    TRIP_COMPLETE("출장 완료", "TRIP"),

    EXPENSE_CREATE("경비 등록", "EXPENSE"),
    EXPENSE_UPDATE("경비 수정", "EXPENSE"),
    EXPENSE_DELETE("경비 삭제", "EXPENSE"),
    FILE_UPLOAD("영수증 첨부", "EXPENSE"),
    FILE_DELETE("영수증 삭제", "EXPENSE"),

    SETTLEMENT_REQUEST("정산 신청", "SETTLEMENT"),
    SETTLEMENT_RESUBMIT("정산 재신청", "SETTLEMENT"),
    SETTLEMENT_COMPLETE("정산 지급 완료", "SETTLEMENT"),

    APPROVAL_APPROVE("결재 승인", "APPROVAL"),
    APPROVAL_REJECT("결재 반려", "APPROVAL"),

    CATEGORY_CREATE("비용 항목 등록", "CODE"),
    CATEGORY_UPDATE("비용 항목 수정", "CODE"),
    PAYMENT_METHOD_CREATE("결제 수단 등록", "CODE"),
    PAYMENT_METHOD_UPDATE("결제 수단 수정", "CODE");

    private final String label;
    private final String targetType;
}
