package com.bizexpense.domain.settlement.dto;

import com.bizexpense.domain.approval.dto.ApprovalResponse;
import com.bizexpense.domain.expense.dto.ExpenseResponse;
import java.util.List;

/** 정산 상세: 정산 금액 + 포함된 경비 + 결재 이력(반려 사유 포함) */
public record SettlementDetailResponse(
        SettlementResponse settlement,
        List<ExpenseResponse> expenses,
        List<ApprovalResponse> approvals) {
}
