package com.bizexpense.domain.approval;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ApprovalStatus {

    PENDING("대기"),
    APPROVED("승인"),
    REJECTED("반려"),
    /** 신청자가 결재 전에 신청을 취소한 경우 */
    CANCELLED("취소");

    private final String label;
}
