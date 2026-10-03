package com.bizexpense.domain.expense.dto;

import com.bizexpense.domain.expense.ReceiptType;
import java.time.LocalDateTime;

/** 영수증 파일 메타 정보 (내용은 다운로드 API 로 따로 받는다) */
public record ExpenseFileResponse(
        Long fileId,
        String originalName,
        String contentType,
        long size,
        LocalDateTime createdAt) {

    public ExpenseFileResponse(Long fileId, String originalName, ReceiptType type, long size, LocalDateTime createdAt) {
        this(fileId, originalName, type.getContentType(), size, createdAt);
    }
}
