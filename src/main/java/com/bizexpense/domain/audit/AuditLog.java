package com.bizexpense.domain.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 감사 로그 1건. 누가 / 언제 / 어디서(IP) / 무엇을(작업·대상) / 결과를 남긴다.
 * 사용자가 삭제되거나 이름이 바뀌어도 기록이 그대로 남도록 외래 키 없이 값으로 저장한다.
 */
@Getter
@Entity
@Table(name = "audit_log", indexes = {
        @Index(name = "idx_audit_created_at", columnList = "created_at"),
        @Index(name = "idx_audit_actor", columnList = "actor_id, created_at"),
        @Index(name = "idx_audit_target", columnList = "target_type, target_id")
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "audit_log_id")
    private Long id;

    /** 수행자. 로그인 실패처럼 사용자를 특정할 수 없으면 null */
    @Column(name = "actor_id")
    private Long actorId;

    @Column(name = "actor_login_id", length = 50)
    private String actorLoginId;

    @Column(name = "actor_name", length = 50)
    private String actorName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private AuditAction action;

    @Column(name = "target_type", nullable = false, length = 20)
    private String targetType;

    @Column(name = "target_id")
    private Long targetId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private AuditResult result;

    /** 실패했을 때 오류 코드 (예: ACCESS_DENIED) */
    @Column(name = "error_code", length = 50)
    private String errorCode;

    @Column(name = "http_method", nullable = false, length = 10)
    private String httpMethod;

    @Column(nullable = false, length = 300)
    private String uri;

    @Column(length = 64)
    private String ip;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Builder
    private AuditLog(Long actorId, String actorLoginId, String actorName, AuditAction action, Long targetId,
                     AuditResult result, String errorCode, String httpMethod, String uri, String ip) {
        this.actorId = actorId;
        this.actorLoginId = actorLoginId;
        this.actorName = actorName;
        this.action = action;
        this.targetType = action.getTargetType();
        this.targetId = targetId;
        this.result = result;
        this.errorCode = errorCode;
        this.httpMethod = httpMethod;
        this.uri = truncate(uri, 300);
        this.ip = truncate(ip, 64);
        this.createdAt = LocalDateTime.now();
    }

    private static String truncate(String value, int max) {
        return value == null || value.length() <= max ? value : value.substring(0, max);
    }
}
