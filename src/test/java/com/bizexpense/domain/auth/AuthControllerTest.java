package com.bizexpense.domain.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bizexpense.domain.department.Department;
import com.bizexpense.domain.department.DepartmentRepository;
import com.bizexpense.domain.user.Role;
import com.bizexpense.domain.user.User;
import com.bizexpense.domain.user.UserRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;
    @Autowired DepartmentRepository departmentRepository;
    @Autowired PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        Department dept = departmentRepository.save(new Department("영업1팀", "SALES1"));
        userRepository.save(User.builder()
                .department(dept)
                .loginId("user1")
                .password(passwordEncoder.encode("pass1234"))
                .name("이사원")
                .role(Role.USER)
                .build());
    }

    @Test
    void 로그인_성공시_토큰과_사용자정보를_반환한다() throws Exception {
        mockMvc.perform(login("user1", "pass1234"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.user.loginId").value("user1"))
                .andExpect(jsonPath("$.data.user.departmentName").value("영업1팀"));
    }

    @Test
    void 비밀번호가_틀리면_401() throws Exception {
        mockMvc.perform(login("user1", "wrong"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    void 없는_아이디도_같은_오류코드로_응답한다() throws Exception {
        mockMvc.perform(login("nobody", "pass1234"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    void 필수값이_없으면_400() throws Exception {
        mockMvc.perform(login("", ""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_INPUT"));
    }

    @Test
    void 토큰_없이_내정보_조회하면_401() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    @Test
    void 위조된_토큰이면_401() throws Exception {
        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer abc.def.ghi"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void 토큰으로_내정보를_조회한다() throws Exception {
        String body = mockMvc.perform(login("user1", "pass1234"))
                .andReturn().getResponse().getContentAsString();
        String token = JsonPath.read(body, "$.data.accessToken");

        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("이사원"))
                .andExpect(jsonPath("$.data.role").value("USER"));
    }

    @Test
    void 일반_직원은_관리자_API에_접근할_수_없다() throws Exception {
        String body = mockMvc.perform(login("user1", "pass1234"))
                .andReturn().getResponse().getContentAsString();
        String token = JsonPath.read(body, "$.data.accessToken");

        mockMvc.perform(get("/api/admin/anything").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("ACCESS_DENIED"));
    }

    private org.springframework.test.web.servlet.RequestBuilder login(String loginId, String password) {
        return post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"loginId":"%s","password":"%s"}
                        """.formatted(loginId, password));
    }
}
