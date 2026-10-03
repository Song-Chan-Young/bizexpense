package com.bizexpense.domain.approval;

import com.bizexpense.domain.approval.dto.ApprovalSearchCondition;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

final class ApprovalSpecs {

    private ApprovalSpecs() {
    }

    static Specification<Approval> search(Long viewerId, ApprovalSearchCondition cond) {
        List<Specification<Approval>> specs = new ArrayList<>();
        switch (cond.boxOrDefault()) {
            case INBOX -> specs.add((root, query, cb) -> cb.equal(root.get("approver").get("id"), viewerId));
            case SENT -> specs.add((root, query, cb) -> cb.equal(root.get("requester").get("id"), viewerId));
            case ALL -> { /* 관리자: 조건 없음 (권한은 서비스에서 확인) */ }
        }
        if (cond.status() != null) {
            specs.add((root, query, cb) -> cb.equal(root.get("status"), cond.status()));
        }
        if (cond.targetType() != null) {
            specs.add((root, query, cb) -> cb.equal(root.get("targetType"), cond.targetType()));
        }
        if (StringUtils.hasText(cond.requesterName())) {
            String like = "%" + cond.requesterName().trim() + "%";
            specs.add((root, query, cb) -> cb.like(root.get("requester").get("name"), like));
        }
        if (cond.from() != null) {
            LocalDateTime from = cond.from().atStartOfDay();
            specs.add((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("requestedAt"), from));
        }
        if (cond.to() != null) {
            LocalDateTime toExclusive = cond.to().plusDays(1).atStartOfDay();
            specs.add((root, query, cb) -> cb.lessThan(root.get("requestedAt"), toExclusive));
        }
        return Specification.allOf(specs);
    }
}
