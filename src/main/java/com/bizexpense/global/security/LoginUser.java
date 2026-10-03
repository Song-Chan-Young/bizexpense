package com.bizexpense.global.security;

import com.bizexpense.domain.user.Role;
import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/**
 * 인증된 사용자 정보. JWT 클레임에서 복원되며 컨트롤러에서
 * {@code @AuthenticationPrincipal LoginUser loginUser} 로 받는다.
 * 서비스 계층의 소유권/부서 권한 검사에 사용한다.
 */
public record LoginUser(Long userId, String loginId, Role role, Long departmentId) {

    public Collection<? extends GrantedAuthority> authorities() {
        return List.of(new SimpleGrantedAuthority(role.authority()));
    }

    public boolean isAdmin() {
        return role == Role.ADMIN;
    }

    public boolean isManager() {
        return role == Role.MANAGER;
    }
}
