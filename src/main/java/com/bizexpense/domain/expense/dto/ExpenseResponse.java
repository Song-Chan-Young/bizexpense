package com.bizexpense.domain.expense.dto;

import com.bizexpense.domain.expense.Expense;
import com.bizexpense.domain.expense.ExpenseStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * @param corporate 법인 결제 여부 (정산 시 개인 지급액에서 제외)
 * @param editable  조회한 사용자가 지금 수정/삭제할 수 있는지
 */
public record ExpenseResponse(
        Long expenseId,
        Long tripId,
        String tripTitle,
        Long userId,
        String userName,
        Long categoryId,
        String categoryName,
        Long paymentMethodId,
        String paymentMethodName,
        boolean corporate,
        LocalDate usedAt,
        String storeName,
        long amount,
        String description,
        boolean proof,
        ExpenseStatus status,
        String statusLabel,
        boolean editable,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public static ExpenseResponse of(Expense e, Long viewerId) {
        return new ExpenseResponse(
                e.getId(),
                e.getTrip().getId(),
                e.getTrip().getTitle(),
                e.getUser().getId(),
                e.getUser().getName(),
                e.getCategory().getId(),
                e.getCategory().getName(),
                e.getPaymentMethod().getId(),
                e.getPaymentMethod().getName(),
                e.getPaymentMethod().isCorporate(),
                e.getUsedAt(),
                e.getStoreName(),
                e.getAmount(),
                e.getDescription(),
                e.isProof(),
                e.getStatus(),
                e.getStatus().getLabel(),
                e.isOwnedBy(viewerId) && e.getStatus().isEditable(),
                e.getCreatedAt(),
                e.getUpdatedAt());
    }
}
