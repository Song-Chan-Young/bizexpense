package com.bizexpense.domain.expense.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record ExpenseRequest(
        @NotNull(message = "출장을 선택하세요.") Long tripId,
        @NotNull(message = "비용 항목을 선택하세요.") Long categoryId,
        @NotNull(message = "결제 수단을 선택하세요.") Long paymentMethodId,
        @NotNull(message = "사용일을 입력하세요.") LocalDate usedAt,
        @NotBlank(message = "사용처를 입력하세요.") @Size(max = 100) String storeName,
        @NotNull(message = "금액을 입력하세요.") @Positive(message = "금액은 0보다 커야 합니다.")
        @Max(value = 100_000_000, message = "금액은 1억 원 이하로 입력하세요.") Long amount,
        @Size(max = 1000) String description) {
}
