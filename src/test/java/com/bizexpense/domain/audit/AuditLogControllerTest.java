package com.bizexpense.domain.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bizexpense.domain.department.Department;
import com.bizexpense.domain.department.DepartmentRepository;
import com.bizexpense.domain.user.Role;
import com.bizexpense.domain.user.User;
import com.bizexpense.domain.user.UserRepository;
import com.bizexpense.global.security.JwtTokenProvider;
import com.bizexpense.global.security.LoginUser;
import com.jayway.jsonpath.JsonPath;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

/** 감사 로그: 변경 요청의 성공/실패 기록과 관리자 조회 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuditLogControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;
    @Autowired DepartmentRepository departmentRepository;
    @Autowired AuditLogRepository auditLogRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired JwtTokenProvider jwtTokenProvider;

    final Map<String, User> users = new HashMap<>();
    final Map<String, String> tokens = new HashMap<>();

    @BeforeEach
    void setUp() {
        Department sales1 = departmentRepository.save(new Department("영업1팀", "SALES1"));
        user(sales1, "user1", "이사원", Role.USER);
        user(sales1, "user2", "박사원", Role.USER);
        user(sales1, "admin", "관리자", Role.ADMIN);
    }

    @Test
    void 로그인_성공과_실패를_남긴다() throws Exception {
        login("user1", "pass1234").andExpect(status().isOk());
        login("user1", "wrong").andExpect(status().isUnauthorized());
        login("nobody", "pass1234").andExpect(status().isUnauthorized());

        List<AuditLog> logs = logs();
        assertThat(logs).extracting(AuditLog::getAction).containsOnly(AuditAction.LOGIN);
        assertThat(logs).extracting(AuditLog::getResult)
                .containsExactly(AuditResult.SUCCESS, AuditResult.FAILURE, AuditResult.FAILURE);
        assertThat(logs.get(0).getActorId()).isEqualTo(users.get("user1").getId());
        assertThat(logs.get(0).getActorName()).isEqualTo("이사원");
        assertThat(logs.get(1).getErrorCode()).isEqualTo("INVALID_CREDENTIALS");
        // 없는 아이디는 입력한 아이디만 남는다
        assertThat(logs.get(2).getActorId()).isNull();
        assertThat(logs.get(2).getActorLoginId()).isEqualTo("nobody");
    }

    @Test
    void 등록은_응답의_ID를_삭제는_주소의_ID를_대상으로_남긴다() throws Exception {
        long tripId = createTrip("user1");
        mockMvc.perform(delete("/api/trips/{id}", tripId).header("Authorization", tokens.get("user1")))
                .andExpect(status().isOk());

        List<AuditLog> logs = logs();
        assertThat(logs).extracting(AuditLog::getAction)
                .containsExactly(AuditAction.TRIP_CREATE, AuditAction.TRIP_DELETE);
        assertThat(logs).extracting(AuditLog::getTargetId).containsOnly(tripId);
        assertThat(logs).extracting(AuditLog::getTargetType).containsOnly("TRIP");
        assertThat(logs.get(1).getHttpMethod()).isEqualTo("DELETE");
        assertThat(logs.get(1).getUri()).isEqualTo("/api/trips/" + tripId);
    }

    @Test
    void 권한_없는_변경_시도는_실패로_남긴다() throws Exception {
        long tripId = createTrip("user1");

        mockMvc.perform(put("/api/trips/{id}", tripId).header("Authorization", tokens.get("user2"))
                        .contentType(MediaType.APPLICATION_JSON).content(tripJson()))
                .andExpect(status().isForbidden());

        AuditLog log = logs().getLast();
        assertThat(log.getAction()).isEqualTo(AuditAction.TRIP_UPDATE);
        assertThat(log.getResult()).isEqualTo(AuditResult.FAILURE);
        assertThat(log.getErrorCode()).isEqualTo("ACCESS_DENIED");
        assertThat(log.getActorName()).isEqualTo("박사원");
        assertThat(log.getTargetId()).isEqualTo(tripId);
    }

    @Test
    void 조회_요청은_남기지_않는다() throws Exception {
        mockMvc.perform(get("/api/trips").header("Authorization", tokens.get("user1"))).andExpect(status().isOk());
        assertThat(auditLogRepository.count()).isZero();
    }

    @Test
    void 관리자는_조건으로_검색한다() throws Exception {
        login("user1", "pass1234");
        login("user2", "wrong");
        createTrip("user1");

        search("actor=이사원").andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(2))
                // 최신순
                .andExpect(jsonPath("$.data.content[0].action").value("TRIP_CREATE"))
                .andExpect(jsonPath("$.data.content[0].actionLabel").value("출장 등록"));
        search("result=FAILURE")
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].actorLoginId").value("user2"))
                .andExpect(jsonPath("$.data.content[0].errorCode").value("INVALID_CREDENTIALS"));
        search("targetType=TRIP").andExpect(jsonPath("$.data.totalElements").value(1));
        search("action=LOGIN").andExpect(jsonPath("$.data.totalElements").value(2));
    }

    @Test
    void 관리자만_조회할_수_있다() throws Exception {
        mockMvc.perform(get("/api/admin/audit-logs").header("Authorization", tokens.get("user1")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/audit-logs/actions").header("Authorization", tokens.get("admin")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].value").value("LOGIN"))
                .andExpect(jsonPath("$.data[0].label").value("로그인"));
    }

    @Test
    void API_문서를_제공한다() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("BizExpense API"))
                .andExpect(jsonPath("$.paths['/api/trips']").exists())
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"));
    }

    // ---------- helpers ----------

    private void user(Department dept, String loginId, String name, Role role) {
        User user = userRepository.save(User.builder().department(dept).loginId(loginId)
                .password(passwordEncoder.encode("pass1234")).name(name).role(role).build());
        users.put(loginId, user);
        tokens.put(loginId, "Bearer " + jwtTokenProvider.createToken(
                new LoginUser(user.getId(), loginId, role, dept.getId())));
    }

    private List<AuditLog> logs() {
        return auditLogRepository.findAll(Sort.by("id"));
    }

    private ResultActions login(String loginId, String password) throws Exception {
        return mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"loginId\":\"%s\",\"password\":\"%s\"}".formatted(loginId, password)));
    }

    private String tripJson() {
        return """
                {"title":"부산 출장","purpose":"거래처 미팅","destination":"부산","startDate":"2026-12-01",
                 "endDate":"2026-12-03","expectedAmount":300000,"submit":false}
                """;
    }

    private long createTrip(String who) throws Exception {
        String body = mockMvc.perform(post("/api/trips").header("Authorization", tokens.get(who))
                        .contentType(MediaType.APPLICATION_JSON).content(tripJson()))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.data.tripId")).longValue();
    }

    private ResultActions search(String query) throws Exception {
        return mockMvc.perform(get("/api/admin/audit-logs?" + query).header("Authorization", tokens.get("admin")));
    }
}
