package com.bizexpense.domain.settlement.dto;

import com.bizexpense.domain.department.Department;
import com.bizexpense.domain.settlement.Settlement;
import com.bizexpense.domain.settlement.SettlementStatus;
import com.bizexpense.global.security.LoginUser;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * @param actions 조회한 사용자가 지금 할 수 있는 작업 (화면 버튼용, 실제 검사는 서버에서 다시 한다)
 */
public record SettlementResponse(
        Long settlementId,
        Long tripId,
        String tripTitle,
        LocalDate tripStartDate,
        LocalDate tripEndDate,
        Long userId,
        String userName,
        String departmentName,
        long totalAmount,
        long corporateAmount,
        long personalAmount,
        long payableAmount,
        int expenseCount,
        SettlementStatus status,
        String statusLabel,
        LocalDateTime requestedAt,
        LocalDateTime approvedAt,
        LocalDateTime settledAt,
        Actions actions) {

    public static SettlementResponse of(Settlement s, LoginUser viewer) {
        Department dept = s.getUser().getDepartment();
        return new SettlementResponse(
                s.getId(),
                s.getTrip().getId(),
                s.getTrip().getTitle(),
                s.getTrip().getStartDate(),
                s.getTrip().getEndDate(),
                s.getUser().getId(),
                s.getUser().getName(),
                dept == null ? null : dept.getName(),
                s.getTotalAmount(),
                s.getCorporateAmount(),
                s.getPersonalAmount(),
                s.getPayableAmount(),
                s.getExpenseCount(),
                s.getStatus(),
                s.getStatus().getLabel(),
                s.getRequestedAt(),
                s.getApprovedAt(),
                s.getSettledAt(),
                new Actions(
                        s.isOwnedBy(viewer.userId()) && s.getStatus() == SettlementStatus.REJECTED,
                        viewer.isAdmin() && s.getStatus() == SettlementStatus.APPROVED));
    }

    /**
     * @param resubmit 신청자: 반려 건 재신청
     * @param complete 관리자: 승인 건 지급 완료 처리
     */
    public record Actions(boolean resubmit, boolean complete) {
    }
}
