package com.bizexpense.domain.approval;

import com.bizexpense.domain.approval.dto.ApprovalProcessRequest;
import com.bizexpense.domain.approval.dto.ApprovalResponse;
import com.bizexpense.domain.approval.dto.ApprovalSearchCondition;
import com.bizexpense.global.common.ApiResponse;
import com.bizexpense.global.common.PageResponse;
import com.bizexpense.global.security.LoginUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/approvals")
@RequiredArgsConstructor
public class ApprovalController {

    private final ApprovalService approvalService;

    @GetMapping
    public ApiResponse<PageResponse<ApprovalResponse>> search(
            @AuthenticationPrincipal LoginUser loginUser,
            @ModelAttribute ApprovalSearchCondition cond,
            @PageableDefault(size = 10, sort = "requestedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.ok(approvalService.search(loginUser, cond, pageable));
    }

    @PostMapping("/{approvalId}/approve")
    public ApiResponse<ApprovalResponse> approve(@AuthenticationPrincipal LoginUser loginUser,
                                                 @PathVariable Long approvalId,
                                                 @Valid @RequestBody(required = false) ApprovalProcessRequest request) {
        return ApiResponse.ok(approvalService.approve(loginUser, approvalId, request == null ? null : request.comment()));
    }

    @PostMapping("/{approvalId}/reject")
    public ApiResponse<ApprovalResponse> reject(@AuthenticationPrincipal LoginUser loginUser,
                                                @PathVariable Long approvalId,
                                                @Valid @RequestBody ApprovalProcessRequest request) {
        return ApiResponse.ok(approvalService.reject(loginUser, approvalId, request.comment()));
    }
}
