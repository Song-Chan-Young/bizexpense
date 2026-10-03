package com.bizexpense.domain.trip.dto;

import com.bizexpense.domain.trip.TripStatus;
import com.bizexpense.global.common.ViewScope;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * 출장 목록 검색 조건.
 *
 * @param scope   ME(기본) / TEAM(팀장) / ALL(관리자)
 * @param keyword 출장 제목 또는 출장지
 * @param from    이 날짜 이후에 끝나는 출장 (기간 겹침)
 * @param to      이 날짜 이전에 시작하는 출장 (기간 겹침)
 */
public record TripSearchCondition(
        ViewScope scope,
        TripStatus status,
        String keyword,
        String userName,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

    public ViewScope scopeOrDefault() {
        return scope == null ? ViewScope.ME : scope;
    }
}
