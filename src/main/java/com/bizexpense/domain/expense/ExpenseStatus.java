package com.bizexpense.domain.expense;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 경비 상태. 등록 후에는 정산(Phase 8) 흐름에 따라 바뀐다.
 * <pre>
 * DRAFT ──정산신청──▶ REQUESTED ──승인──▶ APPROVED ──정산완료──▶ SETTLED
 *   ▲                     │
 *   └──(수정 후 재신청)── REJECTED ◀──반려──┘
 * </pre>
 */
@Getter
@RequiredArgsConstructor
public enum ExpenseStatus {

    DRAFT("임시저장"),
    REQUESTED("정산신청"),
    APPROVED("승인"),
    REJECTED("반려"),
    SETTLED("정산완료");

    private final String label;

    /** 수정/삭제는 정산 신청 전(임시저장)이나 반려된 경우만 가능 */
    public boolean isEditable() {
        return this == DRAFT || this == REJECTED;
    }
}
