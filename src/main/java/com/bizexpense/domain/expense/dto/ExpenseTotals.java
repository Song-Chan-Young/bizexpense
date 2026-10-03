package com.bizexpense.domain.expense.dto;

/** 경비 합계 집계 결과: 전체 / 법인 결제분 / 건수 (JPQL constructor expression) */
public record ExpenseTotals(Long totalAmount, Long corporateAmount, Long count) {
}
