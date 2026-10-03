package com.bizexpense.domain.schedule.dto;

import com.bizexpense.domain.schedule.ScheduleStatus;
import com.bizexpense.domain.schedule.ScheduleType;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

/** 내 일정 목록 검색 조건. from / to 는 둘 다 포함하는 날짜 범위다. */
public record ScheduleSearchCondition(
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
        ScheduleType type,
        ScheduleStatus status,
        String keyword) {
}
