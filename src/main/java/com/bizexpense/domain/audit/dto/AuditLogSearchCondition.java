package com.bizexpense.domain.audit.dto;

import com.bizexpense.domain.audit.AuditAction;
import com.bizexpense.domain.audit.AuditResult;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * 감사 로그 검색 조건.
 *
 * @param actor      수행자 이름 또는 아이디 (부분 일치)
 * @param targetType 대상 종류 (TRIP, EXPENSE ...)
 * @param from / to  기록일 범위 (둘 다 포함)
 */
public record AuditLogSearchCondition(
        String actor,
        AuditAction action,
        String targetType,
        AuditResult result,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
}
