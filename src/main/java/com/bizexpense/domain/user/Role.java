package com.bizexpense.domain.user;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Role {

    USER("일반 직원"),
    MANAGER("팀장"),
    ADMIN("관리자");

    private final String label;

    /** Spring Security 권한 문자열 (ROLE_USER, ROLE_MANAGER, ROLE_ADMIN) */
    public String authority() {
        return "ROLE_" + name();
    }
}
