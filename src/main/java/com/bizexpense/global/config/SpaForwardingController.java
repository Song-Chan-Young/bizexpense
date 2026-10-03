package com.bizexpense.global.config;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 배포 시 React 빌드 결과(static/index.html)를 Spring Boot 가 함께 서비스한다.
 * /trips/1 처럼 React Router 가 처리하는 화면 주소로 새로고침/직접 접속해도
 * index.html 을 돌려줘서 화면이 열리게 한다. (/api 와 파일(점 포함 경로)은 제외)
 */
@Controller
public class SpaForwardingController {

    @GetMapping({
            "/login", "/schedules", "/expenses", "/approvals", "/admin", "/admin/audit-logs",
            "/trips", "/trips/new", "/trips/{id:\\d+}", "/trips/{id:\\d+}/edit",
            "/settlements", "/settlements/{id:\\d+}"
    })
    public String forward() {
        return "forward:/index.html";
    }
}
