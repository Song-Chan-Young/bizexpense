package com.bizexpense.domain.code;

import com.bizexpense.domain.code.dto.CodeDtos.CategoryRequest;
import com.bizexpense.domain.code.dto.CodeDtos.CategoryResponse;
import com.bizexpense.domain.code.dto.CodeDtos.PaymentMethodRequest;
import com.bizexpense.domain.code.dto.CodeDtos.PaymentMethodResponse;
import com.bizexpense.global.error.BusinessException;
import com.bizexpense.global.error.ErrorCode;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CodeService {

    private final ExpenseCategoryRepository categoryRepository;
    private final PaymentMethodRepository paymentMethodRepository;

    // ---------- 비용 항목 ----------

    /** includeInactive=false 면 사용 중인 항목만 (경비 등록 화면용) */
    public List<CategoryResponse> categories(boolean includeInactive) {
        return (includeInactive ? categoryRepository.findAllByOrderByIdAsc()
                : categoryRepository.findByActiveTrueOrderByIdAsc())
                .stream().map(CategoryResponse::from).toList();
    }

    @Transactional
    public CategoryResponse createCategory(CategoryRequest request) {
        String name = request.name().trim();
        if (categoryRepository.existsByName(name)) {
            throw new BusinessException(ErrorCode.DUPLICATE_CODE_NAME);
        }
        return CategoryResponse.from(categoryRepository.save(new ExpenseCategory(name)));
    }

    @Transactional
    public CategoryResponse updateCategory(Long id, CategoryRequest request) {
        ExpenseCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.CODE_NOT_FOUND));
        String name = request.name().trim();
        if (categoryRepository.existsByNameAndIdNot(name, id)) {
            throw new BusinessException(ErrorCode.DUPLICATE_CODE_NAME);
        }
        category.update(name, request.active() == null ? category.isActive() : request.active());
        return CategoryResponse.from(category);
    }

    /** 경비 등록/수정 시 사용: 사용 중인 항목만 허용 */
    public ExpenseCategory activeCategory(Long id) {
        ExpenseCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.CODE_NOT_FOUND));
        if (!category.isActive()) {
            throw new BusinessException(ErrorCode.INACTIVE_CODE);
        }
        return category;
    }

    // ---------- 결제 수단 ----------

    public List<PaymentMethodResponse> paymentMethods(boolean includeInactive) {
        return (includeInactive ? paymentMethodRepository.findAllByOrderByIdAsc()
                : paymentMethodRepository.findByActiveTrueOrderByIdAsc())
                .stream().map(PaymentMethodResponse::from).toList();
    }

    @Transactional
    public PaymentMethodResponse createPaymentMethod(PaymentMethodRequest request) {
        String name = request.name().trim();
        if (paymentMethodRepository.existsByName(name)) {
            throw new BusinessException(ErrorCode.DUPLICATE_CODE_NAME);
        }
        return PaymentMethodResponse.from(
                paymentMethodRepository.save(new PaymentMethod(name, Boolean.TRUE.equals(request.corporate()))));
    }

    @Transactional
    public PaymentMethodResponse updatePaymentMethod(Long id, PaymentMethodRequest request) {
        PaymentMethod method = paymentMethodRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.CODE_NOT_FOUND));
        String name = request.name().trim();
        if (paymentMethodRepository.existsByNameAndIdNot(name, id)) {
            throw new BusinessException(ErrorCode.DUPLICATE_CODE_NAME);
        }
        method.update(name,
                request.corporate() == null ? method.isCorporate() : request.corporate(),
                request.active() == null ? method.isActive() : request.active());
        return PaymentMethodResponse.from(method);
    }

    public PaymentMethod activePaymentMethod(Long id) {
        PaymentMethod method = paymentMethodRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.CODE_NOT_FOUND));
        if (!method.isActive()) {
            throw new BusinessException(ErrorCode.INACTIVE_CODE);
        }
        return method;
    }
}
