package com.bizexpense.domain.user;

import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

    @EntityGraph(attributePaths = "department")
    Optional<User> findByLoginId(String loginId);

    @EntityGraph(attributePaths = "department")
    Optional<User> findWithDepartmentById(Long id);

    /** 부서 결재자(팀장) 찾기 */
    Optional<User> findFirstByDepartmentIdAndRoleAndActiveTrueOrderByIdAsc(Long departmentId, Role role);

    /** 팀장/관리자 본인 신청 건의 결재자(다른 관리자) 찾기 */
    Optional<User> findFirstByRoleAndActiveTrueAndIdNotOrderByIdAsc(Role role, Long excludeUserId);
}
