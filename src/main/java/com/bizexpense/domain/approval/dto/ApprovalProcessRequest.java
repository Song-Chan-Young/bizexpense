package com.bizexpense.domain.approval.dto;

import jakarta.validation.constraints.Size;

/** 승인/반려 요청. 반려 시 comment(반려 사유)는 필수다. */
public record ApprovalProcessRequest(@Size(max = 1000) String comment) {
}
