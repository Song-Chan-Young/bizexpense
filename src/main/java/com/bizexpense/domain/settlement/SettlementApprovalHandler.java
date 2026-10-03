package com.bizexpense.domain.settlement;

import com.bizexpense.domain.approval.ApprovalHandler;
import com.bizexpense.domain.approval.ApprovalTargetType;
import com.bizexpense.domain.expense.Expense;
import com.bizexpense.domain.expense.ExpenseRepository;
import com.bizexpense.global.error.BusinessException;
import com.bizexpense.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 정산 결재 결과를 정산과 포함된 경비 상태에 반영한다.
 * ApprovalService 가 처리기를 주입받으므로, 순환 참조를 피하려고 SettlementService 대신 저장소를 직접 쓴다.
 */
@Component
@RequiredArgsConstructor
public class SettlementApprovalHandler implements ApprovalHandler {

    private final SettlementRepository settlementRepository;
    private final ExpenseRepository expenseRepository;

    @Override
    public ApprovalTargetType targetType() {
        return ApprovalTargetType.SETTLEMENT;
    }

    @Override
    public void onApproved(Long settlementId) {
        find(settlementId).approve();
        expenseRepository.findBySettlementId(settlementId).forEach(Expense::approveSettlement);
    }

    @Override
    public void onRejected(Long settlementId) {
        find(settlementId).reject();
        expenseRepository.findBySettlementId(settlementId).forEach(Expense::rejectSettlement);
    }

    private Settlement find(Long settlementId) {
        return settlementRepository.findById(settlementId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SETTLEMENT_NOT_FOUND));
    }
}
