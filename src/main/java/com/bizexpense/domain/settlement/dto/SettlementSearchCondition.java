package com.bizexpense.domain.settlement.dto;

import com.bizexpense.domain.settlement.SettlementStatus;
import com.bizexpense.global.common.ViewScope;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * 정산 목록 검색 조건.
 *
 * @param from / to 신청일 범위 (둘 다 포함)
 */
public record SettlementSearchCondition(
        ViewScope scope,
        SettlementStatus status,
        String userName,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

    public ViewScope scopeOrDefault() {
        return scope == null ? ViewScope.ME : scope;
    }
}
