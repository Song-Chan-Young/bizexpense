package com.bizexpense.domain.dashboard.dto;

/** 월별 경비 합계. 경비가 없는 달도 0 으로 채운다. */
public record MonthAmount(int year, int month, long count, long amount) {
}
