package com.bizexpense.domain.dashboard;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bizexpense.domain.approval.Approval;
import com.bizexpense.domain.approval.ApprovalRepository;
import com.bizexpense.domain.approval.ApprovalTargetType;
import com.bizexpense.domain.code.ExpenseCategory;
import com.bizexpense.domain.code.ExpenseCategoryRepository;
import com.bizexpense.domain.code.PaymentMethod;
import com.bizexpense.domain.code.PaymentMethodRepository;
import com.bizexpense.domain.department.Department;
import com.bizexpense.domain.department.DepartmentRepository;
import com.bizexpense.domain.expense.Expense;
import com.bizexpense.domain.expense.ExpenseRepository;
import com.bizexpense.domain.settlement.Settlement;
import com.bizexpense.domain.settlement.SettlementAmounts;
import com.bizexpense.domain.settlement.SettlementRepository;
import com.bizexpense.domain.trip.Trip;
import com.bizexpense.domain.trip.TripRepository;
import com.bizexpense.domain.user.Role;
import com.bizexpense.domain.user.User;
import com.bizexpense.domain.user.UserRepository;
import com.bizexpense.global.security.JwtTokenProvider;
import com.bizexpense.global.security.LoginUser;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.transaction.annotation.Transactional;

/**
 * 역할별 대시보드 통계. 집계는 '이번 달' 기준이라 오늘 날짜를 기준으로 데이터를 만든다.
 * 상태 흐름은 각 도메인 테스트에서 검증하므로 여기서는 엔티티로 바로 상태를 만든다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class DashboardControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;
    @Autowired DepartmentRepository departmentRepository;
    @Autowired TripRepository tripRepository;
    @Autowired ExpenseRepository expenseRepository;
    @Autowired SettlementRepository settlementRepository;
    @Autowired ApprovalRepository approvalRepository;
    @Autowired ExpenseCategoryRepository categoryRepository;
    @Autowired PaymentMethodRepository paymentMethodRepository;
    @Autowired JwtTokenProvider jwtTokenProvider;

    final Map<String, User> users = new HashMap<>();
    final Map<String, String> tokens = new HashMap<>();
    final LocalDate today = LocalDate.now();

    ExpenseCategory transport;
    ExpenseCategory meal;
    PaymentMethod corporateCard;
    PaymentMethod personalCard;

    @BeforeEach
    void setUp() {
        Department sales1 = departmentRepository.save(new Department("영업1팀", "SALES1"));
        Department sales2 = departmentRepository.save(new Department("영업2팀", "SALES2"));
        user(sales1, "user1", Role.USER);
        user(sales1, "user2", Role.USER);
        user(sales1, "manager1", Role.MANAGER);
        user(sales2, "user3", Role.USER);
        user(sales2, "admin", Role.ADMIN);
        categoryRepository.findAll().forEach(c -> {
            if (c.getName().equals("교통비")) transport = c;
            if (c.getName().equals("식비")) meal = c;
        });
        paymentMethodRepository.findAll().forEach(p -> {
            if (p.getName().equals("법인카드")) corporateCard = p;
            if (p.getName().equals("개인카드")) personalCard = p;
        });

        // user1: 이번 달 경비 2건(교통 50,000 + 식비 20,000), 지난달 1건 30,000
        Trip trip1 = trip("user1", today.minusMonths(1).withDayOfMonth(1), today);
        expense(trip1, transport, corporateCard, today, 50_000);
        expense(trip1, meal, personalCard, today, 20_000);
        expense(trip1, meal, personalCard, today.minusMonths(1).withDayOfMonth(1), 30_000);
        // user2 (같은 부서): 이번 달 10,000
        Trip trip2 = trip("user2", today, today);
        expense(trip2, meal, personalCard, today, 10_000);
        // user3 (다른 부서): 이번 달 5,000
        Trip trip3 = trip("user3", today, today);
        expense(trip3, meal, personalCard, today, 5_000);

        // user1 정산 신청 1건 (지급 예정 20,000) + 팀장 결재 대기
        Settlement settlement = settlementRepository.save(
                new Settlement(trip1, users.get("user1"), new SettlementAmounts(70_000, 50_000, 20_000, 20_000, 2)));
        approvalRepository.save(new Approval(ApprovalTargetType.SETTLEMENT, settlement.getId(), "정산",
                users.get("user1"), users.get("manager1")));
        // user2 출장 결재: 팀장이 이번 달에 반려
        Approval rejected = new Approval(ApprovalTargetType.TRIP, trip2.getId(), "출장",
                users.get("user2"), users.get("manager1"));
        rejected.reject("일정 조정");
        approvalRepository.save(rejected);
        // user3 정산: 승인되어 지급 대기 5,000
        Settlement approved = new Settlement(trip3, users.get("user3"), new SettlementAmounts(5_000, 0, 5_000, 5_000, 1));
        approved.approve();
        settlementRepository.save(approved);
    }

    @Test
    void 직원은_본인_기준_통계를_본다() throws Exception {
        dashboard("user1")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.scope").value("ME"))
                .andExpect(jsonPath("$.data.cards", hasSize(4)))
                .andExpect(card("monthExpense", 70_000))
                .andExpect(jsonPath("$.data.cards[0].hint").value("2건"))
                .andExpect(card("payable", 20_000))
                .andExpect(card("myPending", 1))
                .andExpect(card("upcomingTrips", 1))
                // 6개월 추이: 이번 달 70,000, 지난달 30,000
                .andExpect(jsonPath("$.data.monthly", hasSize(6)))
                .andExpect(jsonPath("$.data.monthly[5].month").value(today.getMonthValue()))
                .andExpect(jsonPath("$.data.monthly[5].amount").value(70_000))
                .andExpect(jsonPath("$.data.monthly[4].amount").value(30_000))
                .andExpect(jsonPath("$.data.monthly[0].amount").value(0))
                // 이번 달 항목별: 교통비 50,000 > 식비 20,000
                .andExpect(jsonPath("$.data.categories[0].categoryName").value("교통비"))
                .andExpect(jsonPath("$.data.categories[1].amount").value(20_000));
    }

    @Test
    void 팀장은_부서_기준_통계를_본다() throws Exception {
        dashboard("manager1")
                .andExpect(jsonPath("$.data.scope").value("TEAM"))
                .andExpect(card("approvalInbox", 1))
                .andExpect(card("monthExpense", 80_000))   // user1 70,000 + user2 10,000 (다른 부서 제외)
                .andExpect(card("monthTrips", 2))
                .andExpect(card("monthRejected", 1))
                .andExpect(jsonPath("$.data.categories[0].categoryName").value("교통비"));
    }

    @Test
    void 관리자는_전체_통계를_본다() throws Exception {
        dashboard("admin")
                .andExpect(jsonPath("$.data.scope").value("ALL"))
                .andExpect(card("pendingApprovals", 1))
                .andExpect(card("monthExpense", 85_000))
                .andExpect(card("toPay", 5_000))
                .andExpect(card("monthSettled", 0));
    }

    @Test
    void 지급_완료하면_이번_달_지급_완료에_잡힌다() throws Exception {
        settlementRepository.findAll().stream()
                .filter(s -> s.getStatus().name().equals("APPROVED"))
                .forEach(Settlement::complete);

        dashboard("admin")
                .andExpect(card("toPay", 0))
                .andExpect(card("monthSettled", 5_000));
    }

    @Test
    void 로그인하지_않으면_401() throws Exception {
        mockMvc.perform(get("/api/dashboard")).andExpect(status().isUnauthorized());
    }

    // ---------- helpers ----------

    private ResultActions dashboard(String who) throws Exception {
        return mockMvc.perform(get("/api/dashboard").header("Authorization", tokens.get(who)));
    }

    /** key 로 카드를 찾아 값 확인 */
    private static ResultMatcher card(String key, long value) {
        return jsonPath("$.data.cards[?(@.key == '" + key + "')].value").value(contains((int) value));
    }

    private void user(Department dept, String loginId, Role role) {
        User user = userRepository.save(User.builder()
                .department(dept).loginId(loginId).password("{noop}x").name(loginId).role(role).build());
        users.put(loginId, user);
        tokens.put(loginId, "Bearer " + jwtTokenProvider.createToken(
                new LoginUser(user.getId(), loginId, role, dept.getId())));
    }

    private Trip trip(String owner, LocalDate start, LocalDate end) {
        Trip trip = Trip.builder().user(users.get(owner)).title(owner + " 출장").destination("부산")
                .startDate(start).endDate(end).expectedAmount(300_000).build();
        trip.request();
        trip.approve();
        trip.start();
        return tripRepository.save(trip);
    }

    private void expense(Trip trip, ExpenseCategory category, PaymentMethod method, LocalDate usedAt, long amount) {
        expenseRepository.save(Expense.builder().trip(trip).user(trip.getUser()).category(category)
                .paymentMethod(method).usedAt(usedAt).storeName("가게").amount(amount).build());
    }
}
