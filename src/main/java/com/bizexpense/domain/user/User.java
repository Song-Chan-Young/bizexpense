package com.bizexpense.domain.user;

import com.bizexpense.domain.department.Department;
import com.bizexpense.global.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 사원 (USER 는 대부분의 DB 에서 예약어라 테이블명은 users) */
@Getter
@Entity
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    @Column(name = "login_id", nullable = false, unique = true, length = 50)
    private String loginId;

    @Column(nullable = false)
    private String password;

    @Column(name = "user_name", nullable = false, length = 50)
    private String name;

    @Column(length = 100)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @Column(name = "hire_date")
    private LocalDate hireDate;

    @Column(name = "use_yn", nullable = false)
    private boolean active = true;

    @Builder
    private User(Department department, String loginId, String password, String name,
                 String email, Role role, LocalDate hireDate) {
        this.department = department;
        this.loginId = loginId;
        this.password = password;
        this.name = name;
        this.email = email;
        this.role = role;
        this.hireDate = hireDate;
    }
}
