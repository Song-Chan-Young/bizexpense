package com.bizexpense.domain.schedule.dto;

import com.bizexpense.domain.schedule.ScheduleStatus;
import com.bizexpense.domain.schedule.ScheduleType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

/**
 * 일정 등록/수정 요청.
 *
 * @param tripId       연결할 출장 (선택). null 이면 연결하지 않는다(수정 시에는 연결 해제).
 * @param status       수정 시에만 사용. 등록 시에는 무시하고 PLANNED 로 시작한다.
 * @param allowOverlap 시간대가 겹치는 일정이 있어도 저장할지 여부.
 *                     false 면 겹칠 때 409 SCHEDULE_CONFLICT 로 응답해 사용자에게 경고한다.
 */
public record ScheduleRequest(
        @NotNull(message = "일정 종류를 선택하세요.") ScheduleType type,
        @NotBlank(message = "제목을 입력하세요.") @Size(max = 200) String title,
        @Size(max = 2000) String content,
        @NotNull(message = "시작 일시를 입력하세요.") LocalDateTime startAt,
        @NotNull(message = "종료 일시를 입력하세요.") LocalDateTime endAt,
        @Size(max = 200) String location,
        Long tripId,
        ScheduleStatus status,
        Boolean allowOverlap) {

    /** 생략하면 false (겹치면 경고) */
    public boolean overlapAllowed() {
        return Boolean.TRUE.equals(allowOverlap);
    }
}
