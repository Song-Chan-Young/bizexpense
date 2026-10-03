package com.bizexpense.domain.settlement;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 정산 상태.
 * <pre>
 * REQUESTED ──팀장 승인──▶ APPROVED ──관리자 지급 처리──▶ SETTLED
 *     │  ▲
 *   반려 │ 경비 수정 후 재신청
 *     ▼  │
 *   REJECTED
 * </pre>
 */
@Getter
@RequiredArgsConstructor
public enum SettlementStatus {

    REQUESTED("정산신청"),
    APPROVED("승인"),
    REJECTED("반려"),
    SETTLED("정산완료");

    private final String label;

    /** 진행 중(같은 출장에 새 정산을 만들 수 없는 상태) */
    public boolean isOpen() {
        return this == REQUESTED || this == APPROVED || this == REJECTED;
    }
}
