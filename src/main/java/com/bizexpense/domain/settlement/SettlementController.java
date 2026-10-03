package com.bizexpense.domain.settlement;

import com.bizexpense.domain.settlement.dto.SettlementDetailResponse;
import com.bizexpense.domain.settlement.dto.SettlementResponse;
import com.bizexpense.domain.settlement.dto.SettlementSearchCondition;
import com.bizexpense.domain.settlement.dto.TripSettlementResponse;
import com.bizexpense.global.common.ApiResponse;
import com.bizexpense.global.common.PageResponse;
import com.bizexpense.global.security.LoginUser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class SettlementController {

    private final SettlementService settlementService;

    @GetMapping("/api/settlements")
    public ApiResponse<PageResponse<SettlementResponse>> search(
            @AuthenticationPrincipal LoginUser loginUser,
            @ModelAttribute SettlementSearchCondition cond,
            @PageableDefault(size = 10, sort = "requestedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.ok(settlementService.search(loginUser, cond, pageable));
    }

    @GetMapping("/api/settlements/{settlementId}")
    public ApiResponse<SettlementDetailResponse> get(@AuthenticationPrincipal LoginUser loginUser,
                                                     @PathVariable Long settlementId) {
        return ApiResponse.ok(settlementService.get(loginUser, settlementId));
    }

    /** 출장 상세의 정산 영역 */
    @GetMapping("/api/trips/{tripId}/settlement")
    public ApiResponse<TripSettlementResponse> tripSettlement(@AuthenticationPrincipal LoginUser loginUser,
                                                              @PathVariable Long tripId) {
        return ApiResponse.ok(settlementService.tripSettlement(loginUser, tripId));
    }

    /** 정산 신청 */
    @PostMapping("/api/trips/{tripId}/settlement")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<SettlementResponse> request(@AuthenticationPrincipal LoginUser loginUser,
                                                   @PathVariable Long tripId) {
        return ApiResponse.ok(settlementService.request(loginUser, tripId));
    }

    /** 반려 후 재신청 */
    @PostMapping("/api/settlements/{settlementId}/request")
    public ApiResponse<SettlementResponse> resubmit(@AuthenticationPrincipal LoginUser loginUser,
                                                    @PathVariable Long settlementId) {
        return ApiResponse.ok(settlementService.resubmit(loginUser, settlementId));
    }

    /** 지급 완료 처리 (관리자) */
    @PostMapping("/api/settlements/{settlementId}/complete")
    public ApiResponse<SettlementResponse> complete(@AuthenticationPrincipal LoginUser loginUser,
                                                    @PathVariable Long settlementId) {
        return ApiResponse.ok(settlementService.complete(loginUser, settlementId));
    }
}
