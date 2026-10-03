package com.bizexpense.domain.trip;

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
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 출장. 상태 전이 규칙은 이 엔티티의 메서드에서만 바꾸도록 해서
 * 서비스 어디에서 호출하더라도 같은 규칙이 적용되게 한다.
 */
@Getter
@Entity
@Table(name = "trip", indexes = {
        @Index(name = "idx_trip_user_period", columnList = "user_id, start_date, end_date"),
        @Index(name = "idx_trip_status", columnList = "status")
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Trip extends BaseTimeEntity {

    /** 출장 시작 전 사전 예매 경비를 인정하는 일수 */
    public static final int PRE_BOOKING_DAYS = 30;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "trip_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 1000)
    private String purpose;

    @Column(nullable = false, length = 200)
    private String destination;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    /** 예상 경비 (원) */
    @Column(name = "expected_amount", nullable = false)
    private long expectedAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TripStatus status;

    @Builder
    private Trip(User user, String title, String purpose, String destination,
                 LocalDate startDate, LocalDate endDate, long expectedAmount) {
        validatePeriod(startDate, endDate);
        this.user = user;
        this.title = title;
        this.purpose = purpose;
        this.destination = destination;
        this.startDate = startDate;
        this.endDate = endDate;
        this.expectedAmount = expectedAmount;
        this.status = TripStatus.DRAFT;
    }

    public void update(String title, String purpose, String destination,
                       LocalDate startDate, LocalDate endDate, long expectedAmount) {
        requireStatus(status.isEditable(), "수정");
        validatePeriod(startDate, endDate);
        this.title = title;
        this.purpose = purpose;
        this.destination = destination;
        this.startDate = startDate;
        this.endDate = endDate;
        this.expectedAmount = expectedAmount;
    }

    /** 신청 / 반려 후 재신청 */
    public void request() {
        requireStatus(status.isEditable(), "신청");
        this.status = TripStatus.REQUESTED;
    }

    public void approve() {
        requireStatus(status == TripStatus.REQUESTED, "승인");
        this.status = TripStatus.APPROVED;
    }

    public void reject() {
        requireStatus(status == TripStatus.REQUESTED, "반려");
        this.status = TripStatus.REJECTED;
    }

    public void cancel() {
        requireStatus(status.isCancellable(), "취소");
        this.status = TripStatus.CANCELLED;
    }

    public void start() {
        requireStatus(status == TripStatus.APPROVED, "시작");
        this.status = TripStatus.IN_PROGRESS;
    }

    public void complete() {
        requireStatus(status == TripStatus.IN_PROGRESS, "완료");
        this.status = TripStatus.COMPLETED;
    }

    /** 삭제는 신청 전(임시저장)에만 가능하다. 그 이후에는 이력을 남기기 위해 취소만 허용한다. */
    public void checkDeletable() {
        requireStatus(status == TripStatus.DRAFT, "삭제");
    }

    public boolean isOwnedBy(Long userId) {
        return user.getId().equals(userId);
    }

    /** [startAt, endAt] 이 출장 기간(시작일 00:00 ~ 종료일 24:00) 안에 있는지 */
    public boolean covers(LocalDateTime startAt, LocalDateTime endAt) {
        return !startAt.isBefore(startDate.atStartOfDay())
                && !endAt.isAfter(endDate.plusDays(1).atStartOfDay());
    }

    /**
     * 경비 사용일로 인정하는 기간: 출장 시작 {@value #PRE_BOOKING_DAYS}일 전 ~ 종료일.
     * 교통편·숙소를 미리 예매하는 경우를 고려해 시작일 이전 사용분도 일정 기간 인정한다.
     */
    public boolean acceptsExpenseOn(LocalDate usedAt) {
        return !usedAt.isBefore(startDate.minusDays(PRE_BOOKING_DAYS)) && !usedAt.isAfter(endDate);
    }

    private void requireStatus(boolean allowed, String action) {
        if (!allowed) {
            throw new BusinessException(ErrorCode.INVALID_TRIP_STATUS,
                    "'" + status.getLabel() + "' 상태의 출장은 " + action + "할 수 없습니다.");
        }
    }

    private static void validatePeriod(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null || endDate.isBefore(startDate)) {
            throw new BusinessException(ErrorCode.INVALID_TRIP_PERIOD);
        }
    }
}
