package com.bizexpense.domain.trip.dto;

import com.bizexpense.domain.department.Department;
import com.bizexpense.domain.trip.Trip;
import com.bizexpense.domain.trip.TripStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * @param days    출장 일수 (당일 출장 = 1)
 * @param actions 조회한 사용자가 지금 할 수 있는 작업. 화면 버튼 표시용이며 실제 검사는 서버에서 다시 한다.
 */
public record TripResponse(
        Long tripId,
        Long userId,
        String userName,
        String departmentName,
        String title,
        String purpose,
        String destination,
        LocalDate startDate,
        LocalDate endDate,
        long days,
        long expectedAmount,
        TripStatus status,
        String statusLabel,
        Actions actions,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public static TripResponse of(Trip t, Long viewerId) {
        Department dept = t.getUser().getDepartment();
        return new TripResponse(
                t.getId(),
                t.getUser().getId(),
                t.getUser().getName(),
                dept == null ? null : dept.getName(),
                t.getTitle(),
                t.getPurpose(),
                t.getDestination(),
                t.getStartDate(),
                t.getEndDate(),
                ChronoUnit.DAYS.between(t.getStartDate(), t.getEndDate()) + 1,
                t.getExpectedAmount(),
                t.getStatus(),
                t.getStatus().getLabel(),
                Actions.of(t, viewerId),
                t.getCreatedAt(),
                t.getUpdatedAt());
    }

    public record Actions(boolean edit, boolean request, boolean cancel, boolean start,
                          boolean complete, boolean delete, boolean addSchedule) {

        static Actions of(Trip t, Long viewerId) {
            if (!t.isOwnedBy(viewerId)) {
                return new Actions(false, false, false, false, false, false, false);
            }
            TripStatus s = t.getStatus();
            return new Actions(
                    s.isEditable(),
                    s.isEditable(),
                    s.isCancellable(),
                    s == TripStatus.APPROVED,
                    s == TripStatus.IN_PROGRESS,
                    s == TripStatus.DRAFT,
                    s.isSchedulable());
        }
    }
}
