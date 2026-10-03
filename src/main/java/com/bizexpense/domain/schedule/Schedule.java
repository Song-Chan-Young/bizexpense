package com.bizexpense.domain.schedule;

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
 * 업무 일정. 출장 연결(trip_id)은 출장 도메인(Phase 3)을 만들 때 추가한다.
 */
@Getter
@Entity
@Table(name = "schedule", indexes = {
        // 사원별 기간 조회와 시간대 충돌 검사에 사용
        @Index(name = "idx_schedule_user_period", columnList = "user_id, start_at, end_at")
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

    public boolean isOwnedBy(Long userId) {
        return user.getId().equals(userId);
    }

    private static void validatePeriod(LocalDateTime startAt, LocalDateTime endAt) {
        if (startAt == null || endAt == null || !endAt.isAfter(startAt)) {
            throw new BusinessException(ErrorCode.INVALID_SCHEDULE_PERIOD);
        }
    }
}
