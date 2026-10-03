package com.bizexpense.domain.expense;

import com.bizexpense.domain.code.ExpenseCategory;
import com.bizexpense.domain.code.PaymentMethod;
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
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;

/**
 * 출장 경비 1건. 출장이 시작된 뒤(진행중/완료)에만 등록할 수 있고,
 * 사용일은 출장 기간(사전 예매 허용 기간 포함) 안이어야 한다.
 */
@Getter
@Entity
@Table(name = "expense", indexes = {
        @Index(name = "idx_expense_user_used_at", columnList = "user_id, used_at"),
        @Index(name = "idx_expense_trip", columnList = "trip_id"),
        @Index(name = "idx_expense_status", columnList = "status"),
        @Index(name = "idx_expense_settlement", columnList = "settlement_id")
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Expense extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "expense_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "trip_id", nullable = false)
    private Trip trip;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private ExpenseCategory category;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payment_method_id", nullable = false)
    private PaymentMethod paymentMethod;

    @Column(name = "used_at", nullable = false)
    private LocalDate usedAt;

    /** 상호 / 사용처 */
    @Column(name = "store_name", nullable = false, length = 100)
    private String storeName;

    /** 금액 (원) */
    @Column(nullable = false)
    private long amount;

    @Column(length = 1000)
    private String description;

    /** 증빙 첨부 여부. 영수증 파일({@link ExpenseFile})이 1개 이상이면 true */
    @Column(name = "proof_yn", nullable = false)
    private boolean proof;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ExpenseStatus status;

    /**
     * 포함된 정산. 한 경비는 한 정산에만 들어갈 수 있다 (중복 정산 방지).
     * 정산 → 경비 방향의 패키지 의존을 피하려고 연관관계 대신 ID 로 둔다.
     */
    @Column(name = "settlement_id")
    private Long settlementId;

    /** 같은 경비를 동시에 두 정산에 넣거나 수정하는 것을 막는 낙관적 락 */
    @Version
    @ColumnDefault("0")
    private Long version;

    @Builder
    private Expense(Trip trip, User user, ExpenseCategory category, PaymentMethod paymentMethod,
                    LocalDate usedAt, String storeName, long amount, String description) {
        validate(trip, usedAt);
        this.trip = trip;
        this.user = user;
        this.category = category;
        this.paymentMethod = paymentMethod;
        this.usedAt = usedAt;
        this.storeName = storeName;
        this.amount = amount;
        this.description = description;
        this.status = ExpenseStatus.DRAFT;
    }

    public void update(Trip trip, ExpenseCategory category, PaymentMethod paymentMethod,
                       LocalDate usedAt, String storeName, long amount, String description) {
        checkEditable("수정");
        validate(trip, usedAt);
        if (!this.trip.getId().equals(trip.getId())) {
            // 다른 출장으로 옮기면 기존 정산과의 연결을 끊고 새 경비처럼 다룬다
            this.settlementId = null;
            this.status = ExpenseStatus.DRAFT;
        }
        this.trip = trip;
        this.category = category;
        this.paymentMethod = paymentMethod;
        this.usedAt = usedAt;
        this.storeName = storeName;
        this.amount = amount;
        this.description = description;
    }

    public void checkDeletable() {
        checkEditable("삭제");
    }

    // ---------- 정산 흐름에 따른 상태 변경 ----------

    /**
     * 정산 신청에 포함: 아직 정산에 들어가지 않은 임시저장 경비이거나,
     * 같은 정산에서 반려되어 수정한 경비만 가능하다.
     */
    public void requestSettlement(Long settlementId) {
        boolean newExpense = status == ExpenseStatus.DRAFT && this.settlementId == null;
        boolean resubmit = status == ExpenseStatus.REJECTED && settlementId.equals(this.settlementId);
        if (!newExpense && !resubmit) {
            throw new BusinessException(ErrorCode.INVALID_EXPENSE_STATUS,
                    "'" + status.getLabel() + "' 상태의 경비는 정산에 포함할 수 없습니다.");
        }
        this.settlementId = settlementId;
        this.status = ExpenseStatus.REQUESTED;
    }

    public void approveSettlement() {
        changeStatus(ExpenseStatus.REQUESTED, ExpenseStatus.APPROVED);
    }

    public void rejectSettlement() {
        changeStatus(ExpenseStatus.REQUESTED, ExpenseStatus.REJECTED);
    }

    public void settle() {
        changeStatus(ExpenseStatus.APPROVED, ExpenseStatus.SETTLED);
    }

    public boolean isCorporatePaid() {
        return paymentMethod.isCorporate();
    }

    private void changeStatus(ExpenseStatus from, ExpenseStatus to) {
        if (status != from) {
            throw new BusinessException(ErrorCode.INVALID_EXPENSE_STATUS,
                    "'" + status.getLabel() + "' 상태의 경비는 '" + to.getLabel() + "'(으)로 바꿀 수 없습니다.");
        }
        this.status = to;
    }

    public void markProof(boolean proof) {
        this.proof = proof;
    }

    public boolean isOwnedBy(Long userId) {
        return user.getId().equals(userId);
    }

    public void checkEditable(String action) {
        if (!status.isEditable()) {
            throw new BusinessException(ErrorCode.INVALID_EXPENSE_STATUS,
                    "'" + status.getLabel() + "' 상태의 경비는 " + action + "할 수 없습니다.");
        }
    }

    private static void validate(Trip trip, LocalDate usedAt) {
        if (!trip.acceptsExpenseOn(usedAt)) {
            throw new BusinessException(ErrorCode.EXPENSE_DATE_OUT_OF_TRIP,
                    "사용일은 출장 시작 " + Trip.PRE_BOOKING_DAYS + "일 전부터 출장 종료일까지만 등록할 수 있습니다.");
        }
    }
}
