package com.bizexpense.domain.settlement;

import com.bizexpense.domain.expense.Expense;
import java.util.List;

/**
 * 정산 금액 계산.
 * <pre>
 * 총 경비       153,000원
 * 법인카드       63,000원  ← 회사가 이미 결제
 * ----------------------
 * 개인 부담      90,000원  ← 개인카드·현금·계좌이체로 직원이 먼저 낸 금액
 * 지급 예정액    90,000원  ← 회사가 직원에게 돌려줄 금액
 * </pre>
 * 지급 예정액은 현재 개인 부담액과 같다. (한도 초과분 차감 같은 규칙이 생기면 여기서 계산한다)
 */
public record SettlementAmounts(long totalAmount, long corporateAmount, long personalAmount,
                                long payableAmount, int expenseCount) {

    public static SettlementAmounts of(List<Expense> expenses) {
        long total = 0;
        long corporate = 0;
        for (Expense e : expenses) {
            total += e.getAmount();
            if (e.isCorporatePaid()) {
                corporate += e.getAmount();
            }
        }
        long personal = total - corporate;
        return new SettlementAmounts(total, corporate, personal, personal, expenses.size());
    }
}
