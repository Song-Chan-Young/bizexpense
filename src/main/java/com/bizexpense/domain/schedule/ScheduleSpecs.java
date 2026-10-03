package com.bizexpense.domain.schedule;

import com.bizexpense.domain.schedule.dto.ScheduleSearchCondition;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

/** 일정 목록 검색 조건 (값이 있는 조건만 AND 로 조합) */
final class ScheduleSpecs {

    private ScheduleSpecs() {
    }

    static Specification<Schedule> search(Long userId, ScheduleSearchCondition cond) {
        List<Specification<Schedule>> specs = new ArrayList<>();
        specs.add((root, query, cb) -> cb.equal(root.get("user").get("id"), userId));

        // 기간: [from 00:00, to+1일 00:00) 과 겹치는 일정
        if (cond.from() != null) {
            LocalDateTime from = cond.from().atStartOfDay();
            specs.add((root, query, cb) -> cb.greaterThan(root.get("endAt"), from));
        }
        if (cond.to() != null) {
            LocalDateTime toExclusive = cond.to().plusDays(1).atStartOfDay();
            specs.add((root, query, cb) -> cb.lessThan(root.get("startAt"), toExclusive));
        }
        if (cond.type() != null) {
            specs.add((root, query, cb) -> cb.equal(root.get("type"), cond.type()));
        }
        if (cond.status() != null) {
            specs.add((root, query, cb) -> cb.equal(root.get("status"), cond.status()));
        }
        if (StringUtils.hasText(cond.keyword())) {
            String like = "%" + cond.keyword().trim().toLowerCase() + "%";
            specs.add((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("title")), like),
                    cb.like(cb.lower(root.get("location")), like)));
        }
        return Specification.allOf(specs);
    }
}
