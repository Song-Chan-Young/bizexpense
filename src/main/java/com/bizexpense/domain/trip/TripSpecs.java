package com.bizexpense.domain.trip;

import com.bizexpense.domain.trip.dto.TripSearchCondition;
import com.bizexpense.global.security.LoginUser;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

final class TripSpecs {

    private TripSpecs() {
    }

    static Specification<Trip> search(LoginUser loginUser, TripSearchCondition cond) {
        List<Specification<Trip>> specs = new ArrayList<>();
        switch (cond.scopeOrDefault()) {
            case ME -> specs.add((root, query, cb) -> cb.equal(root.get("user").get("id"), loginUser.userId()));
            case TEAM -> specs.add((root, query, cb) ->
                    cb.equal(root.get("user").get("department").get("id"), loginUser.departmentId()));
            case ALL -> { /* 관리자: 조건 없음 */ }
        }
        if (cond.status() != null) {
            specs.add((root, query, cb) -> cb.equal(root.get("status"), cond.status()));
        }
        if (StringUtils.hasText(cond.keyword())) {
            String like = "%" + cond.keyword().trim().toLowerCase() + "%";
            specs.add((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("title")), like),
                    cb.like(cb.lower(root.get("destination")), like)));
        }
        if (StringUtils.hasText(cond.userName())) {
            String like = "%" + cond.userName().trim() + "%";
            specs.add((root, query, cb) -> cb.like(root.get("user").get("name"), like));
        }
        if (cond.from() != null) {
            specs.add((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("endDate"), cond.from()));
        }
        if (cond.to() != null) {
            specs.add((root, query, cb) -> cb.lessThanOrEqualTo(root.get("startDate"), cond.to()));
        }
        return Specification.allOf(specs);
    }
}
