package com.bizexpense.domain.approval;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ApprovalTargetType {

    TRIP("출장"),
    SETTLEMENT("정산");

    private final String label;
}
