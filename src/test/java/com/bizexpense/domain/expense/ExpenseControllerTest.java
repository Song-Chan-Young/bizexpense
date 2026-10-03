package com.bizexpense.domain.expense;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bizexpense.domain.code.ExpenseCategory;
import com.bizexpense.domain.code.ExpenseCategoryRepository;
import com.bizexpense.domain.code.PaymentMethod;
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

/** 경비 + 기준 코드 통합 테스트. 비용 항목/결제 수단은 CodeDataInitializer 가 넣은 기본 데이터를 쓴다. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ExpenseControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;
    @Autowired DepartmentRepository departmentRepository;
    @Autowired TripRepository tripRepository;
    @Autowired ExpenseCategoryRepository categoryRepository;
    @Autowired PaymentMethodRepository paymentMethodRepository;
    @Autowired JwtTokenProvider jwtTokenProvider;

    final Map<String, User> users = new HashMap<>();
    final Map<String, String> tokens = new HashMap<>();
    final Map<String, Long> category = new HashMap<>();
    final Map<String, Long> payment = new HashMap<>();

    long trip1;          // user1, 진행중, 10/12 ~ 10/14
    long approvedTrip;   // user1, 승인 (아직 시작 전)
    long user2Trip;      // user2, 진행중

    @BeforeEach
    void setUp() {
        Department sales1 = departmentRepository.save(new Department("영업1팀", "SALES1"));
        Department sales2 = departmentRepository.save(new Department("영업2팀", "SALES2"));
        user(sales1, "user1", Role.USER);
        user(sales1, "user2", Role.USER);
        user(sales1, "manager1", Role.MANAGER);
        user(sales2, "manager2", Role.MANAGER);
        user(sales2, "admin", Role.ADMIN);

        categoryRepository.findAll().forEach(c -> category.put(c.getName(), c.getId()));
        paymentMethodRepository.findAll().forEach(p -> payment.put(p.getName(), p.getId()));

        trip1 = trip("user1", true);
        approvedTrip = trip("user1", false);
        user2Trip = trip("user2", true);
    }

    // ---------- 등록 ----------

    @Test
    void 진행중인_출장에_경비를_등록한다() throws Exception {
        create("user1", trip1, "교통비", "법인카드", "2026-10-12", "KTX", 59_800)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andExpect(jsonPath("$.data.categoryName").value("교통비"))
                .andExpect(jsonPath("$.data.corporate").value(true))
                .andExpect(jsonPath("$.data.tripTitle").value("부산 출장"))
                .andExpect(jsonPath("$.data.editable").value(true));
    }

    @Test
    void 시작_전_출장에는_경비를_등록할_수_없다() throws Exception {
        create("user1", approvedTrip, "교통비", "법인카드", "2026-10-12", "KTX", 59_800)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("TRIP_NOT_EXPENSABLE"));
    }

    @Test
    void 다른_직원의_출장에는_경비를_등록할_수_없다() throws Exception {
        create("user1", user2Trip, "식비", "개인카드", "2026-10-12", "식당", 10_000)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("TRIP_NOT_EXPENSABLE"));
    }

    @Test
    void 출장_기간을_벗어난_사용일은_400() throws Exception {
        create("user1", trip1, "식비", "개인카드", "2026-10-20", "식당", 10_000)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("EXPENSE_DATE_OUT_OF_TRIP"));
    }

    @Test
    void 금액이_0원_이하면_400() throws Exception {
        create("user1", trip1, "식비", "개인카드", "2026-10-12", "식당", 0)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_INPUT"));
    }

    @Test
    void 사용_중지된_비용_항목으로는_등록할_수_없다() throws Exception {
        ExpenseCategory etc = categoryRepository.findById(category.get("기타")).orElseThrow();
        etc.update("기타", false);

        create("user1", trip1, "기타", "개인카드", "2026-10-12", "문구점", 3_000)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INACTIVE_CODE"));
    }

    // ---------- 수정 / 삭제 ----------

    @Test
    void 본인_경비를_수정한다() throws Exception {
        long id = createdId("user1", trip1, "식비", "개인카드", "2026-10-12", "식당", 10_000);

        mockMvc.perform(put("/api/expenses/{id}", id).header("Authorization", tokens.get("user1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(trip1, "식비", "법인카드", "2026-10-13", "고깃집", 45_000)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.amount").value(45_000))
                .andExpect(jsonPath("$.data.paymentMethodName").value("법인카드"));
    }

    @Test
    void 다른_직원의_경비는_수정_삭제할_수_없다() throws Exception {
        long id = createdId("user1", trip1, "식비", "개인카드", "2026-10-12", "식당", 10_000);

        mockMvc.perform(put("/api/expenses/{id}", id).header("Authorization", tokens.get("manager1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(trip1, "식비", "개인카드", "2026-10-12", "식당", 1)))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/expenses/{id}", id).header("Authorization", tokens.get("user2")))
                .andExpect(status().isForbidden());
    }

    @Test
    void 본인_경비를_삭제한다() throws Exception {
        long id = createdId("user1", trip1, "식비", "개인카드", "2026-10-12", "식당", 10_000);

        mockMvc.perform(delete("/api/expenses/{id}", id).header("Authorization", tokens.get("user1")))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/expenses/{id}", id).header("Authorization", tokens.get("user1")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("EXPENSE_NOT_FOUND"));
    }

    @Test
    void 경비_상세는_본인_같은부서_팀장_관리자만_조회한다() throws Exception {
        long id = createdId("user1", trip1, "식비", "개인카드", "2026-10-12", "식당", 10_000);

        detail("user1", id).andExpect(status().isOk());
        detail("manager1", id).andExpect(status().isOk()).andExpect(jsonPath("$.data.editable").value(false));
        detail("admin", id).andExpect(status().isOk());
        detail("user2", id).andExpect(status().isForbidden());
        detail("manager2", id).andExpect(status().isForbidden());
    }

    // ---------- 검색 / 집계 ----------

    @Test
    void 검색_조건과_합계() throws Exception {
        createdId("user1", trip1, "교통비", "법인카드", "2026-10-12", "KTX", 59_800);
        createdId("user1", trip1, "식비", "개인카드", "2026-10-12", "부산 돼지국밥", 9_000);
        createdId("user1", trip1, "숙박비", "개인카드", "2026-10-13", "해운대 호텔", 120_000);
        createdId("user1", trip1, "식비", "현금", "2026-10-14", "카페", 4_500);
        createdId("user2", user2Trip, "식비", "개인카드", "2026-10-12", "남의 식당", 8_000);

        search("user1", "").andExpect(jsonPath("$.data.page.totalElements").value(4))
                .andExpect(jsonPath("$.data.totalAmount").value(193_300));
        search("user1", "categoryId=" + category.get("식비"))
                .andExpect(jsonPath("$.data.page.totalElements").value(2))
                .andExpect(jsonPath("$.data.totalAmount").value(13_500));
        search("user1", "paymentMethodId=" + payment.get("개인카드") + "&minAmount=10000")
                .andExpect(jsonPath("$.data.page.content", hasSize(1)))
                .andExpect(jsonPath("$.data.page.content[0].storeName").value("해운대 호텔"));
        search("user1", "from=2026-10-13&to=2026-10-14")
                .andExpect(jsonPath("$.data.page.totalElements").value(2));
        search("user1", "keyword=국밥").andExpect(jsonPath("$.data.page.totalElements").value(1));
        // 합계는 현재 페이지가 아니라 조건 전체 기준
        search("user1", "size=1").andExpect(jsonPath("$.data.page.content", hasSize(1)))
                .andExpect(jsonPath("$.data.totalAmount").value(193_300));
        // 최신 사용일 순
        search("user1", "").andExpect(jsonPath("$.data.page.content[0].storeName").value("카페"));
    }

    @Test
    void 조회_범위_권한() throws Exception {
        createdId("user1", trip1, "식비", "개인카드", "2026-10-12", "식당", 10_000);
        createdId("user2", user2Trip, "식비", "개인카드", "2026-10-12", "식당", 8_000);

        search("manager1", "scope=TEAM").andExpect(jsonPath("$.data.page.totalElements").value(2));
        search("manager2", "scope=TEAM").andExpect(jsonPath("$.data.page.totalElements").value(0));
        search("admin", "scope=ALL&userName=user2").andExpect(jsonPath("$.data.totalAmount").value(8_000));
        search("user1", "scope=TEAM").andExpect(status().isForbidden());
    }

    @Test
    void 출장별_경비_요약은_법인카드와_개인부담을_나눈다() throws Exception {
        createdId("user1", trip1, "교통비", "법인카드", "2026-10-12", "KTX", 63_000);
        createdId("user1", trip1, "식비", "개인카드", "2026-10-12", "식당", 30_000);
        createdId("user1", trip1, "숙박비", "개인카드", "2026-10-13", "호텔", 60_000);

        mockMvc.perform(get("/api/trips/{id}/expense-summary", trip1).header("Authorization", tokens.get("user1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalAmount").value(153_000))
                .andExpect(jsonPath("$.data.corporateAmount").value(63_000))
                .andExpect(jsonPath("$.data.personalAmount").value(90_000))
                .andExpect(jsonPath("$.data.count").value(3))
                .andExpect(jsonPath("$.data.byCategory[0].categoryName").value("교통비"))
                .andExpect(jsonPath("$.data.byCategory[0].amount").value(63_000));

        mockMvc.perform(get("/api/trips/{id}/expense-summary", trip1).header("Authorization", tokens.get("user2")))
                .andExpect(status().isForbidden());
    }

    @Test
    void 경비가_없는_출장_요약은_0원() throws Exception {
        mockMvc.perform(get("/api/trips/{id}/expense-summary", trip1).header("Authorization", tokens.get("user1")))
                .andExpect(jsonPath("$.data.totalAmount").value(0))
                .andExpect(jsonPath("$.data.byCategory", hasSize(0)));
    }

    @Test
    void 경비_등록용_출장_목록은_진행중_완료_출장만() throws Exception {
        mockMvc.perform(get("/api/trips/expensable").header("Authorization", tokens.get("user1")))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].tripId").value(trip1));
    }

    // ---------- 기준 코드 관리 ----------

    @Test
    void 기준_코드_조회는_사용중인_것만() throws Exception {
        mockMvc.perform(get("/api/codes/expense-categories").header("Authorization", tokens.get("user1")))
                .andExpect(jsonPath("$.data", hasSize(7)))
                .andExpect(jsonPath("$.data[0].name").value("교통비"));
        mockMvc.perform(get("/api/codes/payment-methods").header("Authorization", tokens.get("user1")))
                .andExpect(jsonPath("$.data", hasSize(4)))
                .andExpect(jsonPath("$.data[0].corporate").value(true));
    }

    @Test
    void 관리자는_비용_항목을_추가하고_사용중지할_수_있다() throws Exception {
        String admin = tokens.get("admin");
        String body = mockMvc.perform(post("/api/admin/expense-categories").header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"주차비\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long id = ((Number) JsonPath.read(body, "$.data.categoryId")).longValue();

        mockMvc.perform(put("/api/admin/expense-categories/{id}", id).header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"주차비\",\"active\":false}"))
                .andExpect(jsonPath("$.data.active").value(false));

        mockMvc.perform(get("/api/codes/expense-categories").header("Authorization", tokens.get("user1")))
                .andExpect(jsonPath("$.data", hasSize(7)));
        mockMvc.perform(get("/api/codes/expense-categories?all=true").header("Authorization", admin))
                .andExpect(jsonPath("$.data", hasSize(8)));
    }

    @Test
    void 같은_이름의_코드는_만들_수_없다() throws Exception {
        mockMvc.perform(post("/api/admin/payment-methods").header("Authorization", tokens.get("admin"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"현금\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("DUPLICATE_CODE_NAME"));
    }

    @Test
    void 일반_직원과_팀장은_코드를_변경할_수_없다() throws Exception {
        for (String who : new String[] {"user1", "manager1"}) {
            mockMvc.perform(post("/api/admin/expense-categories").header("Authorization", tokens.get(who))
                            .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"주차비\"}"))
                    .andExpect(status().isForbidden());
        }
    }

    // ---------- helpers ----------

    private void user(Department dept, String loginId, Role role) {
        User user = userRepository.save(User.builder()
                .department(dept).loginId(loginId).password("{noop}x").name(loginId).role(role).build());
        users.put(loginId, user);
        tokens.put(loginId, "Bearer " + jwtTokenProvider.createToken(
                new LoginUser(user.getId(), loginId, role, dept.getId())));
    }

    /** 결재 흐름은 출장 테스트에서 검증하므로 여기서는 엔티티로 바로 상태를 만든다. */
    private long trip(String owner, boolean started) {
        Trip trip = Trip.builder().user(users.get(owner)).title("부산 출장").destination("부산")
                .startDate(LocalDate.of(2026, 10, 12)).endDate(LocalDate.of(2026, 10, 14))
                .expectedAmount(300_000).build();
        trip.request();
        trip.approve();
        if (started) {
            trip.start();
        }
        return tripRepository.save(trip).getId();
    }

    private String json(long tripId, String cat, String pay, String usedAt, String store, long amount) {
        return """
                {"tripId":%d,"categoryId":%d,"paymentMethodId":%d,"usedAt":"%s","storeName":"%s","amount":%d}
                """.formatted(tripId, category.get(cat), payment.get(pay), usedAt, store, amount);
    }

    private ResultActions create(String who, long tripId, String cat, String pay, String usedAt, String store,
                                 long amount) throws Exception {
        return mockMvc.perform(post("/api/expenses").header("Authorization", tokens.get(who))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(tripId, cat, pay, usedAt, store, amount)));
    }

    private long createdId(String who, long tripId, String cat, String pay, String usedAt, String store, long amount)
            throws Exception {
        String body = create(who, tripId, cat, pay, usedAt, store, amount)
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.data.expenseId")).longValue();
    }

    private ResultActions detail(String who, long id) throws Exception {
        return mockMvc.perform(get("/api/expenses/{id}", id).header("Authorization", tokens.get(who)));
    }

    private ResultActions search(String who, String query) throws Exception {
        return mockMvc.perform(get("/api/expenses?" + query).header("Authorization", tokens.get(who)));
    }
}
