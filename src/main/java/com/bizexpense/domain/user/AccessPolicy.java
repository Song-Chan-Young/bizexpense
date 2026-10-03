package com.bizexpense.domain.user;

import com.bizexpense.global.error.BusinessException;
import com.bizexpense.global.error.ErrorCode;
import com.bizexpense.global.security.LoginUser;
import java.util.Objects;

/**
 * 사원 소유 데이터(일정, 출장, 경비 …)의 공통 접근 규칙.
 * - 조회: 본인, 같은 부서 팀장, 관리자
 * - 변경: 본인만
 */
public final class AccessPolicy {

    private AccessPolicy() {
    }

    public static boolean canRead(LoginUser viewer, User owner) {
        if (owner.getId().equals(viewer.userId()) || viewer.isAdmin()) {
            return true;
        }
        Long ownerDeptId = owner.getDepartment() == null ? null : owner.getDepartment().getId();
        return viewer.isManager() && ownerDeptId != null && Objects.equals(ownerDeptId, viewer.departmentId());
    }

    public static void checkReadable(LoginUser viewer, User owner) {
        if (!canRead(viewer, owner)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
    }

    public static void checkOwner(LoginUser viewer, User owner) {
        if (!owner.getId().equals(viewer.userId())) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
    }
}
