package com.bizexpense.domain.expense.dto;

import com.bizexpense.domain.expense.ExpenseStatus;
import com.bizexpense.global.common.ViewScope;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * 경비 목록 검색 조건. 값이 있는 조건만 AND 로 적용한다.
 *
 * @param from / to   사용일 범위 (둘 다 포함)
 * @param minAmount / maxAmount 금액 범위 (둘 다 포함)
 * @param keyword     사용처 또는 내용
 */
public record ExpenseSearchCondition(
        ViewScope scope,
        Long tripId,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
        Long categoryId,
        Long paymentMethodId,
        ExpenseStatus status,
        Long minAmount,
        Long maxAmount,
        String keyword,
        String userName) {

    public ViewScope scopeOrDefault() {
        return scope == null ? ViewScope.ME : scope;
    }
}
