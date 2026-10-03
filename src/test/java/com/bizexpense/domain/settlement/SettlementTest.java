package com.bizexpense.domain.settlement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bizexpense.domain.code.PaymentMethod;
import com.bizexpense.domain.expense.Expense;
import com.bizexpense.domain.trip.Trip;
import com.bizexpense.global.error.BusinessException;
import com.bizexpense.global.error.ErrorCode;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class SettlementTest {

    private final Trip trip = Trip.builder().title("부산 출장").destination("부산")
            .startDate(LocalDate.of(2026, 10, 12)).endDate(LocalDate.of(2026, 10, 14)).build();
    private final PaymentMethod corporateCard = new PaymentMethod("법인카드", true);
    private final PaymentMethod personalCard = new PaymentMethod("개인카드", false);
    private final PaymentMethod cash = new PaymentMethod("현금", false);

    @Test
    void 설계서_예시_총경비_153000_법인카드_63000_이면_개인지급_90000() {
        SettlementAmounts amounts = SettlementAmounts.of(List.of(
                expense(corporateCard, 63_000),
                expense(personalCard, 60_000),
                expense(cash, 30_000)));

        assertThat(amounts.totalAmount()).isEqualTo(153_000);
        assertThat(amounts.corporateAmount()).isEqualTo(63_000);
        assertThat(amounts.personalAmount()).isEqualTo(90_000);
        assertThat(amounts.payableAmount()).isEqualTo(90_000);
        assertThat(amounts.expenseCount()).isEqualTo(3);
    }

    @Test
    void 모두_법인카드면_지급액은_0원() {
        SettlementAmounts amounts = SettlementAmounts.of(List.of(expense(corporateCard, 50_000)));
        assertThat(amounts.payableAmount()).isZero();
    }

    @Test
    void 경비가_없으면_정산을_만들_수_없다() {
        assertThatThrownBy(() -> new Settlement(trip, null, SettlementAmounts.of(List.of())))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.NO_EXPENSES_TO_SETTLE);
    }

    @Test
    void 신청_승인_지급완료() {
        Settlement s = settlement();
        assertThat(s.getStatus()).isEqualTo(SettlementStatus.REQUESTED);
        s.approve();
        assertThat(s.getApprovedAt()).isNotNull();
        s.complete();
        assertThat(s.getStatus()).isEqualTo(SettlementStatus.SETTLED);
        assertThat(s.getSettledAt()).isNotNull();
    }

    @Test
    void 반려되면_금액을_다시_계산해_재신청한다() {
        Settlement s = settlement();
        s.reject();
        s.resubmit(SettlementAmounts.of(List.of(expense(personalCard, 70_000))));

        assertThat(s.getStatus()).isEqualTo(SettlementStatus.REQUESTED);
        assertThat(s.getPayableAmount()).isEqualTo(70_000);
    }

    @Test
    void 승인_전에는_지급완료할_수_없고_반려_전에는_재신청할_수_없다() {
        Settlement s = settlement();
        assertThatThrownBy(s::complete).extracting("errorCode").isEqualTo(ErrorCode.INVALID_SETTLEMENT_STATUS);
        assertThatThrownBy(() -> s.resubmit(SettlementAmounts.of(List.of(expense(cash, 1)))))
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_SETTLEMENT_STATUS);
    }

    private Settlement settlement() {
        return new Settlement(trip, null, SettlementAmounts.of(List.of(expense(personalCard, 10_000))));
    }

    private Expense expense(PaymentMethod method, long amount) {
        return Expense.builder().trip(trip).paymentMethod(method)
                .usedAt(LocalDate.of(2026, 10, 12)).storeName("사용처").amount(amount).build();
    }
}
