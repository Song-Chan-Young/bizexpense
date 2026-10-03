package com.bizexpense.domain.settlement;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bizexpense.domain.code.ExpenseCategoryRepository;
import com.bizexpense.domain.code.PaymentMethodRepository;
import com.bizexpense.domain.department.Department;
import com.bizexpense.domain.department.DepartmentRepository;
import com.bizexpense.domain.trip.Trip;
import com.bizexpense.domain.trip.TripRepository;
import com.bizexpense.domain.user.Role;
import com.bizexpense.domain.user.User;
import com.bizexpense.domain.user.UserRepository;
import com.bizexpense.global.security.JwtTokenProvider;
import com.bizexpense.global.security.LoginUser;
import com.jayway.jsonpath.JsonPath;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
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

/** 정산 신청 → 팀장 결재(승인/반려) → 재신청 → 관리자 지급 처리 통합 테스트 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class SettlementControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;
    @Autowired DepartmentRepository departmentRepository;
    @Autowired TripRepository tripRepository;
    @Autowired ExpenseCategoryRepository categoryRepository;
    @Autowired PaymentMethodRepository paymentMethodRepository;
    @Autowired JwtTokenProvider jwtTokenProvider;

    final Map<String, User> users = new HashMap<>();
    final Map<String, String> tokens = new HashMap<>();
    final Map<String, Long> payment = new HashMap<>();
    long categoryId;

    long completedTrip;   // user1, 완료
    long inProgressTrip;  // user1, 진행중

    @BeforeEach
    void setUp() {
        Department sales1 = departmentRepository.save(new Department("영업1팀", "SALES1"));
        Department sales2 = departmentRepository.save(new Department("영업2팀", "SALES2"));
        user(sales1, "user1", Role.USER);
        user(sales1, "user2", Role.USER);
        user(sales1, "manager1", Role.MANAGER);
        user(sales2, "manager2", Role.MANAGER);
        user(sales2, "admin", Role.ADMIN);
        categoryId = categoryRepository.findAll().getFirst().getId();
        paymentMethodRepository.findAll().forEach(p -> payment.put(p.getName(), p.getId()));

        completedTrip = trip("user1", true);
        inProgressTrip = trip("user1", false);
    }

    @Test
    void 정산을_신청하면_금액이_계산되고_팀장에게_결재가_올라간다() throws Exception {
        long e1 = expense(completedTrip, "법인카드", 63_000);
        expense(completedTrip, "개인카드", 60_000);
        expense(completedTrip, "현금", 30_000);

        // 신청 전 미리보기
        tripSettlement("user1")
                .andExpect(jsonPath("$.data.canRequest").value(true))
                .andExpect(jsonPath("$.data.preview.totalAmount").value(153_000))
                .andExpect(jsonPath("$.data.preview.payableAmount").value(90_000));

        long settlementId = requestSettlement("user1", completedTrip)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("REQUESTED"))
                .andExpect(jsonPath("$.data.totalAmount").value(153_000))
                .andExpect(jsonPath("$.data.corporateAmount").value(63_000))
                .andExpect(jsonPath("$.data.payableAmount").value(90_000))
                .andExpect(jsonPath("$.data.expenseCount").value(3))
                .andReturn().getResponse().getContentAsString().transform(this::idOf);

        // 경비는 정산신청 상태로 잠긴다
        expenseDetail("user1", e1).andExpect(jsonPath("$.data.status").value("REQUESTED"))
                .andExpect(jsonPath("$.data.editable").value(false))
                .andExpect(jsonPath("$.data.settlementId").value(settlementId));
        updateExpense(e1, 1_000).andExpect(status().isConflict());

        mockMvc.perform(get("/api/approvals?status=PENDING&targetType=SETTLEMENT")
                        .header("Authorization", tokens.get("manager1")))
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].title").value("부산 출장 정산 (지급 90,000원)"));
    }

    @Test
    void 완료되지_않은_출장은_정산을_신청할_수_없다() throws Exception {
        expense(inProgressTrip, "개인카드", 10_000);

        requestSettlement("user1", inProgressTrip)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("TRIP_NOT_SETTLEABLE"));
    }

    @Test
    void 경비가_없으면_정산을_신청할_수_없다() throws Exception {
        requestSettlement("user1", completedTrip)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("NO_EXPENSES_TO_SETTLE"));
    }

    @Test
    void 진행중인_정산이_있으면_다시_신청할_수_없다() throws Exception {
        expense(completedTrip, "개인카드", 10_000);
        requestSettlement("user1", completedTrip).andExpect(status().isCreated());

        requestSettlement("user1", completedTrip)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("SETTLEMENT_IN_PROGRESS"));
    }

    @Test
    void 다른_직원의_출장은_정산을_신청할_수_없다() throws Exception {
        expense(completedTrip, "개인카드", 10_000);
        requestSettlement("user2", completedTrip).andExpect(status().isForbidden());
    }

    @Test
    void 승인_후_관리자가_지급완료하면_경비가_정산완료된다() throws Exception {
        long e1 = expense(completedTrip, "개인카드", 40_000);
        long settlementId = createdSettlement();

        approve("manager1", settlementId).andExpect(status().isOk());
        settlementDetail("user1", settlementId)
                .andExpect(jsonPath("$.data.settlement.status").value("APPROVED"))
                .andExpect(jsonPath("$.data.expenses[0].status").value("APPROVED"));

        // 팀장은 지급 처리 불가, 관리자만
        complete("manager1", settlementId).andExpect(status().isForbidden());
        complete("admin", settlementId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SETTLED"))
                .andExpect(jsonPath("$.data.settledAt").isNotEmpty());

        expenseDetail("user1", e1).andExpect(jsonPath("$.data.status").value("SETTLED"));
        complete("admin", settlementId).andExpect(status().isConflict());
    }

    @Test
    void 승인_전에는_지급완료할_수_없다() throws Exception {
        expense(completedTrip, "개인카드", 40_000);
        long settlementId = createdSettlement();

        complete("admin", settlementId)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("INVALID_SETTLEMENT_STATUS"));
    }

    @Test
    void 반려되면_경비를_수정하고_새_경비까지_포함해_재신청한다() throws Exception {
        long hotel = expense(completedTrip, "개인카드", 150_000);
        long settlementId = createdSettlement();

        reject("manager1", settlementId, "숙박비 한도(1박 10만원) 초과").andExpect(status().isOk());
        settlementDetail("user1", settlementId)
                .andExpect(jsonPath("$.data.settlement.status").value("REJECTED"))
                .andExpect(jsonPath("$.data.settlement.actions.resubmit").value(true))
                .andExpect(jsonPath("$.data.approvals[0].comment").value("숙박비 한도(1박 10만원) 초과"));

        // 반려된 경비는 다시 수정 가능
        updateExpense(hotel, 100_000).andExpect(status().isOk());
        // 반려 상태에서 새로 등록한 경비도 재신청에 포함
        expense(completedTrip, "현금", 12_000);

        mockMvc.perform(post("/api/settlements/{id}/request", settlementId)
                        .header("Authorization", tokens.get("user1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("REQUESTED"))
                .andExpect(jsonPath("$.data.payableAmount").value(112_000))
                .andExpect(jsonPath("$.data.expenseCount").value(2));

        settlementDetail("user1", settlementId)
                .andExpect(jsonPath("$.data.expenses", hasSize(2)))
                .andExpect(jsonPath("$.data.approvals", hasSize(2)));
    }

    @Test
    void 반려된_경비를_삭제하면_재신청에서_빠진다() throws Exception {
        long a = expense(completedTrip, "개인카드", 30_000);
        expense(completedTrip, "개인카드", 20_000);
        long settlementId = createdSettlement();
        reject("manager1", settlementId, "중복 경비 확인 필요");

        mockMvc.perform(delete("/api/expenses/{id}", a).header("Authorization", tokens.get("user1")))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/settlements/{id}/request", settlementId)
                        .header("Authorization", tokens.get("user1")))
                .andExpect(jsonPath("$.data.payableAmount").value(20_000))
                .andExpect(jsonPath("$.data.expenseCount").value(1));
    }

    @Test
    void 다른_팀_팀장은_정산을_결재할_수_없다() throws Exception {
        expense(completedTrip, "개인카드", 10_000);
        long settlementId = createdSettlement();

        long approvalId = pendingApprovalId("manager1");
        mockMvc.perform(post("/api/approvals/{id}/approve", approvalId)
                        .header("Authorization", tokens.get("manager2"))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
        settlementDetail("user1", settlementId).andExpect(jsonPath("$.data.settlement.status").value("REQUESTED"));
    }

    @Test
    void 정산완료_후_추가된_경비는_새_정산으로_신청한다_이미_정산된_경비는_다시_포함되지_않는다() throws Exception {
        expense(completedTrip, "개인카드", 10_000);
        long first = createdSettlement();
        approve("manager1", first);
        complete("admin", first);

        expense(completedTrip, "현금", 5_000);
        requestSettlement("user1", completedTrip)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.totalAmount").value(5_000))
                .andExpect(jsonPath("$.data.expenseCount").value(1));

        tripSettlement("user1").andExpect(jsonPath("$.data.settlements", hasSize(2)));
    }

    @Test
    void 정산_목록과_조회_권한() throws Exception {
        expense(completedTrip, "개인카드", 10_000);
        long settlementId = createdSettlement();

        mockMvc.perform(get("/api/settlements").header("Authorization", tokens.get("user1")))
                .andExpect(jsonPath("$.data.totalElements").value(1));
        mockMvc.perform(get("/api/settlements?scope=TEAM").header("Authorization", tokens.get("manager1")))
                .andExpect(jsonPath("$.data.totalElements").value(1));
        mockMvc.perform(get("/api/settlements?scope=ALL&status=APPROVED").header("Authorization", tokens.get("admin")))
                .andExpect(jsonPath("$.data.totalElements").value(0));
        mockMvc.perform(get("/api/settlements?scope=TEAM").header("Authorization", tokens.get("user1")))
                .andExpect(status().isForbidden());

        settlementDetail("user2", settlementId).andExpect(status().isForbidden());
        settlementDetail("manager1", settlementId).andExpect(status().isOk());
        settlementDetail("manager2", settlementId).andExpect(status().isForbidden());
    }

    // ---------- helpers ----------

    private void user(Department dept, String loginId, Role role) {
        User user = userRepository.save(User.builder()
                .department(dept).loginId(loginId).password("{noop}x").name(loginId).role(role).build());
        users.put(loginId, user);
        tokens.put(loginId, "Bearer " + jwtTokenProvider.createToken(
                new LoginUser(user.getId(), loginId, role, dept.getId())));
    }

    private long trip(String owner, boolean completed) {
        Trip trip = Trip.builder().user(users.get(owner)).title("부산 출장").destination("부산")
                .startDate(LocalDate.of(2026, 10, 12)).endDate(LocalDate.of(2026, 10, 14))
                .expectedAmount(300_000).build();
        trip.request();
        trip.approve();
        trip.start();
        if (completed) {
            trip.complete();
        }
        return tripRepository.save(trip).getId();
    }

    private long expense(long tripId, String pay, long amount) throws Exception {
        return mockMvc.perform(post("/api/expenses").header("Authorization", tokens.get("user1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(expenseJson(tripId, pay, amount)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString()
                .transform(body -> ((Number) JsonPath.read(body, "$.data.expenseId")).longValue());
    }

    private String expenseJson(long tripId, String pay, long amount) {
        return """
                {"tripId":%d,"categoryId":%d,"paymentMethodId":%d,"usedAt":"2026-10-13","storeName":"사용처","amount":%d}
                """.formatted(tripId, categoryId, payment.get(pay), amount);
    }

    private ResultActions updateExpense(long expenseId, long amount) throws Exception {
        return mockMvc.perform(put("/api/expenses/{id}", expenseId).header("Authorization", tokens.get("user1"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(expenseJson(completedTrip, "개인카드", amount)));
    }

    private ResultActions expenseDetail(String who, long id) throws Exception {
        return mockMvc.perform(get("/api/expenses/{id}", id).header("Authorization", tokens.get(who)));
    }

    private ResultActions tripSettlement(String who) throws Exception {
        return mockMvc.perform(get("/api/trips/{id}/settlement", completedTrip).header("Authorization", tokens.get(who)));
    }

    private ResultActions requestSettlement(String who, long tripId) throws Exception {
        return mockMvc.perform(post("/api/trips/{id}/settlement", tripId).header("Authorization", tokens.get(who)));
    }

    private long createdSettlement() throws Exception {
        return requestSettlement("user1", completedTrip).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString().transform(this::idOf);
    }

    private long idOf(String body) {
        return ((Number) JsonPath.read(body, "$.data.settlementId")).longValue();
    }

    private ResultActions settlementDetail(String who, long id) throws Exception {
        return mockMvc.perform(get("/api/settlements/{id}", id).header("Authorization", tokens.get(who)));
    }

    private long pendingApprovalId(String approver) throws Exception {
        String body = mockMvc.perform(get("/api/approvals?status=PENDING").header("Authorization", tokens.get(approver)))
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.data.content[0].approvalId")).longValue();
    }

    private ResultActions approve(String approver, long settlementId) throws Exception {
        return mockMvc.perform(post("/api/approvals/{id}/approve", pendingApprovalId(approver))
                .header("Authorization", tokens.get(approver))
                .contentType(MediaType.APPLICATION_JSON).content("{}"));
    }

    private ResultActions reject(String approver, long settlementId, String comment) throws Exception {
        return mockMvc.perform(post("/api/approvals/{id}/reject", pendingApprovalId(approver))
                .header("Authorization", tokens.get(approver))
                .contentType(MediaType.APPLICATION_JSON).content("{\"comment\":\"%s\"}".formatted(comment)));
    }

    private ResultActions complete(String who, long settlementId) throws Exception {
        return mockMvc.perform(post("/api/settlements/{id}/complete", settlementId)
                .header("Authorization", tokens.get(who)));
    }
}
