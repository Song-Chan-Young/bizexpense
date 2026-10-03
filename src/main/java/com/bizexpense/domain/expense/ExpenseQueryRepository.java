package com.bizexpense.domain.expense;

import org.springframework.data.jpa.domain.Specification;

/** Specification 으로는 표현할 수 없는 집계 조회 */
public interface ExpenseQueryRepository {

    /** 검색 조건에 맞는 경비 금액 합계 (목록 화면 합계 행) */
    long sumAmount(Specification<Expense> spec);
}
