package com.bizexpense.domain.expense;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;

@RequiredArgsConstructor
class ExpenseQueryRepositoryImpl implements ExpenseQueryRepository {

    private final EntityManager em;

    @Override
    public long sumAmount(Specification<Expense> spec) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Long> query = cb.createQuery(Long.class);
        Root<Expense> root = query.from(Expense.class);
        query.select(cb.coalesce(cb.sum(root.<Long>get("amount")), 0L));

        // 목록 조회와 같은 Specification 을 그대로 써서 조건이 어긋나지 않게 한다
        Predicate predicate = spec.toPredicate(root, query, cb);
        if (predicate != null) {
            query.where(predicate);
        }
        return em.createQuery(query).getSingleResult();
    }
}
