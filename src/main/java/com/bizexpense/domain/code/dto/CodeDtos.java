package com.bizexpense.domain.code.dto;

import com.bizexpense.domain.code.ExpenseCategory;
import com.bizexpense.domain.code.PaymentMethod;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 기준 코드(비용 항목, 결제 수단) 요청/응답 */
public final class CodeDtos {

    private CodeDtos() {
    }

    public record CategoryRequest(
            @NotBlank(message = "이름을 입력하세요.") @Size(max = 50) String name,
            Boolean active) {
    }

    public record CategoryResponse(Long categoryId, String name, boolean active) {

        public static CategoryResponse from(ExpenseCategory c) {
            return new CategoryResponse(c.getId(), c.getName(), c.isActive());
        }
    }

    public record PaymentMethodRequest(
            @NotBlank(message = "이름을 입력하세요.") @Size(max = 50) String name,
            Boolean corporate,
            Boolean active) {
    }

    public record PaymentMethodResponse(Long paymentMethodId, String name, boolean corporate, boolean active) {

        public static PaymentMethodResponse from(PaymentMethod p) {
            return new PaymentMethodResponse(p.getId(), p.getName(), p.isCorporate(), p.isActive());
        }
    }
}
