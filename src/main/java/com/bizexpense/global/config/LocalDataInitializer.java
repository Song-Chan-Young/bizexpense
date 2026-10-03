package com.bizexpense.global.config;

import com.bizexpense.domain.department.Department;
import com.bizexpense.domain.department.DepartmentRepository;
import com.bizexpense.domain.user.Role;
import com.bizexpense.domain.user.User;
import com.bizexpense.domain.user.UserRepository;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** 로컬 개발용 초기 데이터. 사용자가 한 명도 없을 때만 넣는다. */
@Slf4j
@Component
@Profile("local")
@RequiredArgsConstructor
public class LocalDataInitializer implements ApplicationRunner {

    public static final String DEFAULT_PASSWORD = "pass1234";

    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userRepository.count() > 0) {
            return;
        }

        Department admin = departmentRepository.save(new Department("경영지원팀", "MGT"));
        Department sales = departmentRepository.save(new Department("영업1팀", "SALES1"));

        String encoded = passwordEncoder.encode(DEFAULT_PASSWORD);
        userRepository.save(user(admin, "admin", encoded, "관리자", Role.ADMIN));
        userRepository.save(user(sales, "manager1", encoded, "김팀장", Role.MANAGER));
        userRepository.save(user(sales, "user1", encoded, "이사원", Role.USER));
        userRepository.save(user(sales, "user2", encoded, "박사원", Role.USER));

        log.info("로컬 초기 데이터 생성 완료 (admin / manager1 / user1 / user2, 비밀번호 {})", DEFAULT_PASSWORD);
    }

    private User user(Department dept, String loginId, String password, String name, Role role) {
        return User.builder()
                .department(dept)
                .loginId(loginId)
                .password(password)
                .name(name)
                .email(loginId + "@bizexpense.local")
                .role(role)
                .hireDate(LocalDate.of(2024, 1, 2))
                .build();
    }
}
