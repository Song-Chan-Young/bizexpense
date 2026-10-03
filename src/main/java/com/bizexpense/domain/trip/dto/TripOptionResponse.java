package com.bizexpense.domain.trip.dto;

import com.bizexpense.domain.trip.Trip;
import com.bizexpense.domain.trip.TripStatus;
import java.time.LocalDate;

/** 일정 등록 화면의 출장 선택 목록 */
public record TripOptionResponse(Long tripId, String title, LocalDate startDate, LocalDate endDate,
                                 TripStatus status, String statusLabel) {

    public static TripOptionResponse from(Trip t) {
        return new TripOptionResponse(t.getId(), t.getTitle(), t.getStartDate(), t.getEndDate(),
                t.getStatus(), t.getStatus().getLabel());
    }
}
