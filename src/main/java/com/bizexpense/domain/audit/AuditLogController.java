package com.bizexpense.domain.audit;

import io.swagger.v3.oas.annotations.tags.Tag;
import com.bizexpense.domain.audit.dto.AuditActionResponse;
import com.bizexpense.domain.audit.dto.AuditLogResponse;
import com.bizexpense.domain.audit.dto.AuditLogSearchCondition;
import com.bizexpense.global.common.ApiResponse;
import com.bizexpense.global.common.PageResponse;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RestController;

/** 감사 로그 조회 (관리자 전용: /api/admin/**) */
@Tag(name = "10. 감사 로그", description = "변경 요청 이력 (관리자)")
@RestController
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogRepository auditLogRepository;

    @GetMapping("/api/admin/audit-logs")
    @Transactional(readOnly = true)
    public ApiResponse<PageResponse<AuditLogResponse>> search(
            @ModelAttribute AuditLogSearchCondition cond,
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.ok(PageResponse.of(
                auditLogRepository.findAll(AuditLogSpecs.search(cond), pageable), AuditLogResponse::from));
    }

    @GetMapping("/api/admin/audit-logs/actions")
    public ApiResponse<List<AuditActionResponse>> actions() {
        return ApiResponse.ok(Arrays.stream(AuditAction.values()).map(AuditActionResponse::from).toList());
    }
}
