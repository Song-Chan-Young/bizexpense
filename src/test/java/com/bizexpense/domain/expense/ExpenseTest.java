package com.bizexpense.domain.expense;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bizexpense.domain.trip.Trip;
import com.bizexpense.global.error.BusinessException;
import com.bizexpense.global.error.ErrorCode;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class ExpenseTest {

    // 출장 기간 10/12 ~ 10/14
    private final Trip trip = Trip.builder()
            .title("부산 출장").destination("부산")
            .startDate(LocalDate.of(2026, 10, 12)).endDate(LocalDate.of(2026, 10, 14))
            .expectedAmount(300_000).build();

    @Test
    void 새_경비는_임시저장_상태이고_증빙이_없다() {
        Expense expense = expense(LocalDate.of(2026, 10, 12));
        assertThat(expense.getStatus()).isEqualTo(ExpenseStatus.DRAFT);
        assertThat(expense.isProof()).isFalse();
    }

    @Test
    void 출장_종료일_이후_사용분은_등록할_수_없다() {
        assertThatThrownBy(() -> expense(LocalDate.of(2026, 10, 15)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.EXPENSE_DATE_OUT_OF_TRIP);
    }

    @Test
    void 출장_시작_30일_전까지의_사전_예매분은_등록할_수_있다() {
        assertThat(expense(LocalDate.of(2026, 9, 12)).getUsedAt()).isEqualTo(LocalDate.of(2026, 9, 12));
        assertThatThrownBy(() -> expense(LocalDate.of(2026, 9, 11)))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void 수정할_때도_사용일을_검증한다() {
        Expense expense = expense(LocalDate.of(2026, 10, 12));
        assertThatThrownBy(() -> expense.update(trip, null, null, LocalDate.of(2026, 10, 20), "식당", 1000, null))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.EXPENSE_DATE_OUT_OF_TRIP);
    }

    private Expense expense(LocalDate usedAt) {
        return Expense.builder()
                .trip(trip)
                .usedAt(usedAt)
                .storeName("부산역 식당")
                .amount(12_000)
                .build();
    }
}
