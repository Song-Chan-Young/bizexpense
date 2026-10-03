package com.bizexpense.global.common;

import com.bizexpense.global.error.BusinessException;
import com.bizexpense.global.error.ErrorCode;
import com.bizexpense.global.security.LoginUser;

/** 목록/캘린더 조회 범위 */
public enum ViewScope {
    /** 본인 데이터 */
    ME,
    /** 같은 부서 데이터 (팀장, 관리자) */
    TEAM,
    /** 전체 데이터 (관리자) */
    ALL;

    /** 이 범위를 조회할 권한이 없으면 ACCESS_DENIED */
    public void checkAllowed(LoginUser loginUser) {
        boolean allowed = switch (this) {
            case ME -> true;
            case TEAM -> loginUser.isManager() || loginUser.isAdmin();
            case ALL -> loginUser.isAdmin();
        };
        if (!allowed) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
    }
}
