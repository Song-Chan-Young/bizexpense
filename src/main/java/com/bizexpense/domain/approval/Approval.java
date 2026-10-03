package com.bizexpense.domain.approval;

import com.bizexpense.domain.user.User;
import com.bizexpense.global.error.BusinessException;
import com.bizexpense.global.error.ErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.util.StringUtils;

/**
 * 결재 1건 = 신청 1회. 반려 후 재신청하면 새 결재 건이 생기므로
 * 대상(target_type, target_id) 별 결재 이력이 그대로 남는다.
 */
@Getter
@Entity
@Table(name = "approval", indexes = {
        @Index(name = "idx_approval_target", columnList = "target_type, target_id"),
        @Index(name = "idx_approval_approver_status", columnList = "approver_id, status")
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Approval {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "approval_id")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 20)
    private ApprovalTargetType targetType;

    @Column(name = "target_id", nullable = false)
    private Long targetId;

    /** 결재 목록에 보여줄 대상 제목 (신청 시점 기준) */
    @Column(nullable = false, length = 200)
    private String title;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requester_id", nullable = false)
    private User requester;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "approver_id", nullable = false)
    private User approver;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ApprovalStatus status;

    /** 결재 의견 / 반려 사유 */
    @Column(length = 1000)
    private String comment;

    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    public Approval(ApprovalTargetType targetType, Long targetId, String title, User requester, User approver) {
        this.targetType = targetType;
        this.targetId = targetId;
        this.title = title;
        this.requester = requester;
        this.approver = approver;
        this.status = ApprovalStatus.PENDING;
        this.requestedAt = LocalDateTime.now();
    }

    public void approve(String comment) {
        process(ApprovalStatus.APPROVED, comment);
    }

    public void reject(String comment) {
        if (!StringUtils.hasText(comment)) {
            throw new BusinessException(ErrorCode.REJECT_COMMENT_REQUIRED);
        }
        process(ApprovalStatus.REJECTED, comment);
    }

    public void cancel() {
        process(ApprovalStatus.CANCELLED, null);
    }

    public boolean isApprover(Long userId) {
        return approver.getId().equals(userId);
    }

    private void process(ApprovalStatus result, String comment) {
        if (status != ApprovalStatus.PENDING) {
            throw new BusinessException(ErrorCode.APPROVAL_ALREADY_PROCESSED);
        }
        this.status = result;
        this.comment = comment;
        this.processedAt = LocalDateTime.now();
    }
}
