package com.bizexpense.domain.expense;

import com.bizexpense.domain.code.CodeService;
import com.bizexpense.domain.expense.dto.ExpenseRequest;
import com.bizexpense.domain.expense.dto.ExpenseResponse;
import com.bizexpense.domain.expense.dto.ExpenseSearchCondition;
import com.bizexpense.domain.expense.dto.ExpenseSearchResponse;
import com.bizexpense.domain.expense.dto.ExpenseTotals;
import com.bizexpense.domain.expense.dto.TripExpenseSummaryResponse;
import com.bizexpense.domain.trip.Trip;
import com.bizexpense.domain.trip.TripRepository;
import com.bizexpense.domain.user.AccessPolicy;
import com.bizexpense.domain.user.User;
import com.bizexpense.domain.user.UserRepository;
import com.bizexpense.global.common.PageResponse;
import com.bizexpense.global.error.BusinessException;
import com.bizexpense.global.error.ErrorCode;
import com.bizexpense.global.security.LoginUser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final TripRepository tripRepository;
    private final UserRepository userRepository;
    private final CodeService codeService;

    /** 목록 + 조건 전체 합계. 합계는 페이지와 관계없이 같은 조건으로 따로 집계한다. */
    public ExpenseSearchResponse search(LoginUser loginUser, ExpenseSearchCondition cond, Pageable pageable) {
        cond.scopeOrDefault().checkAllowed(loginUser);
        Specification<Expense> spec = ExpenseSpecs.search(loginUser, cond);
        return new ExpenseSearchResponse(
                PageResponse.of(expenseRepository.findAll(spec, pageable), e -> ExpenseResponse.of(e, loginUser.userId())),
                expenseRepository.sumAmount(spec));
    }

    public ExpenseResponse get(LoginUser loginUser, Long expenseId) {
        Expense expense = findDetail(expenseId);
        AccessPolicy.checkReadable(loginUser, expense.getUser());
        return ExpenseResponse.of(expense, loginUser.userId());
    }

    /** 출장별 경비 요약: 총액, 법인/개인 부담, 비용 항목별 합계 */
    public TripExpenseSummaryResponse tripSummary(LoginUser loginUser, Long tripId) {
        Trip trip = tripRepository.findDetailById(tripId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TRIP_NOT_FOUND));
        AccessPolicy.checkReadable(loginUser, trip.getUser());

        ExpenseTotals totals = expenseRepository.sumByTrip(tripId);
        return new TripExpenseSummaryResponse(
                tripId,
                trip.getExpectedAmount(),
                totals.totalAmount(),
                totals.corporateAmount(),
                totals.totalAmount() - totals.corporateAmount(),
                totals.count(),
                expenseRepository.sumByTripGroupByCategory(tripId));
    }

    @Transactional
    public ExpenseResponse create(LoginUser loginUser, ExpenseRequest request) {
        User user = userRepository.findById(loginUser.userId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        Expense expense = Expense.builder()
                .trip(expensableTrip(loginUser, request.tripId()))
                .user(user)
                .category(codeService.activeCategory(request.categoryId()))
                .paymentMethod(codeService.activePaymentMethod(request.paymentMethodId()))
                .usedAt(request.usedAt())
                .storeName(request.storeName().trim())
                .amount(request.amount())
                .description(request.description())
                .build();
        return ExpenseResponse.of(expenseRepository.save(expense), loginUser.userId());
    }

    @Transactional
    public ExpenseResponse update(LoginUser loginUser, Long expenseId, ExpenseRequest request) {
        Expense expense = findDetail(expenseId);
        AccessPolicy.checkOwner(loginUser, expense.getUser());

        Trip trip = expense.getTrip().getId().equals(request.tripId())
                ? expense.getTrip()
                : expensableTrip(loginUser, request.tripId());
        expense.update(trip,
                codeService.activeCategory(request.categoryId()),
                codeService.activePaymentMethod(request.paymentMethodId()),
                request.usedAt(), request.storeName().trim(), request.amount(), request.description());
        return ExpenseResponse.of(expense, loginUser.userId());
    }

    @Transactional
    public void delete(LoginUser loginUser, Long expenseId) {
        Expense expense = findDetail(expenseId);
        AccessPolicy.checkOwner(loginUser, expense.getUser());
        expense.checkDeletable();
        expenseRepository.delete(expense);
    }

    /** 본인 출장이고, 출장이 시작된 뒤(진행중/완료)여야 경비를 등록할 수 있다. */
    private Trip expensableTrip(LoginUser loginUser, Long tripId) {
        Trip trip = tripRepository.findDetailById(tripId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TRIP_NOT_FOUND));
        if (!trip.isOwnedBy(loginUser.userId())) {
            throw new BusinessException(ErrorCode.TRIP_NOT_EXPENSABLE, "본인 출장에만 경비를 등록할 수 있습니다.");
        }
        if (!trip.getStatus().isExpensable()) {
            throw new BusinessException(ErrorCode.TRIP_NOT_EXPENSABLE,
                    "'" + trip.getStatus().getLabel() + "' 상태의 출장에는 경비를 등록할 수 없습니다. (진행중/완료 출장만 가능)");
        }
        return trip;
    }

    private Expense findDetail(Long expenseId) {
        return expenseRepository.findDetailById(expenseId)
                .orElseThrow(() -> new BusinessException(ErrorCode.EXPENSE_NOT_FOUND));
    }
}
