package com.bizexpense.domain.user;

import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

    @EntityGraph(attributePaths = "department")
    Optional<User> findByLoginId(String loginId);

    @EntityGraph(attributePaths = "department")
    Optional<User> findWithDepartmentById(Long id);
}
