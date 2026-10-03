package com.bizexpense.global.config;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 데모 서버 기동 시 초기 데이터가 업무 규칙대로 만들어지는지 확인한다.
 * (실패하면 배포 서버가 뜨지 않으므로 별도로 검증)
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"test", "demo"})
@TestPropertySource(properties = {
        // 다른 테스트와 DB 를 공유하지 않도록 별도 메모리 DB 사용
        "spring.datasource.url=jdbc:h2:mem:demo-seed;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH",
        "demo.password=demo-pass"
})
class DemoDataInitializerTest {

    @Autowired MockMvc mockMvc;

    @Test
    void 체험_계정_안내와_로그인() throws Exception {
        mockMvc.perform(get("/api/public/demo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.enabled").value(true))
                .andExpect(jsonPath("$.data.password").value("demo-pass"))
                .andExpect(jsonPath("$.data.accounts", hasSize(3)))
                .andExpect(jsonPath("$.data.accounts[0].loginId").value("user1"));

        token("user1");
    }

    @Test
    void 예시_데이터가_상태별로_만들어진다() throws Exception {
        String user1 = token("user1");
        mockMvc.perform(get("/api/trips").header("Authorization", user1))
                .andExpect(jsonPath("$.data.totalElements").value(4));
        mockMvc.perform(get("/api/trips?status=IN_PROGRESS").header("Authorization", user1))
                .andExpect(jsonPath("$.data.content[0].title").value("부산 거래처 방문"));
        mockMvc.perform(get("/api/expenses").header("Authorization", user1))
                .andExpect(jsonPath("$.data.page.totalElements").value(9));
        // 수원 출장은 정산 완료
        mockMvc.perform(get("/api/settlements").header("Authorization", user1))
                .andExpect(jsonPath("$.data.content[0].status").value("SETTLED"))
                .andExpect(jsonPath("$.data.content[0].payableAmount").value(100_000));

        // 팀장 결재함: 대전 출장 + 인천 출장 정산 대기
        mockMvc.perform(get("/api/approvals?status=PENDING&targetType=TRIP").header("Authorization", token("manager1")))
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].title").value("대전 고객사 교육"));
        mockMvc.perform(get("/api/approvals?status=PENDING&targetType=SETTLEMENT").header("Authorization", token("manager1")))
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].title").value("인천 물류센터 방문 정산 (지급 15,500원)"));
        // 다른 팀 팀장은 자기 팀 건만
        mockMvc.perform(get("/api/approvals?status=PENDING").header("Authorization", token("manager2")))
                .andExpect(jsonPath("$.data.content[0].title").value("울산 공장 미팅"));
        // 반려 건
        mockMvc.perform(get("/api/trips?status=REJECTED").header("Authorization", token("user2")))
                .andExpect(jsonPath("$.data.content[0].title").value("광주 지사 점검"));
    }

    @Test
    void 헬스_체크는_로그인_없이_응답한다() throws Exception {
        mockMvc.perform(get("/api/public/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("UP"));
    }

    private String token(String loginId) throws Exception {
        String body = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"loginId\":\"%s\",\"password\":\"demo-pass\"}".formatted(loginId)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + JsonPath.read(body, "$.data.accessToken");
    }
}
