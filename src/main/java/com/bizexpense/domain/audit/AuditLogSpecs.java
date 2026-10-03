package com.bizexpense.domain.audit;

import com.bizexpense.domain.audit.dto.AuditLogSearchCondition;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

final class AuditLogSpecs {

    private AuditLogSpecs() {
    }

    static Specification<AuditLog> search(AuditLogSearchCondition cond) {
        List<Specification<AuditLog>> specs = new ArrayList<>();
        if (StringUtils.hasText(cond.actor())) {
            String like = "%" + cond.actor().trim() + "%";
            specs.add((root, query, cb) -> cb.or(
                    cb.like(root.get("actorName"), like),
                    cb.like(root.get("actorLoginId"), like)));
        }
        if (cond.action() != null) {
            specs.add((root, query, cb) -> cb.equal(root.get("action"), cond.action()));
        }
        if (StringUtils.hasText(cond.targetType())) {
            specs.add((root, query, cb) -> cb.equal(root.get("targetType"), cond.targetType()));
        }
        if (cond.result() != null) {
            specs.add((root, query, cb) -> cb.equal(root.get("result"), cond.result()));
        }
        if (cond.from() != null) {
            specs.add((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), cond.from().atStartOfDay()));
        }
        if (cond.to() != null) {
            specs.add((root, query, cb) -> cb.lessThan(root.get("createdAt"), cond.to().plusDays(1).atStartOfDay()));
        }
        return Specification.allOf(specs);
    }
}
