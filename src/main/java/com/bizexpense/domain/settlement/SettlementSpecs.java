package com.bizexpense.domain.settlement;

import com.bizexpense.domain.settlement.dto.SettlementSearchCondition;
import com.bizexpense.global.security.LoginUser;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

final class SettlementSpecs {

    private SettlementSpecs() {
    }

    static Specification<Settlement> search(LoginUser loginUser, SettlementSearchCondition cond) {
        List<Specification<Settlement>> specs = new ArrayList<>();
        switch (cond.scopeOrDefault()) {
            case ME -> specs.add((root, query, cb) -> cb.equal(root.get("user").get("id"), loginUser.userId()));
            case TEAM -> specs.add((root, query, cb) ->
                    cb.equal(root.get("user").get("department").get("id"), loginUser.departmentId()));
            case ALL -> { /* 관리자: 조건 없음 */ }
        }
        if (cond.status() != null) {
            specs.add((root, query, cb) -> cb.equal(root.get("status"), cond.status()));
        }
        if (StringUtils.hasText(cond.userName())) {
            String like = "%" + cond.userName().trim() + "%";
            specs.add((root, query, cb) -> cb.like(root.get("user").get("name"), like));
        }
        if (cond.from() != null) {
            specs.add((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("requestedAt"), cond.from().atStartOfDay()));
        }
        if (cond.to() != null) {
            specs.add((root, query, cb) ->
                    cb.lessThan(root.get("requestedAt"), cond.to().plusDays(1).atStartOfDay()));
        }
        return Specification.allOf(specs);
    }
}
