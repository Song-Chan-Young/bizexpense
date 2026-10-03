package com.bizexpense.domain.schedule.dto;

/** 캘린더 조회 범위 */
public enum CalendarScope {
    /** 본인 일정 */
    ME,
    /** 같은 부서 일정 (팀장, 관리자) */
    TEAM,
    /** 전체 일정 (관리자) */
    ALL
}
