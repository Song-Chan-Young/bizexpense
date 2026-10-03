package com.bizexpense.domain.schedule.dto;

import com.bizexpense.domain.schedule.Schedule;
import java.time.LocalDateTime;

/** 시간대가 겹치는 기존 일정 요약 */
public record ScheduleConflictResponse(Long scheduleId, String title, LocalDateTime startAt, LocalDateTime endAt) {

    public static ScheduleConflictResponse from(Schedule s) {
        return new ScheduleConflictResponse(s.getId(), s.getTitle(), s.getStartAt(), s.getEndAt());
    }
}
