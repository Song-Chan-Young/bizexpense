package com.bizexpense.domain.auth.dto;

import com.bizexpense.domain.department.Department;
import com.bizexpense.domain.user.Role;
import com.bizexpense.domain.user.User;

public record MeResponse(
        Long userId,
        String loginId,
        String name,
        String email,
        Role role,
        String roleLabel,
        Long departmentId,
        String departmentName) {

    public static MeResponse from(User user) {
        Department dept = user.getDepartment();
        return new MeResponse(
                user.getId(),
                user.getLoginId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.getRole().getLabel(),
                dept == null ? null : dept.getId(),
                dept == null ? null : dept.getName());
    }
}
