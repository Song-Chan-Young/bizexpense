package com.bizexpense.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * API 문서 (springdoc). Swagger UI: /swagger-ui.html
 * POST /api/auth/login 으로 받은 accessToken 을 오른쪽 위 Authorize 에 넣으면 나머지 API 를 바로 호출해 볼 수 있다.
 */
@Configuration
public class OpenApiConfig {

    private static final String BEARER = "bearerAuth";

    @Bean
    public OpenAPI openApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("BizExpense API")
                        .version("v1")
                        .description("""
                                기업 출장 · 일정 · 경비 · 결재 · 정산 관리 API.

                                - 모든 응답은 `{ success, data, error }` 형식이다.
                                - `POST /api/auth/login` 의 `accessToken` 을 **Authorize** 에 넣고 호출한다.
                                - 데모 계정: user1(직원) / manager1(팀장) / admin(관리자)
                                """))
                // 프록시(Render) 뒤에서도 페이지와 같은 주소로 호출하도록 상대 경로를 쓴다
                .addServersItem(new Server().url("/"))
                .components(new Components().addSecuritySchemes(BEARER, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER));
    }
}
