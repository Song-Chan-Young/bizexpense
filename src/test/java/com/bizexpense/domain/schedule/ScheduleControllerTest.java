package com.bizexpense.domain.schedule;

import static org.hamcrest.Matchers.containsString;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ScheduleControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;
    @Autowired DepartmentRepository departmentRepository;
    @Autowired JwtTokenProvider jwtTokenProvider;

    String user1;      // 영업1팀 직원
    String user2;      // 영업1팀 직원
    String manager;    // 영업1팀 팀장
    String otherMgr;   // 영업2팀 팀장
    String admin;

    @BeforeEach
    void setUp() {
        Department sales1 = departmentRepository.save(new Department("영업1팀", "SALES1"));
        Department sales2 = departmentRepository.save(new Department("영업2팀", "SALES2"));
        user1 = token(sales1, "user1", Role.USER);
        user2 = token(sales1, "user2", Role.USER);
        manager = token(sales1, "manager1", Role.MANAGER);
        otherMgr = token(sales2, "manager2", Role.MANAGER);
        admin = token(sales2, "admin", Role.ADMIN);
    }

    // ---------- 등록 / 조회 ----------

    @Test
    void 일정을_등록하면_예정_상태로_저장된다() throws Exception {
        create(user1, "A거래처 미팅", "2026-10-12T14:00", "2026-10-12T16:00", false)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.title").value("A거래처 미팅"))
                .andExpect(jsonPath("$.data.status").value("PLANNED"))
                .andExpect(jsonPath("$.data.editable").value(true));
    }

    @Test
    void 종료가_시작보다_빠르면_400() throws Exception {
        create(user1, "잘못된 일정", "2026-10-12T16:00", "2026-10-12T14:00", false)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_SCHEDULE_PERIOD"));
    }

    @Test
    void 제목이_없으면_400() throws Exception {
        create(user1, "", "2026-10-12T14:00", "2026-10-12T16:00", false)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_INPUT"));
    }

    // ---------- 일정 충돌 검사 ----------

    @Test
    void 같은_시간대에_일정이_있으면_409_로_경고한다() throws Exception {
        create(user1, "A거래처 미팅", "2026-10-12T14:00", "2026-10-12T16:00", false);

        create(user1, "B거래처 미팅", "2026-10-12T15:00", "2026-10-12T17:00", false)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("SCHEDULE_CONFLICT"))
                .andExpect(jsonPath("$.error.message", containsString("A거래처 미팅")));
    }

    @Test
    void 경고를_확인하고_allowOverlap_으로_요청하면_저장된다() throws Exception {
        create(user1, "A거래처 미팅", "2026-10-12T14:00", "2026-10-12T16:00", false);

        create(user1, "B거래처 미팅", "2026-10-12T15:00", "2026-10-12T17:00", true)
                .andExpect(status().isCreated());
    }

    @Test
    void 끝나는_시각에_바로_시작하는_일정은_충돌이_아니다() throws Exception {
        create(user1, "A거래처 미팅", "2026-10-12T14:00", "2026-10-12T16:00", false);

        create(user1, "이동", "2026-10-12T16:00", "2026-10-12T17:00", false)
                .andExpect(status().isCreated());
    }

    @Test
    void 다른_직원의_일정과는_충돌하지_않는다() throws Exception {
        create(user1, "A거래처 미팅", "2026-10-12T14:00", "2026-10-12T16:00", false);

        create(user2, "B거래처 미팅", "2026-10-12T14:00", "2026-10-12T16:00", false)
                .andExpect(status().isCreated());
    }

    @Test
    void 취소된_일정은_충돌_대상이_아니다() throws Exception {
        long id = createdId(user1, "A거래처 미팅", "2026-10-12T14:00", "2026-10-12T16:00");
        update(user1, id, "A거래처 미팅", "2026-10-12T14:00", "2026-10-12T16:00", "CANCELLED")
                .andExpect(status().isOk());

        create(user1, "B거래처 미팅", "2026-10-12T14:00", "2026-10-12T16:00", false)
                .andExpect(status().isCreated());
    }

    @Test
    void 수정할_때_자기_자신과는_충돌하지_않는다() throws Exception {
        long id = createdId(user1, "A거래처 미팅", "2026-10-12T14:00", "2026-10-12T16:00");

        update(user1, id, "A거래처 미팅(연장)", "2026-10-12T14:00", "2026-10-12T17:00", "PLANNED")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("A거래처 미팅(연장)"));
    }

    @Test
    void 수정으로_다른_일정과_겹치게_되면_409() throws Exception {
        createdId(user1, "A거래처 미팅", "2026-10-12T14:00", "2026-10-12T16:00");
        long id = createdId(user1, "B거래처 미팅", "2026-10-12T16:00", "2026-10-12T17:00");

        update(user1, id, "B거래처 미팅", "2026-10-12T15:30", "2026-10-12T17:00", "PLANNED")
                .andExpect(status().isConflict());
    }

    @Test
    void 충돌_미리확인_API는_겹치는_일정을_돌려준다() throws Exception {
        createdId(user1, "A거래처 미팅", "2026-10-12T14:00", "2026-10-12T16:00");

        mockMvc.perform(get("/api/schedules/conflicts")
                        .header("Authorization", user1)
                        .param("startAt", "2026-10-12T15:00")
                        .param("endAt", "2026-10-12T17:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].title").value("A거래처 미팅"));
    }

    // ---------- 권한 ----------

    @Test
    void 다른_직원의_일정은_수정_삭제할_수_없다() throws Exception {
        long id = createdId(user1, "A거래처 미팅", "2026-10-12T14:00", "2026-10-12T16:00");

        update(user2, id, "변경", "2026-10-12T14:00", "2026-10-12T16:00", "PLANNED")
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/schedules/{id}", id).header("Authorization", user2))
                .andExpect(status().isForbidden());
        // 팀장도 팀원 일정을 수정할 수는 없다
        update(manager, id, "변경", "2026-10-12T14:00", "2026-10-12T16:00", "PLANNED")
                .andExpect(status().isForbidden());
    }

    @Test
    void 일정_상세는_본인_같은부서_팀장_관리자만_조회할_수_있다() throws Exception {
        long id = createdId(user1, "A거래처 미팅", "2026-10-12T14:00", "2026-10-12T16:00");

        detail(user1, id).andExpect(status().isOk()).andExpect(jsonPath("$.data.editable").value(true));
        detail(manager, id).andExpect(status().isOk()).andExpect(jsonPath("$.data.editable").value(false));
        detail(admin, id).andExpect(status().isOk());
        detail(user2, id).andExpect(status().isForbidden());
        detail(otherMgr, id).andExpect(status().isForbidden());
    }

    @Test
    void 본인_일정을_삭제한다() throws Exception {
        long id = createdId(user1, "A거래처 미팅", "2026-10-12T14:00", "2026-10-12T16:00");

        mockMvc.perform(delete("/api/schedules/{id}", id).header("Authorization", user1))
                .andExpect(status().isOk());
        detail(user1, id).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("SCHEDULE_NOT_FOUND"));
    }

    // ---------- 캘린더 ----------

    @Test
    void 캘린더는_기간과_겹치는_일정을_조회한다() throws Exception {
        createdId(user1, "9월말 출장", "2026-09-30T09:00", "2026-10-01T18:00"); // 기간 시작에 걸침
        createdId(user1, "10월 회의", "2026-10-15T10:00", "2026-10-15T11:00");
        createdId(user1, "11월 회의", "2026-11-01T10:00", "2026-11-01T11:00"); // to(11/1) 미포함

        calendar(user1, "ME")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].title").value("9월말 출장"));
    }

    @Test
    void 팀장은_팀_일정을_볼_수_있고_다른_팀_일정은_보이지_않는다() throws Exception {
        createdId(user1, "user1 미팅", "2026-10-12T10:00", "2026-10-12T11:00");
        createdId(user2, "user2 미팅", "2026-10-13T10:00", "2026-10-13T11:00");
        createdId(otherMgr, "다른팀 미팅", "2026-10-14T10:00", "2026-10-14T11:00");

        calendar(manager, "TEAM")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].userName").value("user1"));
    }

    @Test
    void 일반_직원은_팀_전체_캘린더를_볼_수_없다() throws Exception {
        calendar(user1, "TEAM").andExpect(status().isForbidden());
        calendar(manager, "ALL").andExpect(status().isForbidden());
        calendar(admin, "ALL").andExpect(status().isOk());
    }

    // ---------- 목록 ----------

    @Test
    void 내_일정_목록은_본인_일정만_검색조건과_페이징으로_조회한다() throws Exception {
        createdId(user1, "A거래처 미팅", "2026-10-12T10:00", "2026-10-12T11:00");
        createdId(user1, "B거래처 미팅", "2026-10-13T10:00", "2026-10-13T11:00");
        createdId(user1, "사내 교육", "2026-10-14T10:00", "2026-10-14T11:00");
        createdId(user2, "남의 미팅", "2026-10-12T10:00", "2026-10-12T11:00");

        mockMvc.perform(get("/api/schedules").header("Authorization", user1)
                        .param("keyword", "거래처").param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].title").value("B거래처 미팅")) // 최신순
                .andExpect(jsonPath("$.data.totalElements").value(2))
                .andExpect(jsonPath("$.data.totalPages").value(2));

        mockMvc.perform(get("/api/schedules").header("Authorization", user1)
                        .param("from", "2026-10-14").param("to", "2026-10-14"))
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].title").value("사내 교육"));
    }

    @Test
    void 잘못된_검색_파라미터는_400() throws Exception {
        mockMvc.perform(get("/api/schedules").header("Authorization", user1).param("type", "NOPE"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/schedules").header("Authorization", user1).param("sort", "password"))
                .andExpect(status().isBadRequest());
    }

    // ---------- helpers ----------

    private String token(Department dept, String loginId, Role role) {
        User user = userRepository.save(User.builder()
                .department(dept).loginId(loginId).password("{noop}x").name(loginId).role(role).build());
        return "Bearer " + jwtTokenProvider.createToken(
                new LoginUser(user.getId(), loginId, role, dept.getId()));
    }

    private ResultActions create(String token, String title, String start, String end, boolean allowOverlap)
            throws Exception {
        return mockMvc.perform(post("/api/schedules")
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"type":"CLIENT_VISIT","title":"%s","startAt":"%s","endAt":"%s",
                         "location":"부산","allowOverlap":%s}
                        """.formatted(title, start, end, allowOverlap)));
    }

    private long createdId(String token, String title, String start, String end) throws Exception {
        String body = create(token, title, start, end, true)
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.data.scheduleId")).longValue();
    }

    private ResultActions update(String token, long id, String title, String start, String end, String status)
            throws Exception {
        return mockMvc.perform(put("/api/schedules/{id}", id)
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"type":"CLIENT_VISIT","title":"%s","startAt":"%s","endAt":"%s","status":"%s"}
                        """.formatted(title, start, end, status)));
    }

    private ResultActions detail(String token, long id) throws Exception {
        return mockMvc.perform(get("/api/schedules/{id}", id).header("Authorization", token));
    }

    private ResultActions calendar(String token, String scope) throws Exception {
        return mockMvc.perform(get("/api/schedules/calendar").header("Authorization", token)
                .param("from", "2026-10-01").param("to", "2026-11-01").param("scope", scope));
    }
}
