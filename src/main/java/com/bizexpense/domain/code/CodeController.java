package com.bizexpense.domain.code;

import io.swagger.v3.oas.annotations.tags.Tag;
import com.bizexpense.domain.audit.AuditAction;
import com.bizexpense.domain.audit.Audited;
import com.bizexpense.domain.code.dto.CodeDtos.CategoryRequest;
import com.bizexpense.domain.code.dto.CodeDtos.CategoryResponse;
import com.bizexpense.domain.code.dto.CodeDtos.PaymentMethodRequest;
import com.bizexpense.domain.code.dto.CodeDtos.PaymentMethodResponse;
import com.bizexpense.global.common.ApiResponse;
import com.bizexpense.global.security.LoginUser;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 조회(/api/codes/**)는 로그인 사용자 모두, 변경(/api/admin/**)은 관리자만 (SecurityConfig).
 */
@Tag(name = "09. 기준 코드", description = "비용 항목 / 결제 수단 (관리자 변경)")
@RestController
@RequiredArgsConstructor
public class CodeController {

    private final CodeService codeService;

    /** all=true 는 관리자만 의미가 있다 (사용 중지 항목 포함) */
    @GetMapping("/api/codes/expense-categories")
    public ApiResponse<List<CategoryResponse>> categories(@AuthenticationPrincipal LoginUser loginUser,
                                                          @RequestParam(defaultValue = "false") boolean all) {
        return ApiResponse.ok(codeService.categories(all && loginUser.isAdmin()));
    }

    @GetMapping("/api/codes/payment-methods")
    public ApiResponse<List<PaymentMethodResponse>> paymentMethods(@AuthenticationPrincipal LoginUser loginUser,
                                                                   @RequestParam(defaultValue = "false") boolean all) {
        return ApiResponse.ok(codeService.paymentMethods(all && loginUser.isAdmin()));
    }

    @Audited(AuditAction.CATEGORY_CREATE)
    @PostMapping("/api/admin/expense-categories")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CategoryResponse> createCategory(@Valid @RequestBody CategoryRequest request) {
        return ApiResponse.ok(codeService.createCategory(request));
    }

    @Audited(AuditAction.CATEGORY_UPDATE)
    @PutMapping("/api/admin/expense-categories/{id}")
    public ApiResponse<CategoryResponse> updateCategory(@PathVariable Long id,
                                                        @Valid @RequestBody CategoryRequest request) {
        return ApiResponse.ok(codeService.updateCategory(id, request));
    }

    @Audited(AuditAction.PAYMENT_METHOD_CREATE)
    @PostMapping("/api/admin/payment-methods")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<PaymentMethodResponse> createPaymentMethod(@Valid @RequestBody PaymentMethodRequest request) {
        return ApiResponse.ok(codeService.createPaymentMethod(request));
    }

    @Audited(AuditAction.PAYMENT_METHOD_UPDATE)
    @PutMapping("/api/admin/payment-methods/{id}")
    public ApiResponse<PaymentMethodResponse> updatePaymentMethod(@PathVariable Long id,
                                                                  @Valid @RequestBody PaymentMethodRequest request) {
        return ApiResponse.ok(codeService.updatePaymentMethod(id, request));
    }
}
