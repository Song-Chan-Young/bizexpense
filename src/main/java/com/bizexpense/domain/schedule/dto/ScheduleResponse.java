package com.bizexpense.domain.schedule.dto;

import com.bizexpense.domain.schedule.Schedule;
import com.bizexpense.domain.schedule.ScheduleStatus;
import com.bizexpense.domain.schedule.ScheduleType;
import java.time.LocalDateTime;

/**
 * @param editable 조회한 사용자가 수정/삭제할 수 있는지 (본인 일정만 가능)
 */
public record ScheduleResponse(
        Long scheduleId,
        Long userId,
        String userName,
        ScheduleType type,
        String typeLabel,
        String title,
        String content,
        LocalDateTime startAt,
        LocalDateTime endAt,
        String location,
        ScheduleStatus status,
        String statusLabel,
        boolean editable,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public static ScheduleResponse of(Schedule s, Long viewerId) {
        return new ScheduleResponse(
                s.getId(),
                s.getUser().getId(),
                s.getUser().getName(),
                s.getType(),
                s.getType().getLabel(),
                s.getTitle(),
                s.getContent(),
                s.getStartAt(),
                s.getEndAt(),
                s.getLocation(),
                s.getStatus(),
                s.getStatus().getLabel(),
                s.isOwnedBy(viewerId),
                s.getCreatedAt(),
                s.getUpdatedAt());
    }
}
