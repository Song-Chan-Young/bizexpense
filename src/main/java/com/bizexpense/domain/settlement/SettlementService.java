package com.bizexpense.domain.settlement;

import com.bizexpense.domain.approval.ApprovalService;
import com.bizexpense.domain.approval.ApprovalTargetType;
import com.bizexpense.domain.expense.Expense;
import com.bizexpense.domain.expense.ExpenseRepository;
import com.bizexpense.domain.expense.dto.ExpenseResponse;
import com.bizexpense.domain.settlement.dto.SettlementDetailResponse;
import com.bizexpense.domain.settlement.dto.SettlementResponse;
import com.bizexpense.domain.settlement.dto.SettlementSearchCondition;
import com.bizexpense.domain.settlement.dto.TripSettlementResponse;
import com.bizexpense.domain.trip.Trip;
import com.bizexpense.domain.trip.TripRepository;
import com.bizexpense.domain.trip.TripStatus;
import com.bizexpense.domain.user.AccessPolicy;
import com.bizexpense.global.common.PageResponse;
import com.bizexpense.global.error.BusinessException;
import com.bizexpense.global.error.ErrorCode;
import com.bizexpense.global.security.LoginUser;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 정산 신청 → 팀장 결재 → 관리자 지급 처리.
 * 상태가 바뀌는 작업은 정산 + 포함된 경비 + 결재 건을 한 트랜잭션에서 함께 바꾸고,
 * 중간에 실패하면 전부 롤백한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SettlementService {

    private static final List<SettlementStatus> OPEN_STATUSES =
            Arrays.stream(SettlementStatus.values()).filter(SettlementStatus::isOpen).toList();

    private final SettlementRepository settlementRepository;
    private final ExpenseRepository expenseRepository;
    private final TripRepository tripRepository;
    private final ApprovalService approvalService;

    public PageResponse<SettlementResponse> search(LoginUser loginUser, SettlementSearchCondition cond,
                                                   Pageable pageable) {
        cond.scopeOrDefault().checkAllowed(loginUser);
        return PageResponse.of(
                settlementRepository.findAll(SettlementSpecs.search(loginUser, cond), pageable),
                s -> SettlementResponse.of(s, loginUser));
    }

    public SettlementDetailResponse get(LoginUser loginUser, Long settlementId) {
        Settlement settlement = findDetail(settlementId);
        AccessPolicy.checkReadable(loginUser, settlement.getUser());
        List<ExpenseResponse> expenses = expenseRepository.findBySettlementId(settlementId).stream()
                .map(e -> ExpenseResponse.of(e, loginUser.userId()))
                .toList();
        return new SettlementDetailResponse(
                SettlementResponse.of(settlement, loginUser),
                expenses,
                approvalService.history(ApprovalTargetType.SETTLEMENT, settlementId, loginUser.userId()));
    }

    /** 출장 상세의 정산 영역: 신청 가능 여부와 미리 계산한 금액, 정산 이력 */
    public TripSettlementResponse tripSettlement(LoginUser loginUser, Long tripId) {
        Trip trip = findTrip(tripId);
        AccessPolicy.checkReadable(loginUser, trip.getUser());

        List<Expense> settleable = expenseRepository.findSettleable(tripId, null);
        String reason = null;
        if (!trip.isOwnedBy(loginUser.userId())) {
            reason = "본인 출장만 정산을 신청할 수 있습니다.";
        } else if (trip.getStatus() != TripStatus.COMPLETED) {
            reason = "출장을 완료한 뒤 정산을 신청할 수 있습니다.";
        } else if (settlementRepository.existsByTripIdAndStatusIn(tripId, OPEN_STATUSES)) {
            reason = "진행 중인 정산이 있습니다.";
        } else if (settleable.isEmpty()) {
            reason = "정산할 경비가 없습니다.";
        }

        return new TripSettlementResponse(
                tripId,
                reason == null,
                reason,
                SettlementAmounts.of(settleable),
                settlementRepository.findByTripIdOrderByIdDesc(tripId).stream()
                        .map(s -> SettlementResponse.of(s, loginUser))
                        .toList());
    }

    /** 정산 신청: 완료된 본인 출장의 미정산 경비를 모두 묶어 결재를 올린다. */
    @Transactional
    public SettlementResponse request(LoginUser loginUser, Long tripId) {
        Trip trip = findTrip(tripId);
        AccessPolicy.checkOwner(loginUser, trip.getUser());
        if (trip.getStatus() != TripStatus.COMPLETED) {
            throw new BusinessException(ErrorCode.TRIP_NOT_SETTLEABLE);
        }
        if (settlementRepository.existsByTripIdAndStatusIn(tripId, OPEN_STATUSES)) {
            throw new BusinessException(ErrorCode.SETTLEMENT_IN_PROGRESS);
        }

        List<Expense> expenses = expenseRepository.findSettleable(tripId, null);
        Settlement settlement = settlementRepository.save(
                new Settlement(trip, trip.getUser(), SettlementAmounts.of(expenses)));
        expenses.forEach(e -> e.requestSettlement(settlement.getId()));

        submitApproval(settlement);
        return SettlementResponse.of(settlement, loginUser);
    }

    /** 반려 후 재신청: 반려된 경비(수정/삭제 반영) + 그 사이 새로 등록한 경비로 다시 계산한다. */
    @Transactional
    public SettlementResponse resubmit(LoginUser loginUser, Long settlementId) {
        Settlement settlement = findDetail(settlementId);
        AccessPolicy.checkOwner(loginUser, settlement.getUser());

        List<Expense> expenses = expenseRepository.findSettleable(settlement.getTrip().getId(), settlementId);
        settlement.resubmit(SettlementAmounts.of(expenses));
        expenses.forEach(e -> e.requestSettlement(settlementId));

        submitApproval(settlement);
        return SettlementResponse.of(settlement, loginUser);
    }

    /** 지급 완료 처리 (관리자) */
    @Transactional
    public SettlementResponse complete(LoginUser loginUser, Long settlementId) {
        if (!loginUser.isAdmin()) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
        Settlement settlement = findDetail(settlementId);
        settlement.complete();
        expenseRepository.findBySettlementId(settlementId).forEach(Expense::settle);
        return SettlementResponse.of(settlement, loginUser);
    }

    private void submitApproval(Settlement settlement) {
        String title = settlement.getTrip().getTitle() + " 정산 (지급 "
                + NumberFormat.getInstance(Locale.KOREA).format(settlement.getPayableAmount()) + "원)";
        approvalService.request(ApprovalTargetType.SETTLEMENT, settlement.getId(), title, settlement.getUser());
    }

    private Trip findTrip(Long tripId) {
        return tripRepository.findDetailById(tripId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TRIP_NOT_FOUND));
    }

    private Settlement findDetail(Long settlementId) {
        return settlementRepository.findDetailById(settlementId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SETTLEMENT_NOT_FOUND));
    }
}
