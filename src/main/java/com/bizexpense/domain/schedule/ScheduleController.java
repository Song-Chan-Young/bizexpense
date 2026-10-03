package com.bizexpense.domain.schedule;

import io.swagger.v3.oas.annotations.tags.Tag;
import com.bizexpense.domain.audit.AuditAction;
import com.bizexpense.domain.audit.Audited;
import com.bizexpense.global.common.ViewScope;
import com.bizexpense.domain.schedule.dto.ScheduleConflictResponse;
import com.bizexpense.domain.schedule.dto.ScheduleRequest;
import com.bizexpense.domain.schedule.dto.ScheduleResponse;
import com.bizexpense.domain.schedule.dto.ScheduleSearchCondition;
import com.bizexpense.global.common.ApiResponse;
import com.bizexpense.global.common.PageResponse;
import com.bizexpense.global.security.LoginUser;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "03. 일정", description = "일정 등록·조회, 캘린더, 충돌 검사")
@RestController
@RequestMapping("/api/schedules")
@RequiredArgsConstructor
public class ScheduleController {

    private final ScheduleService scheduleService;

    /** 내 일정 목록 (검색 + 페이징) */
    @GetMapping
    public ApiResponse<PageResponse<ScheduleResponse>> search(
            @AuthenticationPrincipal LoginUser loginUser,
            @ModelAttribute ScheduleSearchCondition cond,
            @PageableDefault(size = 10, sort = "startAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.ok(scheduleService.search(loginUser, cond, pageable));
    }

    /** 캘린더 (from 포함 ~ to 미포함) */
    @GetMapping("/calendar")
    public ApiResponse<List<ScheduleResponse>> calendar(
            @AuthenticationPrincipal LoginUser loginUser,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "ME") ViewScope scope) {
        return ApiResponse.ok(scheduleService.calendar(loginUser, from, to, scope));
    }

    /** 저장 전 시간대 충돌 미리 확인 */
    @GetMapping("/conflicts")
    public ApiResponse<List<ScheduleConflictResponse>> conflicts(
            @AuthenticationPrincipal LoginUser loginUser,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startAt,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endAt,
            @RequestParam(required = false) Long excludeId) {
        return ApiResponse.ok(scheduleService.findConflicts(loginUser, startAt, endAt, excludeId));
    }

    @GetMapping("/{scheduleId}")
    public ApiResponse<ScheduleResponse> get(@AuthenticationPrincipal LoginUser loginUser,
                                             @PathVariable Long scheduleId) {
        return ApiResponse.ok(scheduleService.get(loginUser, scheduleId));
    }

    @Audited(AuditAction.SCHEDULE_CREATE)
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ScheduleResponse> create(@AuthenticationPrincipal LoginUser loginUser,
                                                @Valid @RequestBody ScheduleRequest request) {
        return ApiResponse.ok(scheduleService.create(loginUser, request));
    }

    @Audited(AuditAction.SCHEDULE_UPDATE)
    @PutMapping("/{scheduleId}")
    public ApiResponse<ScheduleResponse> update(@AuthenticationPrincipal LoginUser loginUser,
                                                @PathVariable Long scheduleId,
                                                @Valid @RequestBody ScheduleRequest request) {
        return ApiResponse.ok(scheduleService.update(loginUser, scheduleId, request));
    }

    @Audited(AuditAction.SCHEDULE_DELETE)
    @DeleteMapping("/{scheduleId}")
    public ApiResponse<Void> delete(@AuthenticationPrincipal LoginUser loginUser,
                                    @PathVariable Long scheduleId) {
        scheduleService.delete(loginUser, scheduleId);
        return ApiResponse.ok();
    }
}
