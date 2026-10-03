package com.bizexpense.domain.trip;

import com.bizexpense.domain.approval.ApprovalService;
import com.bizexpense.domain.approval.ApprovalTargetType;
import com.bizexpense.domain.schedule.Schedule;
import com.bizexpense.domain.schedule.ScheduleRepository;
import com.bizexpense.domain.schedule.ScheduleStatus;
import com.bizexpense.domain.schedule.dto.ScheduleResponse;
import com.bizexpense.domain.trip.dto.TripDetailResponse;
import com.bizexpense.domain.trip.dto.TripOptionResponse;
import com.bizexpense.domain.trip.dto.TripRequest;
import com.bizexpense.domain.trip.dto.TripResponse;
import com.bizexpense.domain.trip.dto.TripSearchCondition;
import com.bizexpense.domain.user.AccessPolicy;
import com.bizexpense.domain.user.User;
import com.bizexpense.domain.user.UserRepository;
import com.bizexpense.global.common.PageResponse;
import com.bizexpense.global.error.BusinessException;
import com.bizexpense.global.error.ErrorCode;
import com.bizexpense.global.security.LoginUser;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TripService {

    private final TripRepository tripRepository;
    private final UserRepository userRepository;
    private final ScheduleRepository scheduleRepository;
    private final ApprovalService approvalService;

    public PageResponse<TripResponse> search(LoginUser loginUser, TripSearchCondition cond, Pageable pageable) {
        cond.scopeOrDefault().checkAllowed(loginUser);
        return PageResponse.of(
                tripRepository.findAll(TripSpecs.search(loginUser, cond), pageable),
                t -> TripResponse.of(t, loginUser.userId()));
    }

    public TripDetailResponse get(LoginUser loginUser, Long tripId) {
        Trip trip = findDetail(tripId);
        checkReadable(trip, loginUser);
        List<ScheduleResponse> schedules = scheduleRepository.findByTripIdOrderByStartAtAsc(tripId).stream()
                .map(s -> ScheduleResponse.of(s, loginUser.userId()))
                .toList();
        return new TripDetailResponse(
                TripResponse.of(trip, loginUser.userId()),
                schedules,
                approvalService.history(ApprovalTargetType.TRIP, tripId, loginUser.userId()));
    }

    /** 일정을 연결할 수 있는 내 출장 */
    public List<TripOptionResponse> schedulableTrips(LoginUser loginUser) {
        List<TripStatus> statuses = Arrays.stream(TripStatus.values()).filter(TripStatus::isSchedulable).toList();
        return tripRepository.findByUserIdAndStatusInOrderByStartDateDesc(loginUser.userId(), statuses).stream()
                .map(TripOptionResponse::from)
                .toList();
    }

    /** 경비를 등록할 수 있는 내 출장 (진행중, 완료) */
    public List<TripOptionResponse> expensableTrips(LoginUser loginUser) {
        List<TripStatus> statuses = Arrays.stream(TripStatus.values()).filter(TripStatus::isExpensable).toList();
        return tripRepository.findByUserIdAndStatusInOrderByStartDateDesc(loginUser.userId(), statuses).stream()
                .map(TripOptionResponse::from)
                .toList();
    }

    /** 등록 (임시저장). submit=true 면 같은 트랜잭션에서 바로 신청까지 한다. */
    @Transactional
    public TripResponse create(LoginUser loginUser, TripRequest request) {
        User user = userRepository.findWithDepartmentById(loginUser.userId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Trip trip = tripRepository.save(Trip.builder()
                .user(user)
                .title(request.title())
                .purpose(request.purpose())
                .destination(request.destination())
                .startDate(request.startDate())
                .endDate(request.endDate())
                .expectedAmount(request.expectedAmount())
                .build());

        if (request.submitNow()) {
            submit(trip);
        }
        return TripResponse.of(trip, loginUser.userId());
    }

    /** 수정은 임시저장/반려 상태에서만. 기간을 줄여 연결된 일정이 밖으로 나가면 거부한다. */
    @Transactional
    public TripResponse update(LoginUser loginUser, Long tripId, TripRequest request) {
        Trip trip = findOwned(loginUser, tripId);
        trip.update(request.title(), request.purpose(), request.destination(),
                request.startDate(), request.endDate(), request.expectedAmount());

        boolean outOfRange = scheduleRepository.findByTripIdOrderByStartAtAsc(tripId).stream()
                .anyMatch(s -> !trip.covers(s.getStartAt(), s.getEndAt()));
        if (outOfRange) {
            throw new BusinessException(ErrorCode.TRIP_SCHEDULE_OUT_OF_RANGE);
        }
        return TripResponse.of(trip, loginUser.userId());
    }

    /** 신청 / 반려 후 재신청: 출장 상태 변경 + 결재 건 생성 */
    @Transactional
    public TripResponse request(LoginUser loginUser, Long tripId) {
        Trip trip = findOwned(loginUser, tripId);
        submit(trip);
        return TripResponse.of(trip, loginUser.userId());
    }

    /** 취소: 대기 중인 결재 건과 연결된 일정도 함께 취소한다. */
    @Transactional
    public TripResponse cancel(LoginUser loginUser, Long tripId) {
        Trip trip = findOwned(loginUser, tripId);
        boolean wasRequested = trip.getStatus() == TripStatus.REQUESTED;
        trip.cancel();

        if (wasRequested) {
            approvalService.cancelPending(ApprovalTargetType.TRIP, tripId);
        }
        scheduleRepository.findByTripIdOrderByStartAtAsc(tripId).stream()
                .filter(s -> s.getStatus() != ScheduleStatus.DONE)
                .forEach(Schedule::cancel);
        return TripResponse.of(trip, loginUser.userId());
    }

    @Transactional
    public TripResponse start(LoginUser loginUser, Long tripId) {
        Trip trip = findOwned(loginUser, tripId);
        trip.start();
        return TripResponse.of(trip, loginUser.userId());
    }

    @Transactional
    public TripResponse complete(LoginUser loginUser, Long tripId) {
        Trip trip = findOwned(loginUser, tripId);
        trip.complete();
        return TripResponse.of(trip, loginUser.userId());
    }

    /** 임시저장 건만 삭제. 연결된 일정은 지우지 않고 연결만 해제한다. */
    @Transactional
    public void delete(LoginUser loginUser, Long tripId) {
        Trip trip = findOwned(loginUser, tripId);
        trip.checkDeletable();
        scheduleRepository.findByTripIdOrderByStartAtAsc(tripId).forEach(s -> s.linkTrip(null));
        tripRepository.delete(trip);
    }

    private void submit(Trip trip) {
        trip.request();
        approvalService.request(ApprovalTargetType.TRIP, trip.getId(), trip.getTitle(), trip.getUser());
    }

    private Trip findDetail(Long tripId) {
        return tripRepository.findDetailById(tripId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TRIP_NOT_FOUND));
    }

    /** 변경 작업은 본인 출장만 */
    private Trip findOwned(LoginUser loginUser, Long tripId) {
        Trip trip = findDetail(tripId);
        AccessPolicy.checkOwner(loginUser, trip.getUser());
        return trip;
    }

    /** 조회는 본인, 같은 부서 팀장, 관리자 */
    private void checkReadable(Trip trip, LoginUser loginUser) {
        AccessPolicy.checkReadable(loginUser, trip.getUser());
    }
}
