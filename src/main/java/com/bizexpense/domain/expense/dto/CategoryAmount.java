package com.bizexpense.domain.expense.dto;

/** 비용 항목별 합계 집계 결과 (JPQL constructor expression) */
public record CategoryAmount(Long categoryId, String categoryName, Long count, Long amount) {
}
