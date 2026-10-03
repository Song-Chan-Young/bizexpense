package com.bizexpense.domain.settlement;

import com.bizexpense.domain.trip.Trip;
import com.bizexpense.domain.user.User;
import com.bizexpense.global.common.BaseTimeEntity;
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
import jakarta.persistence.Version;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;

/**
 * 출장 1건에 대한 정산 신청. 포함된 경비는 expense.settlement_id 로 연결된다.
 * 금액은 신청(재신청) 시점의 경비로 계산해 저장한다.
 */
@Getter
@Entity
@Table(name = "settlement", indexes = {
        @Index(name = "idx_settlement_trip", columnList = "trip_id"),
        @Index(name = "idx_settlement_user_status", columnList = "user_id, status")
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Settlement extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "settlement_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "trip_id", nullable = false)
    private Trip trip;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "total_amount", nullable = false)
    private long totalAmount;

    @Column(name = "corporate_card_amount", nullable = false)
    private long corporateAmount;

    @Column(name = "personal_amount", nullable = false)
    private long personalAmount;

    @Column(name = "payable_amount", nullable = false)
    private long payableAmount;

    @Column(name = "expense_count", nullable = false)
    private int expenseCount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SettlementStatus status;

    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "settled_at")
    private LocalDateTime settledAt;

    /** 승인과 지급 처리 등이 동시에 일어나지 않도록 */
    @Version
    @ColumnDefault("0")
    private Long version;

    public Settlement(Trip trip, User user, SettlementAmounts amounts) {
        this.trip = trip;
        this.user = user;
        applyAmounts(amounts);
        this.status = SettlementStatus.REQUESTED;
        this.requestedAt = LocalDateTime.now();
    }

    /** 반려 후 경비를 고쳐 다시 신청: 금액을 새로 계산한다 */
    public void resubmit(SettlementAmounts amounts) {
        require(SettlementStatus.REJECTED, "재신청");
        applyAmounts(amounts);
        this.status = SettlementStatus.REQUESTED;
        this.requestedAt = LocalDateTime.now();
    }

    public void approve() {
        require(SettlementStatus.REQUESTED, "승인");
        this.status = SettlementStatus.APPROVED;
        this.approvedAt = LocalDateTime.now();
    }

    public void reject() {
        require(SettlementStatus.REQUESTED, "반려");
        this.status = SettlementStatus.REJECTED;
    }

    /** 지급 완료 처리 */
    public void complete() {
        require(SettlementStatus.APPROVED, "정산 완료 처리");
        this.status = SettlementStatus.SETTLED;
        this.settledAt = LocalDateTime.now();
    }

    public boolean isOwnedBy(Long userId) {
        return user.getId().equals(userId);
    }

    private void applyAmounts(SettlementAmounts amounts) {
        if (amounts.expenseCount() == 0) {
            throw new BusinessException(ErrorCode.NO_EXPENSES_TO_SETTLE);
        }
        this.totalAmount = amounts.totalAmount();
        this.corporateAmount = amounts.corporateAmount();
        this.personalAmount = amounts.personalAmount();
        this.payableAmount = amounts.payableAmount();
        this.expenseCount = amounts.expenseCount();
    }

    private void require(SettlementStatus expected, String action) {
        if (status != expected) {
            throw new BusinessException(ErrorCode.INVALID_SETTLEMENT_STATUS,
                    "'" + status.getLabel() + "' 상태의 정산은 " + action + "할 수 없습니다.");
        }
    }
}
