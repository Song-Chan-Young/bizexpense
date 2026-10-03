package com.bizexpense.domain.schedule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bizexpense.global.error.BusinessException;
import com.bizexpense.global.error.ErrorCode;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class ScheduleTest {

    private static final LocalDateTime T10 = LocalDateTime.of(2026, 10, 12, 10, 0);
    private static final LocalDateTime T12 = LocalDateTime.of(2026, 10, 12, 12, 0);

    @Test
    void 새_일정은_예정_상태로_시작한다() {
        Schedule schedule = schedule(T10, T12);
        assertThat(schedule.getStatus()).isEqualTo(ScheduleStatus.PLANNED);
    }

    @Test
    void 종료가_시작보다_앞서면_생성할_수_없다() {
        assertThatThrownBy(() -> schedule(T12, T10))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_SCHEDULE_PERIOD);
    }

    @Test
    void 시작과_종료가_같으면_생성할_수_없다() {
        assertThatThrownBy(() -> schedule(T10, T10))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_SCHEDULE_PERIOD);
    }

    @Test
    void 수정할_때도_기간을_검증한다() {
        Schedule schedule = schedule(T10, T12);
        assertThatThrownBy(() -> schedule.update(ScheduleType.MEETING, "회의", null, T12, T10, null,
                ScheduleStatus.PLANNED))
                .isInstanceOf(BusinessException.class);
        // 실패한 수정은 기존 값을 바꾸지 않는다
        assertThat(schedule.getStartAt()).isEqualTo(T10);
    }

    private Schedule schedule(LocalDateTime start, LocalDateTime end) {
        return Schedule.builder()
                .type(ScheduleType.MEETING)
                .title("회의")
                .startAt(start)
                .endAt(end)
                .build();
    }
}
