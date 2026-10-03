package com.bizexpense.domain.trip;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 출장 상태.
 * <pre>
 * DRAFT ──신청──▶ REQUESTED ──승인──▶ APPROVED ──시작──▶ IN_PROGRESS ──완료──▶ COMPLETED
 *   ▲                 │
 *   │               반려
 *   │                 ▼
 *   └──(수정 후 재신청)─ REJECTED
 *
 * DRAFT / REQUESTED / APPROVED 에서는 취소(CANCELLED) 가능
 * </pre>
 */
@Getter
@RequiredArgsConstructor
public enum TripStatus {

    DRAFT("임시저장"),
    REQUESTED("신청"),
    APPROVED("승인"),
    IN_PROGRESS("진행중"),
    COMPLETED("완료"),
    CANCELLED("취소"),
    REJECTED("반려");

    private final String label;

    /** 내용 수정과 (재)신청은 임시저장 또는 반려 상태에서만 가능하다. */
    public boolean isEditable() {
        return this == DRAFT || this == REJECTED;
    }

    public boolean isCancellable() {
        return this == DRAFT || this == REQUESTED || this == APPROVED;
    }

    /** 경비를 등록할 수 있는 상태: 출장이 시작된 뒤 (진행중, 완료) */
    public boolean isExpensable() {
        return this == IN_PROGRESS || this == COMPLETED;
    }

    /** 일정을 연결할 수 있는 상태 (끝났거나 취소된 출장 제외) */
    public boolean isSchedulable() {
        return this != COMPLETED && this != CANCELLED;
    }
}
