package com.bizexpense.domain.dashboard.dto;

/**
 * 대시보드 숫자 카드 1개.
 *
 * @param unit WON(금액) 또는 COUNT(건수)
 * @param hint 보조 설명 (예: "12건")
 * @param link 눌렀을 때 이동할 화면 주소
 */
public record StatCard(String key, String label, long value, String unit, String hint, String link) {

    public static StatCard won(String key, String label, long value, String hint, String link) {
        return new StatCard(key, label, value, "WON", hint, link);
    }

    public static StatCard count(String key, String label, long value, String hint, String link) {
        return new StatCard(key, label, value, "COUNT", hint, link);
    }
}
