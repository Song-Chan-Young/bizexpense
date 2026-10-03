package com.bizexpense.domain.trip;

import com.bizexpense.domain.trip.dto.TripDetailResponse;
import com.bizexpense.domain.trip.dto.TripOptionResponse;
import com.bizexpense.domain.trip.dto.TripRequest;
import com.bizexpense.domain.trip.dto.TripResponse;
import com.bizexpense.domain.trip.dto.TripSearchCondition;
import com.bizexpense.global.common.ApiResponse;
import com.bizexpense.global.common.PageResponse;
import com.bizexpense.global.security.LoginUser;
import jakarta.validation.Valid;
import java.util.List;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/trips")
@RequiredArgsConstructor
public class TripController {

    private final TripService tripService;

    @GetMapping
    public ApiResponse<PageResponse<TripResponse>> search(
            @AuthenticationPrincipal LoginUser loginUser,
            @ModelAttribute TripSearchCondition cond,
            @PageableDefault(size = 10, sort = "startDate", direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.ok(tripService.search(loginUser, cond, pageable));
    }

    /** 일정 등록 화면용: 일정을 연결할 수 있는 내 출장 */
    @GetMapping("/schedulable")
    public ApiResponse<List<TripOptionResponse>> schedulable(@AuthenticationPrincipal LoginUser loginUser) {
        return ApiResponse.ok(tripService.schedulableTrips(loginUser));
    }

    /** 경비 등록 화면용: 경비를 등록할 수 있는 내 출장 */
    @GetMapping("/expensable")
    public ApiResponse<List<TripOptionResponse>> expensable(@AuthenticationPrincipal LoginUser loginUser) {
        return ApiResponse.ok(tripService.expensableTrips(loginUser));
    }

    @GetMapping("/{tripId}")
    public ApiResponse<TripDetailResponse> get(@AuthenticationPrincipal LoginUser loginUser,
                                               @PathVariable Long tripId) {
        return ApiResponse.ok(tripService.get(loginUser, tripId));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<TripResponse> create(@AuthenticationPrincipal LoginUser loginUser,
                                            @Valid @RequestBody TripRequest request) {
        return ApiResponse.ok(tripService.create(loginUser, request));
    }

    @PutMapping("/{tripId}")
    public ApiResponse<TripResponse> update(@AuthenticationPrincipal LoginUser loginUser,
                                            @PathVariable Long tripId,
                                            @Valid @RequestBody TripRequest request) {
        return ApiResponse.ok(tripService.update(loginUser, tripId, request));
    }

    @DeleteMapping("/{tripId}")
    public ApiResponse<Void> delete(@AuthenticationPrincipal LoginUser loginUser, @PathVariable Long tripId) {
        tripService.delete(loginUser, tripId);
        return ApiResponse.ok();
    }

    @PostMapping("/{tripId}/request")
    public ApiResponse<TripResponse> request(@AuthenticationPrincipal LoginUser loginUser, @PathVariable Long tripId) {
        return ApiResponse.ok(tripService.request(loginUser, tripId));
    }

    @PostMapping("/{tripId}/cancel")
    public ApiResponse<TripResponse> cancel(@AuthenticationPrincipal LoginUser loginUser, @PathVariable Long tripId) {
        return ApiResponse.ok(tripService.cancel(loginUser, tripId));
    }

    @PostMapping("/{tripId}/start")
    public ApiResponse<TripResponse> start(@AuthenticationPrincipal LoginUser loginUser, @PathVariable Long tripId) {
        return ApiResponse.ok(tripService.start(loginUser, tripId));
    }

    @PostMapping("/{tripId}/complete")
    public ApiResponse<TripResponse> complete(@AuthenticationPrincipal LoginUser loginUser,
                                              @PathVariable Long tripId) {
        return ApiResponse.ok(tripService.complete(loginUser, tripId));
    }
}
