package com.bizexpense.domain.audit.dto;

import com.bizexpense.domain.audit.AuditAction;
import com.bizexpense.domain.audit.AuditLog;
import com.bizexpense.domain.audit.AuditResult;
import java.time.LocalDateTime;

public record AuditLogResponse(
        Long auditLogId,
        Long actorId,
        String actorLoginId,
        String actorName,
        AuditAction action,
        String actionLabel,
        String targetType,
        Long targetId,
        AuditResult result,
        String errorCode,
        String httpMethod,
        String uri,
        String ip,
        LocalDateTime createdAt) {

    public static AuditLogResponse from(AuditLog a) {
        return new AuditLogResponse(a.getId(), a.getActorId(), a.getActorLoginId(), a.getActorName(),
                a.getAction(), a.getAction().getLabel(), a.getTargetType(), a.getTargetId(), a.getResult(),
                a.getErrorCode(), a.getHttpMethod(), a.getUri(), a.getIp(), a.getCreatedAt());
    }
}
