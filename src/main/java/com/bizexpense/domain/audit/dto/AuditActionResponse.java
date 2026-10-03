package com.bizexpense.domain.audit.dto;

import com.bizexpense.domain.audit.AuditAction;

/** 검색 화면의 작업 선택 목록 */
public record AuditActionResponse(AuditAction value, String label, String targetType) {

    public static AuditActionResponse from(AuditAction a) {
        return new AuditActionResponse(a, a.getLabel(), a.getTargetType());
    }
}
