package com.bizexpense.domain.approval.dto;

import com.bizexpense.domain.approval.ApprovalStatus;
import com.bizexpense.domain.approval.ApprovalTargetType;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * 결재 목록 검색 조건.
 *
 * @param box  INBOX(내가 결재할 건, 기본) / SENT(내가 신청한 건) / ALL(전체, 관리자)
 * @param from 신청일 시작 (포함)
 * @param to   신청일 끝 (포함)
 */
public record ApprovalSearchCondition(
        ApprovalBox box,
        ApprovalStatus status,
        ApprovalTargetType targetType,
        String requesterName,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

    public ApprovalBox boxOrDefault() {
        return box == null ? ApprovalBox.INBOX : box;
    }

    public enum ApprovalBox {
        INBOX, SENT, ALL
    }
}
