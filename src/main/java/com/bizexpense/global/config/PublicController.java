package com.bizexpense.global.config;

import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.bizexpense.domain.user.User;
import com.bizexpense.domain.user.UserRepository;
import com.bizexpense.global.common.ApiResponse;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 로그인 없이 접근하는 API (SecurityConfig 에서 /api/public/** 허용) */
@SecurityRequirements // 인증 없이 호출
@Tag(name = "00. 공개", description = "로그인 전 공개 정보")
@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class PublicController {

    /** 로그인 화면에 보여줄 체험 계정 (역할별 1개) */
    private static final List<String> DEMO_LOGIN_IDS = List.of("user1", "manager1", "admin");

    private final DemoProperties demoProperties;
    private final UserRepository userRepository;

    /** 배포 환경 헬스 체크 */
    @GetMapping("/health")
    public ApiResponse<Map<String, String>> health() {
        return ApiResponse.ok(Map.of("status", "UP"));
    }

    @GetMapping("/demo")
    public ApiResponse<DemoInfo> demo() {
        if (!demoProperties.enabled()) {
            return ApiResponse.ok(new DemoInfo(false, null, List.of()));
        }
        List<DemoAccount> accounts = DEMO_LOGIN_IDS.stream()
                .map(userRepository::findByLoginId)
                .flatMap(Optional::stream)
                .map(DemoAccount::from)
                .toList();
        return ApiResponse.ok(new DemoInfo(true, demoProperties.password(), accounts));
    }

    public record DemoInfo(boolean enabled, String password, List<DemoAccount> accounts) {
    }

    public record DemoAccount(String loginId, String name, String roleLabel, String departmentName) {

        static DemoAccount from(User u) {
            return new DemoAccount(u.getLoginId(), u.getName(), u.getRole().getLabel(),
                    u.getDepartment() == null ? null : u.getDepartment().getName());
        }
    }
}
