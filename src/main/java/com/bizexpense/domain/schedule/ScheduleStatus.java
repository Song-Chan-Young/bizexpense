package com.bizexpense.domain.schedule;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ScheduleStatus {

    PLANNED("예정"),
    ONGOING("진행중"),
    DONE("완료"),
    CANCELLED("취소");

    private final String label;
}
