package com.bizexpense.domain.approval.dto;

import com.bizexpense.domain.approval.Approval;
import com.bizexpense.domain.approval.ApprovalStatus;
import com.bizexpense.domain.approval.ApprovalTargetType;
import com.bizexpense.domain.department.Department;
import java.time.LocalDateTime;

/**
 * @param processable 조회한 사용자가 지금 승인/반려할 수 있는지 (지정된 결재자 + 대기 상태)
 */
public record ApprovalResponse(
        Long approvalId,
        ApprovalTargetType targetType,
        String targetTypeLabel,
        Long targetId,
        String title,
        Long requesterId,
        String requesterName,
        String requesterDepartmentName,
        Long approverId,
        String approverName,
        ApprovalStatus status,
        String statusLabel,
        String comment,
        LocalDateTime requestedAt,
        LocalDateTime processedAt,
        boolean processable) {

    public static ApprovalResponse of(Approval a, Long viewerId) {
        Department dept = a.getRequester().getDepartment();
        return new ApprovalResponse(
                a.getId(),
                a.getTargetType(),
                a.getTargetType().getLabel(),
                a.getTargetId(),
                a.getTitle(),
                a.getRequester().getId(),
                a.getRequester().getName(),
                dept == null ? null : dept.getName(),
                a.getApprover().getId(),
                a.getApprover().getName(),
                a.getStatus(),
                a.getStatus().getLabel(),
                a.getComment(),
                a.getRequestedAt(),
                a.getProcessedAt(),
                a.getStatus() == ApprovalStatus.PENDING && a.isApprover(viewerId));
    }
}
