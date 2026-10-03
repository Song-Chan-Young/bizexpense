package com.bizexpense.domain.dashboard;

import com.bizexpense.domain.dashboard.dto.DashboardResponse;
import com.bizexpense.global.common.ApiResponse;
import com.bizexpense.global.security.LoginUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/api/dashboard")
    public ApiResponse<DashboardResponse> dashboard(@AuthenticationPrincipal LoginUser loginUser) {
        return ApiResponse.ok(dashboardService.dashboard(loginUser));
    }
}
