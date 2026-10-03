package com.bizexpense.domain.schedule;

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
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 업무 일정. 출장(trip)에 연결할 수 있으며, 연결하면 출장 기간 안에 있어야 한다.
 */
@Getter
@Entity
@Table(name = "schedule", indexes = {
        // 사원별 기간 조회와 시간대 충돌 검사에 사용
        @Index(name = "idx_schedule_user_period", columnList = "user_id, start_at, end_at"),
        @Index(name = "idx_schedule_trip", columnList = "trip_id")
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Schedule extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "schedule_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** 연결된 출장 (선택) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id")
    private Trip trip;

    @Enumerated(EnumType.STRING)
    @Column(name = "schedule_type", nullable = false, length = 20)
    private ScheduleType type;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 2000)
    private String content;

    @Column(name = "start_at", nullable = false)
    private LocalDateTime startAt;

    @Column(name = "end_at", nullable = false)
    private LocalDateTime endAt;

    @Column(length = 200)
    private String location;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ScheduleStatus status;

    @Builder
    private Schedule(User user, ScheduleType type, String title, String content,
                     LocalDateTime startAt, LocalDateTime endAt, String location) {
        validatePeriod(startAt, endAt);
        this.user = user;
        this.type = type;
        this.title = title;
        this.content = content;
        this.startAt = startAt;
        this.endAt = endAt;
        this.location = location;
        this.status = ScheduleStatus.PLANNED;
    }

    public void update(ScheduleType type, String title, String content, LocalDateTime startAt,
                       LocalDateTime endAt, String location, ScheduleStatus status) {
        validatePeriod(startAt, endAt);
        this.type = type;
        this.title = title;
        this.content = content;
        this.startAt = startAt;
        this.endAt = endAt;
        this.location = location;
        this.status = status;
    }

    /**
     * 출장 연결/해제. 같은 사원의 출장이어야 하고, 일정 기간이 출장 기간 안에 있어야 한다.
     * 출장 상태(연결 가능 여부) 확인은 서비스에서 한다.
     */
    public void linkTrip(Trip trip) {
        if (trip != null) {
            if (!trip.isOwnedBy(user.getId())) {
                throw new BusinessException(ErrorCode.TRIP_NOT_LINKABLE, "본인 출장에만 일정을 연결할 수 있습니다.");
            }
            if (!trip.covers(startAt, endAt)) {
                throw new BusinessException(ErrorCode.SCHEDULE_OUT_OF_TRIP_PERIOD);
            }
        }
        this.trip = trip;
    }

    /** 출장이 취소되면 연결된 일정도 취소한다. */
    public void cancel() {
        this.status = ScheduleStatus.CANCELLED;
    }

    public boolean isOwnedBy(Long userId) {
        return user.getId().equals(userId);
    }

    private static void validatePeriod(LocalDateTime startAt, LocalDateTime endAt) {
        if (startAt == null || endAt == null || !endAt.isAfter(startAt)) {
            throw new BusinessException(ErrorCode.INVALID_SCHEDULE_PERIOD);
        }
    }
}
