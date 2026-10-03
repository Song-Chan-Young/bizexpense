package com.bizexpense.domain.expense;

import com.bizexpense.domain.expense.dto.ExpenseRequest;
import com.bizexpense.domain.expense.dto.ExpenseResponse;
import com.bizexpense.domain.expense.dto.ExpenseSearchCondition;
import com.bizexpense.domain.expense.dto.ExpenseSearchResponse;
import com.bizexpense.domain.expense.dto.TripExpenseSummaryResponse;
import com.bizexpense.global.common.ApiResponse;
import com.bizexpense.global.security.LoginUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ExpenseController {

    private final ExpenseService expenseService;

    @GetMapping("/api/expenses")
    public ApiResponse<ExpenseSearchResponse> search(
            @AuthenticationPrincipal LoginUser loginUser,
            @ModelAttribute ExpenseSearchCondition cond,
            @PageableDefault(size = 10, sort = {"usedAt", "id"}, direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.ok(expenseService.search(loginUser, cond, pageable));
    }

    @GetMapping("/api/expenses/{expenseId}")
    public ApiResponse<ExpenseResponse> get(@AuthenticationPrincipal LoginUser loginUser,
                                            @PathVariable Long expenseId) {
        return ApiResponse.ok(expenseService.get(loginUser, expenseId));
    }

    @PostMapping("/api/expenses")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ExpenseResponse> create(@AuthenticationPrincipal LoginUser loginUser,
                                               @Valid @RequestBody ExpenseRequest request) {
        return ApiResponse.ok(expenseService.create(loginUser, request));
    }

    @PutMapping("/api/expenses/{expenseId}")
    public ApiResponse<ExpenseResponse> update(@AuthenticationPrincipal LoginUser loginUser,
                                               @PathVariable Long expenseId,
                                               @Valid @RequestBody ExpenseRequest request) {
        return ApiResponse.ok(expenseService.update(loginUser, expenseId, request));
    }

    @DeleteMapping("/api/expenses/{expenseId}")
    public ApiResponse<Void> delete(@AuthenticationPrincipal LoginUser loginUser, @PathVariable Long expenseId) {
        expenseService.delete(loginUser, expenseId);
        return ApiResponse.ok();
    }

    /** 출장별 경비 요약 */
    @GetMapping("/api/trips/{tripId}/expense-summary")
    public ApiResponse<TripExpenseSummaryResponse> tripSummary(@AuthenticationPrincipal LoginUser loginUser,
                                                               @PathVariable Long tripId) {
        return ApiResponse.ok(expenseService.tripSummary(loginUser, tripId));
    }
}
