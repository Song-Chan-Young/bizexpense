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
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 출장 경비 1건. 출장이 시작된 뒤(진행중/완료)에만 등록할 수 있고,
 * 사용일은 출장 기간(사전 예매 허용 기간 포함) 안이어야 한다.
 */
@Getter
@Entity
@Table(name = "expense", indexes = {
        @Index(name = "idx_expense_user_used_at", columnList = "user_id, used_at"),
        @Index(name = "idx_expense_trip", columnList = "trip_id"),
        @Index(name = "idx_expense_status", columnList = "status")
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

    /** 증빙 첨부 여부. 영수증 파일(Phase 6)이 있으면 true */
    @Column(name = "proof_yn", nullable = false)
    private boolean proof;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ExpenseStatus status;

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
        requireEditable("수정");
        validate(trip, usedAt);
        this.trip = trip;
        this.category = category;
        this.paymentMethod = paymentMethod;
        this.usedAt = usedAt;
        this.storeName = storeName;
        this.amount = amount;
        this.description = description;
    }

    public void checkDeletable() {
        requireEditable("삭제");
    }

    public void markProof(boolean proof) {
        this.proof = proof;
    }

    public boolean isOwnedBy(Long userId) {
        return user.getId().equals(userId);
    }

    private void requireEditable(String action) {
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
