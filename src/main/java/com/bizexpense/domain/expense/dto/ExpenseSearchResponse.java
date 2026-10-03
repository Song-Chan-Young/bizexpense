package com.bizexpense.domain.expense.dto;

import com.bizexpense.global.common.PageResponse;

/** 경비 목록 + 검색 조건 전체(모든 페이지)의 금액 합계 */
public record ExpenseSearchResponse(PageResponse<ExpenseResponse> page, long totalAmount) {
}
