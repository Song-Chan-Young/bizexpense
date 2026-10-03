package com.bizexpense.domain.schedule;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ScheduleType {

    TRIP("출장"),
    MEETING("회의"),
    OUTSIDE_WORK("외근"),
    CLIENT_VISIT("고객 방문"),
    TRAINING("교육"),
    PERSONAL("개인 업무"),
    ETC("기타");

    private final String label;
}
