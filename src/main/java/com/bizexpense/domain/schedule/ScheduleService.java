package com.bizexpense.domain.schedule;

import com.bizexpense.domain.schedule.dto.CalendarScope;
import com.bizexpense.domain.schedule.dto.ScheduleConflictResponse;
import com.bizexpense.domain.schedule.dto.ScheduleRequest;
import com.bizexpense.domain.schedule.dto.ScheduleResponse;
import com.bizexpense.domain.schedule.dto.ScheduleSearchCondition;
import com.bizexpense.domain.user.User;
import com.bizexpense.domain.user.UserRepository;
import com.bizexpense.global.common.PageResponse;
import com.bizexpense.global.error.BusinessException;
import com.bizexpense.global.error.ErrorCode;
import com.bizexpense.global.security.LoginUser;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScheduleService {

    /** 캘린더 한 번에 조회할 수 있는 최대 기간 (주간/월간 화면 + 여유) */
    private static final int MAX_CALENDAR_DAYS = 62;

    private final ScheduleRepository scheduleRepository;
    private final UserRepository userRepository;

    public PageResponse<ScheduleResponse> search(LoginUser loginUser, ScheduleSearchCondition cond, Pageable pageable) {
        return PageResponse.of(
                scheduleRepository.findAll(ScheduleSpecs.search(loginUser.userId(), cond), pageable),
                s -> ScheduleResponse.of(s, loginUser.userId()));
    }

    /** 캘린더 조회. from 포함, to 미포함 날짜 범위. */
    public List<ScheduleResponse> calendar(LoginUser loginUser, LocalDate from, LocalDate to, CalendarScope scope) {
        if (from == null || to == null || !to.isAfter(from) || from.plusDays(MAX_CALENDAR_DAYS).isBefore(to)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT,
                    "조회 기간이 올바르지 않습니다. (최대 " + MAX_CALENDAR_DAYS + "일)");
        }

        Long userId = null;
        Long departmentId = null;
        switch (scope) {
            case ME -> userId = loginUser.userId();
            case TEAM -> {
                if (!loginUser.isManager() && !loginUser.isAdmin()) {
                    throw new BusinessException(ErrorCode.ACCESS_DENIED);
                }
                departmentId = loginUser.departmentId();
            }
            case ALL -> {
                if (!loginUser.isAdmin()) {
                    throw new BusinessException(ErrorCode.ACCESS_DENIED);
                }
            }
        }

        return scheduleRepository.findForCalendar(from.atStartOfDay(), to.atStartOfDay(), userId, departmentId)
                .stream()
                .map(s -> ScheduleResponse.of(s, loginUser.userId()))
                .toList();
    }

    public ScheduleResponse get(LoginUser loginUser, Long scheduleId) {
        Schedule schedule = findDetail(scheduleId);
        checkReadable(schedule, loginUser);
        return ScheduleResponse.of(schedule, loginUser.userId());
    }

    public List<ScheduleConflictResponse> findConflicts(LoginUser loginUser, LocalDateTime startAt,
                                                        LocalDateTime endAt, Long excludeId) {
        return scheduleRepository.findOverlapping(loginUser.userId(), startAt, endAt, excludeId).stream()
                .map(ScheduleConflictResponse::from)
                .toList();
    }

    @Transactional
    public ScheduleResponse create(LoginUser loginUser, ScheduleRequest request) {
        User user = userRepository.findById(loginUser.userId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Schedule schedule = Schedule.builder()
                .user(user)
                .type(request.type())
                .title(request.title())
                .content(request.content())
                .startAt(request.startAt())
                .endAt(request.endAt())
                .location(request.location())
                .build();

        checkConflict(loginUser.userId(), request, null);
        return ScheduleResponse.of(scheduleRepository.save(schedule), loginUser.userId());
    }

    @Transactional
    public ScheduleResponse update(LoginUser loginUser, Long scheduleId, ScheduleRequest request) {
        Schedule schedule = findDetail(scheduleId);
        checkOwner(schedule, loginUser);

        ScheduleStatus status = request.status() != null ? request.status() : schedule.getStatus();
        schedule.update(request.type(), request.title(), request.content(),
                request.startAt(), request.endAt(), request.location(), status);

        // 취소로 바꾸는 경우에는 시간을 차지하지 않으므로 충돌 검사를 하지 않는다.
        if (status != ScheduleStatus.CANCELLED) {
            checkConflict(loginUser.userId(), request, scheduleId);
        }
        return ScheduleResponse.of(schedule, loginUser.userId());
    }

    @Transactional
    public void delete(LoginUser loginUser, Long scheduleId) {
        Schedule schedule = findDetail(scheduleId);
        checkOwner(schedule, loginUser);
        scheduleRepository.delete(schedule);
    }

    private Schedule findDetail(Long scheduleId) {
        return scheduleRepository.findDetailById(scheduleId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SCHEDULE_NOT_FOUND));
    }

    /**
     * 같은 사원이 같은 시간대에 다른 일정을 갖고 있으면 409 로 경고한다.
     * 사용자가 확인 후 allowOverlap=true 로 다시 요청하면 저장을 허용한다.
     */
    private void checkConflict(Long userId, ScheduleRequest request, Long excludeId) {
        if (request.overlapAllowed()) {
            return;
        }
        List<Schedule> overlapping = scheduleRepository.findOverlapping(
                userId, request.startAt(), request.endAt(), excludeId);
        if (!overlapping.isEmpty()) {
            String titles = overlapping.stream().map(Schedule::getTitle).collect(Collectors.joining(", "));
            throw new BusinessException(ErrorCode.SCHEDULE_CONFLICT,
                    ErrorCode.SCHEDULE_CONFLICT.getMessage() + " (" + titles + ")");
        }
    }

    /** 본인, 같은 부서 팀장, 관리자만 조회 가능 */
    private void checkReadable(Schedule schedule, LoginUser loginUser) {
        if (schedule.isOwnedBy(loginUser.userId()) || loginUser.isAdmin()) {
            return;
        }
        Long ownerDeptId = schedule.getUser().getDepartment() == null ? null : schedule.getUser().getDepartment().getId();
        if (loginUser.isManager() && ownerDeptId != null && Objects.equals(ownerDeptId, loginUser.departmentId())) {
            return;
        }
        throw new BusinessException(ErrorCode.ACCESS_DENIED);
    }

    /** 수정/삭제는 본인 일정만 가능 */
    private void checkOwner(Schedule schedule, LoginUser loginUser) {
        if (!schedule.isOwnedBy(loginUser.userId())) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
    }
}
