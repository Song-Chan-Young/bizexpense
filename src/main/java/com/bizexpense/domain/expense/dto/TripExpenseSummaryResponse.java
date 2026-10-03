package com.bizexpense.domain.expense.dto;

import java.util.List;

/**
 * 출장별 경비 요약.
 *
 * @param corporateAmount 법인 결제 합계 (회사가 이미 지불)
 * @param personalAmount  개인 부담 합계 (정산 시 지급 대상)
 */
public record TripExpenseSummaryResponse(
        Long tripId,
        long expectedAmount,
        long totalAmount,
        long corporateAmount,
        long personalAmount,
        long count,
        List<CategoryAmount> byCategory) {
}
