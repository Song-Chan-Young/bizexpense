package com.bizexpense.domain.trip;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bizexpense.global.error.BusinessException;
import com.bizexpense.global.error.ErrorCode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class TripTest {

    private static final LocalDate D12 = LocalDate.of(2026, 10, 12);
    private static final LocalDate D14 = LocalDate.of(2026, 10, 14);

    @Test
    void 정상_흐름_임시저장_신청_승인_시작_완료() {
        Trip trip = trip();
        assertThat(trip.getStatus()).isEqualTo(TripStatus.DRAFT);

        trip.request();
        assertThat(trip.getStatus()).isEqualTo(TripStatus.REQUESTED);
        trip.approve();
        assertThat(trip.getStatus()).isEqualTo(TripStatus.APPROVED);
        trip.start();
        assertThat(trip.getStatus()).isEqualTo(TripStatus.IN_PROGRESS);
        trip.complete();
        assertThat(trip.getStatus()).isEqualTo(TripStatus.COMPLETED);
    }

    @Test
    void 반려되면_수정_후_재신청할_수_있다() {
        Trip trip = trip();
        trip.request();
        trip.reject();

        trip.update("수정한 제목", null, "부산", D12, D14, 300_000);
        trip.request();

        assertThat(trip.getStatus()).isEqualTo(TripStatus.REQUESTED);
        assertThat(trip.getTitle()).isEqualTo("수정한 제목");
    }

    @Test
    void 신청된_출장은_수정할_수_없다() {
        Trip trip = trip();
        trip.request();

        assertInvalidStatus(() -> trip.update("변경", null, "부산", D12, D14, 0));
    }

    @Test
    void 승인된_출장은_수정할_수_없다() {
        Trip trip = trip();
        trip.request();
        trip.approve();

        assertInvalidStatus(() -> trip.update("변경", null, "부산", D12, D14, 0));
        assertInvalidStatus(trip::request);
    }

    @Test
    void 승인_전에는_출장을_시작할_수_없다() {
        Trip trip = trip();
        assertInvalidStatus(trip::start);
        trip.request();
        assertInvalidStatus(trip::start);
    }

    @Test
    void 진행중이거나_완료된_출장은_취소할_수_없다() {
        Trip trip = trip();
        trip.request();
        trip.approve();
        trip.start();
        assertInvalidStatus(trip::cancel);
    }

    @Test
    void 임시저장_건만_삭제할_수_있다() {
        Trip trip = trip();
        trip.checkDeletable();
        trip.request();
        assertInvalidStatus(trip::checkDeletable);
    }

    @Test
    void 종료일이_시작일보다_빠르면_생성할_수_없다() {
        assertThatThrownBy(() -> Trip.builder().title("t").destination("부산")
                .startDate(D14).endDate(D12).build())
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_TRIP_PERIOD);
    }

    @Test
    void 출장기간은_시작일_0시부터_종료일_24시까지다() {
        Trip trip = trip(); // 10/12 ~ 10/14
        assertThat(trip.covers(LocalDateTime.of(2026, 10, 12, 0, 0), LocalDateTime.of(2026, 10, 12, 9, 0))).isTrue();
        assertThat(trip.covers(LocalDateTime.of(2026, 10, 14, 22, 0), LocalDateTime.of(2026, 10, 15, 0, 0))).isTrue();
        assertThat(trip.covers(LocalDateTime.of(2026, 10, 11, 23, 0), LocalDateTime.of(2026, 10, 12, 1, 0))).isFalse();
        assertThat(trip.covers(LocalDateTime.of(2026, 10, 14, 23, 0), LocalDateTime.of(2026, 10, 15, 1, 0))).isFalse();
    }

    private Trip trip() {
        return Trip.builder()
                .title("부산 거래처 방문")
                .destination("부산")
                .startDate(D12)
                .endDate(D14)
                .expectedAmount(300_000)
                .build();
    }

    private void assertInvalidStatus(Runnable action) {
        assertThatThrownBy(action::run)
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_TRIP_STATUS);
    }
}
