package com.bizexpense.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 데모(체험) 설정. enabled 면 초기 데모 데이터를 넣고, 로그인 화면에 체험 계정을 안내한다.
 *
 * @param enabled  데모 모드 여부 (local, demo 프로필에서 true)
 * @param password 데모 계정 공통 비밀번호
 */
@ConfigurationProperties(prefix = "demo")
public record DemoProperties(boolean enabled, String password) {
}
