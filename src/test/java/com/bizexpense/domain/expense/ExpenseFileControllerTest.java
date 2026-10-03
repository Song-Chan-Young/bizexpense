package com.bizexpense.domain.expense;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

/** 경비 영수증 첨부 통합 테스트 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ExpenseFileControllerTest {

    static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 1, 2, 3};
    static final byte[] PDF = "%PDF-1.4 receipt".getBytes();

    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;
    @Autowired DepartmentRepository departmentRepository;
    @Autowired TripRepository tripRepository;
    @Autowired ExpenseRepository expenseRepository;
    @Autowired ExpenseFileRepository fileRepository;
    @Autowired ExpenseCategoryRepository categoryRepository;
    @Autowired PaymentMethodRepository paymentMethodRepository;
    @Autowired JwtTokenProvider jwtTokenProvider;

    final Map<String, User> users = new HashMap<>();
    final Map<String, String> tokens = new HashMap<>();

    long expenseId;   // user1 의 임시저장 경비

    @BeforeEach
    void setUp() throws Exception {
        Department sales1 = departmentRepository.save(new Department("영업1팀", "SALES1"));
        Department sales2 = departmentRepository.save(new Department("영업2팀", "SALES2"));
        user(sales1, "user1", Role.USER);
        user(sales1, "user2", Role.USER);
        user(sales1, "manager1", Role.MANAGER);
        user(sales2, "manager2", Role.MANAGER);

        Trip trip = Trip.builder().user(users.get("user1")).title("부산 출장").destination("부산")
                .startDate(LocalDate.of(2026, 10, 12)).endDate(LocalDate.of(2026, 10, 14))
                .expectedAmount(300_000).build();
        trip.request();
        trip.approve();
        trip.start();
        long tripId = tripRepository.save(trip).getId();

        String body = mockMvc.perform(post("/api/expenses").header("Authorization", tokens.get("user1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tripId":%d,"categoryId":%d,"paymentMethodId":%d,"usedAt":"2026-10-12",
                                 "storeName":"KTX","amount":59800}
                                """.formatted(tripId, categoryRepository.findAll().getFirst().getId(),
                                paymentMethodRepository.findAll().getFirst().getId())))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        expenseId = ((Number) JsonPath.read(body, "$.data.expenseId")).longValue();
    }

    @Test
    void 영수증을_첨부하면_증빙_여부가_켜지고_목록에_보인다() throws Exception {
        upload("user1", "영수증.png", PNG)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.originalName").value("영수증.png"))
                .andExpect(jsonPath("$.data.contentType").value("image/png"))
                .andExpect(jsonPath("$.data.size").value(PNG.length));

        expense("user1").andExpect(jsonPath("$.data.proof").value(true));
        mockMvc.perform(get("/api/expenses/{id}/files", expenseId).header("Authorization", tokens.get("user1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)));
    }

    @Test
    void 첨부한_파일을_내려받는다() throws Exception {
        long fileId = uploadedId("user1", "receipt.pdf", PDF);

        mockMvc.perform(get("/api/expenses/{id}/files/{fileId}", expenseId, fileId)
                        .header("Authorization", tokens.get("manager1")))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/pdf"))
                .andExpect(content().bytes(PDF))
                .andExpect(header().string("Content-Disposition", containsString("inline")))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"));
    }

    @Test
    void 다른_부서_팀장과_같은_부서_동료는_볼_수_없다() throws Exception {
        long fileId = uploadedId("user1", "receipt.pdf", PDF);

        for (String who : new String[] {"manager2", "user2"}) {
            mockMvc.perform(get("/api/expenses/{id}/files/{fileId}", expenseId, fileId)
                            .header("Authorization", tokens.get(who)))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    void 본인만_첨부할_수_있다() throws Exception {
        upload("manager1", "receipt.png", PNG).andExpect(status().isForbidden());
    }

    @Test
    void 허용하지_않는_확장자는_400() throws Exception {
        upload("user1", "script.html", "<script>alert(1)</script>".getBytes())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_FILE_TYPE"));
    }

    @Test
    void 확장자와_내용이_다르면_400() throws Exception {
        upload("user1", "fake.png", "<html>not an image</html>".getBytes())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_FILE_TYPE"));
    }

    @Test
    void 빈_파일은_400() throws Exception {
        upload("user1", "empty.png", new byte[0])
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_INPUT"));
    }

    @Test
    void 경비_1건에_5개까지만_첨부할_수_있다() throws Exception {
        for (int i = 0; i < ExpenseFile.MAX_COUNT; i++) {
            upload("user1", "r" + i + ".png", PNG).andExpect(status().isCreated());
        }
        upload("user1", "r5.png", PNG)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("FILE_COUNT_EXCEEDED"));
    }

    @Test
    void 정산_신청한_경비에는_첨부하거나_삭제할_수_없다() throws Exception {
        long fileId = uploadedId("user1", "receipt.png", PNG);
        expenseRepository.findById(expenseId).orElseThrow().requestSettlement(999L);

        upload("user1", "more.png", PNG)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("INVALID_EXPENSE_STATUS"));
        deleteFile("user1", fileId)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("INVALID_EXPENSE_STATUS"));
    }

    @Test
    void 마지막_파일을_지우면_증빙_여부가_꺼진다() throws Exception {
        long first = uploadedId("user1", "a.png", PNG);
        long second = uploadedId("user1", "b.pdf", PDF);

        deleteFile("user1", first).andExpect(status().isOk());
        expense("user1").andExpect(jsonPath("$.data.proof").value(true));

        deleteFile("user1", second).andExpect(status().isOk());
        expense("user1").andExpect(jsonPath("$.data.proof").value(false));
    }

    @Test
    void 다른_경비의_파일_번호로는_접근할_수_없다() throws Exception {
        long fileId = uploadedId("user1", "a.png", PNG);

        mockMvc.perform(get("/api/expenses/{id}/files/{fileId}", expenseId + 1000, fileId)
                        .header("Authorization", tokens.get("user1")))
                .andExpect(status().isNotFound());
        deleteFile("user1", fileId + 1000)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("FILE_NOT_FOUND"));
    }

    @Test
    void 경비를_삭제하면_첨부_파일도_지워진다() throws Exception {
        uploadedId("user1", "a.png", PNG);

        mockMvc.perform(delete("/api/expenses/{id}", expenseId).header("Authorization", tokens.get("user1")))
                .andExpect(status().isOk());
        assertThat(fileRepository.countByExpenseId(expenseId)).isZero();
    }

    // ---------- helpers ----------

    private void user(Department dept, String loginId, Role role) {
        User user = userRepository.save(User.builder()
                .department(dept).loginId(loginId).password("{noop}x").name(loginId).role(role).build());
        users.put(loginId, user);
        tokens.put(loginId, "Bearer " + jwtTokenProvider.createToken(
                new LoginUser(user.getId(), loginId, role, dept.getId())));
    }

    private ResultActions upload(String who, String name, byte[] data) throws Exception {
        return mockMvc.perform(multipart("/api/expenses/{id}/files", expenseId)
                .file(new MockMultipartFile("file", name, "application/octet-stream", data))
                .header("Authorization", tokens.get(who)));
    }

    private long uploadedId(String who, String name, byte[] data) throws Exception {
        String body = upload(who, name, data).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.data.fileId")).longValue();
    }

    private ResultActions deleteFile(String who, long fileId) throws Exception {
        return mockMvc.perform(delete("/api/expenses/{id}/files/{fileId}", expenseId, fileId)
                .header("Authorization", tokens.get(who)));
    }

    private ResultActions expense(String who) throws Exception {
        return mockMvc.perform(get("/api/expenses/{id}", expenseId).header("Authorization", tokens.get(who)));
    }
}
