package com.bizexpense.domain.auth;

import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.bizexpense.domain.audit.AuditAction;
import com.bizexpense.domain.audit.Audited;
import com.bizexpense.domain.auth.dto.LoginRequest;
import com.bizexpense.domain.auth.dto.LoginResponse;
import com.bizexpense.domain.auth.dto.MeResponse;
import com.bizexpense.global.common.ApiResponse;
import com.bizexpense.global.security.LoginUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "01. 인증", description = "로그인 / 로그아웃 / 내 정보")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Audited(AuditAction.LOGIN)
    @SecurityRequirements // 인증 없이 호출
    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(authService.login(request));
    }

    /**
     * JWT 는 서버에 상태가 없으므로 클라이언트가 토큰을 폐기하는 것으로 로그아웃한다.
     * (토큰 블랙리스트 / 리프레시 토큰은 이후 확장 범위)
     */
    @Audited(AuditAction.LOGOUT)
    @PostMapping("/logout")
    public ApiResponse<Void> logout() {
        return ApiResponse.ok();
    }

    @GetMapping("/me")
    public ApiResponse<MeResponse> me(@AuthenticationPrincipal LoginUser loginUser) {
        return ApiResponse.ok(authService.me(loginUser));
    }
}
