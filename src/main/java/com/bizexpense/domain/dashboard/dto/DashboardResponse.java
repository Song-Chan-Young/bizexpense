package com.bizexpense.domain.dashboard.dto;

import com.bizexpense.domain.expense.dto.CategoryAmount;
import com.bizexpense.global.common.ViewScope;
import java.util.List;

/**
 * 역할별 대시보드 통계.
 *
 * @param scope      집계 범위 (직원: 본인, 팀장: 부서, 관리자: 전체)
 * @param cards      숫자 카드 4개
 * @param monthly    최근 6개월 경비 추이 (오래된 달부터)
 * @param categories 이번 달 비용 항목별 경비 (금액 큰 순)
 */
public record DashboardResponse(
        ViewScope scope,
        List<StatCard> cards,
        List<MonthAmount> monthly,
        List<CategoryAmount> categories) {
}
