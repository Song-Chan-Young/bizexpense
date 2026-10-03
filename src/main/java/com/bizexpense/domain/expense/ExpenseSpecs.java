package com.bizexpense.domain.expense;

import com.bizexpense.domain.expense.dto.ExpenseSearchCondition;
import com.bizexpense.global.security.LoginUser;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

final class ExpenseSpecs {

    private ExpenseSpecs() {
    }

    static Specification<Expense> search(LoginUser loginUser, ExpenseSearchCondition cond) {
        List<Specification<Expense>> specs = new ArrayList<>();
        switch (cond.scopeOrDefault()) {
            case ME -> specs.add((root, query, cb) -> cb.equal(root.get("user").get("id"), loginUser.userId()));
            case TEAM -> specs.add((root, query, cb) ->
                    cb.equal(root.get("user").get("department").get("id"), loginUser.departmentId()));
            case ALL -> { /* 관리자: 조건 없음 */ }
        }
        if (cond.tripId() != null) {
            specs.add((root, query, cb) -> cb.equal(root.get("trip").get("id"), cond.tripId()));
        }
        if (cond.from() != null) {
            specs.add((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("usedAt"), cond.from()));
        }
        if (cond.to() != null) {
            specs.add((root, query, cb) -> cb.lessThanOrEqualTo(root.get("usedAt"), cond.to()));
        }
        if (cond.categoryId() != null) {
            specs.add((root, query, cb) -> cb.equal(root.get("category").get("id"), cond.categoryId()));
        }
        if (cond.paymentMethodId() != null) {
            specs.add((root, query, cb) -> cb.equal(root.get("paymentMethod").get("id"), cond.paymentMethodId()));
        }
        if (cond.status() != null) {
            specs.add((root, query, cb) -> cb.equal(root.get("status"), cond.status()));
        }
        if (cond.minAmount() != null) {
            specs.add((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("amount"), cond.minAmount()));
        }
        if (cond.maxAmount() != null) {
            specs.add((root, query, cb) -> cb.lessThanOrEqualTo(root.get("amount"), cond.maxAmount()));
        }
        if (StringUtils.hasText(cond.keyword())) {
            String like = "%" + cond.keyword().trim().toLowerCase() + "%";
            specs.add((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("storeName")), like),
                    cb.like(cb.lower(root.get("description")), like)));
        }
        if (StringUtils.hasText(cond.userName())) {
            String like = "%" + cond.userName().trim() + "%";
            specs.add((root, query, cb) -> cb.like(root.get("user").get("name"), like));
        }
        return Specification.allOf(specs);
    }
}
