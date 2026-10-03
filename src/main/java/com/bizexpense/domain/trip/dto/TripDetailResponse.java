package com.bizexpense.domain.trip.dto;

import com.bizexpense.domain.approval.dto.ApprovalResponse;
import com.bizexpense.domain.schedule.dto.ScheduleResponse;
import java.util.List;

/** 출장 상세: 출장 정보 + 연결된 일정 + 결재 이력(반려 사유 포함) */
public record TripDetailResponse(
        TripResponse trip,
        List<ScheduleResponse> schedules,
        List<ApprovalResponse> approvals) {
}
