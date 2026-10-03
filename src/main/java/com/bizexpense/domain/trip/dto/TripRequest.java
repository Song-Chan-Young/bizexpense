package com.bizexpense.domain.trip.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * 출장 등록/수정 요청.
 *
 * @param submit 등록과 동시에 신청할지 여부 (등록 시에만 사용). false/null 이면 임시저장.
 */
public record TripRequest(
        @NotBlank(message = "출장 제목을 입력하세요.") @Size(max = 200) String title,
        @Size(max = 1000) String purpose,
        @NotBlank(message = "출장지를 입력하세요.") @Size(max = 200) String destination,
        @NotNull(message = "시작일을 입력하세요.") LocalDate startDate,
        @NotNull(message = "종료일을 입력하세요.") LocalDate endDate,
        @NotNull(message = "예상 경비를 입력하세요.") @PositiveOrZero @Max(1_000_000_000) Long expectedAmount,
        Boolean submit) {

    public boolean submitNow() {
        return Boolean.TRUE.equals(submit);
    }
}
