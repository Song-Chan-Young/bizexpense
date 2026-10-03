package com.bizexpense.domain.trip;

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

/** 출장 + 결재 + 일정 연결 통합 테스트 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class TripControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;
    @Autowired DepartmentRepository departmentRepository;
    @Autowired JwtTokenProvider jwtTokenProvider;

    String user1;     // 영업1팀 직원
    String user2;     // 영업1팀 직원
    String manager;   // 영업1팀 팀장
    String otherMgr;  // 영업2팀 팀장
    String admin;     // 경영지원팀 관리자

    @BeforeEach
    void setUp() {
        Department sales1 = departmentRepository.save(new Department("영업1팀", "SALES1"));
        Department sales2 = departmentRepository.save(new Department("영업2팀", "SALES2"));
        Department mgt = departmentRepository.save(new Department("경영지원팀", "MGT"));
        user1 = token(sales1, "user1", Role.USER);
        user2 = token(sales1, "user2", Role.USER);
        manager = token(sales1, "manager1", Role.MANAGER);
        otherMgr = token(sales2, "manager2", Role.MANAGER);
        admin = token(mgt, "admin", Role.ADMIN);
    }

    // ---------- 등록 / 수정 ----------

    @Test
    void 출장을_임시저장한다() throws Exception {
        create(user1, false)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andExpect(jsonPath("$.data.days").value(3))
                .andExpect(jsonPath("$.data.actions.edit").value(true))
                .andExpect(jsonPath("$.data.actions.delete").value(true))
                .andExpect(jsonPath("$.data.actions.start").value(false));
    }

    @Test
    void 종료일이_시작일보다_빠르면_400() throws Exception {
        mockMvc.perform(post("/api/trips").header("Authorization", user1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(tripJson("부산 출장", "2026-10-14", "2026-10-12", false)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_TRIP_PERIOD"));
    }

    @Test
    void 등록과_동시에_신청하면_같은_부서_팀장에게_결재가_올라간다() throws Exception {
        long tripId = createdId(user1, true);

        detail(user1, tripId)
                .andExpect(jsonPath("$.data.trip.status").value("REQUESTED"))
                .andExpect(jsonPath("$.data.approvals", hasSize(1)))
                .andExpect(jsonPath("$.data.approvals[0].approverName").value("manager1"))
                .andExpect(jsonPath("$.data.approvals[0].status").value("PENDING"));

        inbox(manager).andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].processable").value(true));
        inbox(otherMgr).andExpect(jsonPath("$.data.content", hasSize(0)));
    }

    @Test
    void 팀장의_출장은_관리자에게_결재가_올라간다() throws Exception {
        long tripId = createdId(manager, true);

        detail(manager, tripId).andExpect(jsonPath("$.data.approvals[0].approverName").value("admin"));
    }

    @Test
    void 신청된_출장은_수정할_수_없다() throws Exception {
        long tripId = createdId(user1, true);

        update(user1, tripId, "2026-10-12", "2026-10-14")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("INVALID_TRIP_STATUS"))
                .andExpect(jsonPath("$.error.message", containsString("신청")));
    }

    @Test
    void 다른_직원의_출장은_수정하거나_신청할_수_없다() throws Exception {
        long tripId = createdId(user1, false);

        update(user2, tripId, "2026-10-12", "2026-10-14").andExpect(status().isForbidden());
        action(user2, tripId, "request").andExpect(status().isForbidden());
        action(manager, tripId, "request").andExpect(status().isForbidden());
    }

    // ---------- 결재 ----------

    @Test
    void 팀장이_승인하면_출장이_승인된다() throws Exception {
        long tripId = createdId(user1, true);
        long approvalId = pendingApprovalId(manager);

        approve(manager, approvalId, "잘 다녀오세요")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APPROVED"));

        detail(user1, tripId)
                .andExpect(jsonPath("$.data.trip.status").value("APPROVED"))
                .andExpect(jsonPath("$.data.trip.actions.start").value(true))
                .andExpect(jsonPath("$.data.approvals[0].comment").value("잘 다녀오세요"));
    }

    @Test
    void 반려하려면_사유가_필요하다() throws Exception {
        createdId(user1, true);
        long approvalId = pendingApprovalId(manager);

        reject(manager, approvalId, "").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("REJECT_COMMENT_REQUIRED"));
    }

    @Test
    void 반려되면_사유를_확인하고_수정후_재신청한다() throws Exception {
        long tripId = createdId(user1, true);
        reject(manager, pendingApprovalId(manager), "예상 경비가 너무 큽니다").andExpect(status().isOk());

        detail(user1, tripId)
                .andExpect(jsonPath("$.data.trip.status").value("REJECTED"))
                .andExpect(jsonPath("$.data.trip.actions.edit").value(true))
                .andExpect(jsonPath("$.data.approvals[0].comment").value("예상 경비가 너무 큽니다"));

        update(user1, tripId, "2026-10-12", "2026-10-13").andExpect(status().isOk());
        action(user1, tripId, "request").andExpect(jsonPath("$.data.status").value("REQUESTED"));

        // 결재 이력: 새 대기 건 + 이전 반려 건
        detail(user1, tripId)
                .andExpect(jsonPath("$.data.approvals", hasSize(2)))
                .andExpect(jsonPath("$.data.approvals[0].status").value("PENDING"))
                .andExpect(jsonPath("$.data.approvals[1].status").value("REJECTED"));
    }

    @Test
    void 다른_팀_팀장은_결재할_수_없다() throws Exception {
        createdId(user1, true);
        long approvalId = pendingApprovalId(manager);

        approve(otherMgr, approvalId, null).andExpect(status().isForbidden());
        approve(admin, approvalId, null).andExpect(status().isForbidden()); // 지정된 결재자만
    }

    @Test
    void 이미_처리된_결재는_다시_처리할_수_없다() throws Exception {
        createdId(user1, true);
        long approvalId = pendingApprovalId(manager);
        approve(manager, approvalId, null).andExpect(status().isOk());

        reject(manager, approvalId, "취소").andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("APPROVAL_ALREADY_PROCESSED"));
    }

    @Test
    void 결재_대기중에_취소하면_결재_건도_취소된다() throws Exception {
        long tripId = createdId(user1, true);

        action(user1, tripId, "cancel").andExpect(jsonPath("$.data.status").value("CANCELLED"));

        detail(user1, tripId).andExpect(jsonPath("$.data.approvals[0].status").value("CANCELLED"));
        inbox(manager).andExpect(jsonPath("$.data.content[0].processable").value(false));
    }

    @Test
    void 승인_시작_완료() throws Exception {
        long tripId = createdId(user1, true);
        approve(manager, pendingApprovalId(manager), null);

        action(user1, tripId, "start").andExpect(jsonPath("$.data.status").value("IN_PROGRESS"));
        action(user1, tripId, "cancel").andExpect(status().isConflict());
        action(user1, tripId, "complete").andExpect(jsonPath("$.data.status").value("COMPLETED"));
    }

    @Test
    void 임시저장_건만_삭제할_수_있다() throws Exception {
        long draft = createdId(user1, false);
        long requested = createdId(user1, true);

        mockMvc.perform(delete("/api/trips/{id}", requested).header("Authorization", user1))
                .andExpect(status().isConflict());
        mockMvc.perform(delete("/api/trips/{id}", draft).header("Authorization", user1))
                .andExpect(status().isOk());
        detail(user1, draft).andExpect(status().isNotFound());
    }

    // ---------- 일정 연결 ----------

    @Test
    void 출장_기간_안의_일정을_연결한다() throws Exception {
        long tripId = createdId(user1, false); // 10/12 ~ 10/14

        schedule(user1, tripId, "A거래처 미팅", "2026-10-12T14:00", "2026-10-12T16:00")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.tripId").value(tripId))
                .andExpect(jsonPath("$.data.tripTitle").value("부산 거래처 방문"));
        schedule(user1, tripId, "B거래처 미팅", "2026-10-13T10:00", "2026-10-13T11:00");

        detail(user1, tripId)
                .andExpect(jsonPath("$.data.schedules", hasSize(2)))
                .andExpect(jsonPath("$.data.schedules[0].title").value("A거래처 미팅"));
    }

    @Test
    void 출장_기간_밖의_일정은_연결할_수_없다() throws Exception {
        long tripId = createdId(user1, false);

        schedule(user1, tripId, "전날 회의", "2026-10-11T14:00", "2026-10-11T16:00")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("SCHEDULE_OUT_OF_TRIP_PERIOD"));
    }

    @Test
    void 다른_직원의_출장에는_일정을_연결할_수_없다() throws Exception {
        long tripId = createdId(user1, false);

        schedule(user2, tripId, "미팅", "2026-10-12T14:00", "2026-10-12T16:00")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("TRIP_NOT_LINKABLE"));
    }

    @Test
    void 출장_기간을_줄여_연결된_일정이_밖으로_나가면_수정할_수_없다() throws Exception {
        long tripId = createdId(user1, false);
        schedule(user1, tripId, "마지막날 미팅", "2026-10-14T10:00", "2026-10-14T11:00");

        update(user1, tripId, "2026-10-12", "2026-10-13")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("TRIP_SCHEDULE_OUT_OF_RANGE"));
    }

    @Test
    void 출장을_취소하면_연결된_일정도_취소된다() throws Exception {
        long tripId = createdId(user1, false);
        schedule(user1, tripId, "A거래처 미팅", "2026-10-12T14:00", "2026-10-12T16:00");

        action(user1, tripId, "cancel");

        detail(user1, tripId).andExpect(jsonPath("$.data.schedules[0].status").value("CANCELLED"));
    }

    @Test
    void 취소된_출장에는_일정을_연결할_수_없고_선택목록에도_나오지_않는다() throws Exception {
        long cancelled = createdId(user1, false);
        long active = createdId(user1, false);
        action(user1, cancelled, "cancel");

        schedule(user1, cancelled, "미팅", "2026-10-12T14:00", "2026-10-12T16:00")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("TRIP_NOT_LINKABLE"));
        mockMvc.perform(get("/api/trips/schedulable").header("Authorization", user1))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].tripId").value(active));
    }

    @Test
    void 임시저장_출장을_삭제하면_일정은_남고_연결만_해제된다() throws Exception {
        long tripId = createdId(user1, false);
        String body = schedule(user1, tripId, "미팅", "2026-10-12T14:00", "2026-10-12T16:00")
                .andReturn().getResponse().getContentAsString();
        long scheduleId = ((Number) JsonPath.read(body, "$.data.scheduleId")).longValue();

        mockMvc.perform(delete("/api/trips/{id}", tripId).header("Authorization", user1))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/schedules/{id}", scheduleId).header("Authorization", user1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tripId").doesNotExist());
    }

    // ---------- 조회 권한 / 목록 ----------

    @Test
    void 출장_상세는_본인_같은부서_팀장_관리자만_조회할_수_있다() throws Exception {
        long tripId = createdId(user1, false);

        detail(user1, tripId).andExpect(status().isOk());
        detail(manager, tripId).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.trip.actions.edit").value(false));
        detail(admin, tripId).andExpect(status().isOk());
        detail(user2, tripId).andExpect(status().isForbidden());
        detail(otherMgr, tripId).andExpect(status().isForbidden());
    }

    @Test
    void 목록은_범위와_검색조건으로_조회한다() throws Exception {
        createdId(user1, false);
        createdId(user2, true);
        createdId(otherMgr, false);

        list(user1, "scope=ME").andExpect(jsonPath("$.data.totalElements").value(1));
        list(manager, "scope=TEAM").andExpect(jsonPath("$.data.totalElements").value(2));
        list(manager, "scope=TEAM&status=REQUESTED")
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].userName").value("user2"));
        list(admin, "scope=ALL").andExpect(jsonPath("$.data.totalElements").value(3));
        list(admin, "scope=ALL&from=2026-10-15").andExpect(jsonPath("$.data.totalElements").value(0));
        list(user1, "scope=TEAM").andExpect(status().isForbidden());
    }

    // ---------- helpers ----------

    private String token(Department dept, String loginId, Role role) {
        User user = userRepository.save(User.builder()
                .department(dept).loginId(loginId).password("{noop}x").name(loginId).role(role).build());
        return "Bearer " + jwtTokenProvider.createToken(new LoginUser(user.getId(), loginId, role, dept.getId()));
    }

    private String tripJson(String title, String start, String end, boolean submit) {
        return """
                {"title":"%s","purpose":"거래처 미팅","destination":"부산","startDate":"%s","endDate":"%s",
                 "expectedAmount":300000,"submit":%s}
                """.formatted(title, start, end, submit);
    }

    private ResultActions create(String token, boolean submit) throws Exception {
        return mockMvc.perform(post("/api/trips").header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(tripJson("부산 거래처 방문", "2026-10-12", "2026-10-14", submit)));
    }

    private long createdId(String token, boolean submit) throws Exception {
        String body = create(token, submit).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.data.tripId")).longValue();
    }

    private ResultActions update(String token, long id, String start, String end) throws Exception {
        return mockMvc.perform(put("/api/trips/{id}", id).header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(tripJson("부산 거래처 방문", start, end, false)));
    }

    private ResultActions action(String token, long id, String action) throws Exception {
        return mockMvc.perform(post("/api/trips/{id}/{action}", id, action).header("Authorization", token));
    }

    private ResultActions detail(String token, long id) throws Exception {
        return mockMvc.perform(get("/api/trips/{id}", id).header("Authorization", token));
    }

    private ResultActions list(String token, String query) throws Exception {
        return mockMvc.perform(get("/api/trips?" + query).header("Authorization", token));
    }

    private ResultActions inbox(String token) throws Exception {
        return mockMvc.perform(get("/api/approvals").header("Authorization", token));
    }

    private long pendingApprovalId(String approverToken) throws Exception {
        String body = mockMvc.perform(get("/api/approvals").header("Authorization", approverToken)
                        .param("status", "PENDING"))
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.data.content[0].approvalId")).longValue();
    }

    private ResultActions approve(String token, long approvalId, String comment) throws Exception {
        return mockMvc.perform(post("/api/approvals/{id}/approve", approvalId).header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(comment == null ? "{}" : "{\"comment\":\"%s\"}".formatted(comment)));
    }

    private ResultActions reject(String token, long approvalId, String comment) throws Exception {
        return mockMvc.perform(post("/api/approvals/{id}/reject", approvalId).header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"comment\":\"%s\"}".formatted(comment)));
    }

    private ResultActions schedule(String token, long tripId, String title, String start, String end)
            throws Exception {
        return mockMvc.perform(post("/api/schedules").header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"type":"CLIENT_VISIT","title":"%s","startAt":"%s","endAt":"%s",
                         "tripId":%d,"allowOverlap":true}
                        """.formatted(title, start, end, tripId)));
    }
}
