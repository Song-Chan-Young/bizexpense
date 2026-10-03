package com.bizexpense.domain.expense;

import com.bizexpense.domain.expense.dto.CategoryAmount;
import com.bizexpense.domain.expense.dto.ExpenseTotals;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ExpenseRepository extends JpaRepository<Expense, Long>, JpaSpecificationExecutor<Expense>,
        ExpenseQueryRepository {

    @Query("""
            select e from Expense e
            join fetch e.user u
            left join fetch u.department
            join fetch e.trip
            join fetch e.category
            join fetch e.paymentMethod
            where e.id = :id
            """)
    Optional<Expense> findDetailById(@Param("id") Long id);

    /** 출장별 합계: 전체 / 법인 결제분 / 건수 */
    @Query("""
            select new com.bizexpense.domain.expense.dto.ExpenseTotals(
                coalesce(sum(e.amount), 0L),
                coalesce(sum(case when pm.corporate = true then e.amount else 0L end), 0L),
                count(e))
            from Expense e join e.paymentMethod pm
            where e.trip.id = :tripId
            """)
    ExpenseTotals sumByTrip(@Param("tripId") Long tripId);

    /** 출장별 비용 항목 합계 (금액 큰 순) */
    @Query("""
            select new com.bizexpense.domain.expense.dto.CategoryAmount(
                c.id, c.name, count(e), sum(e.amount))
            from Expense e join e.category c
            where e.trip.id = :tripId
            group by c.id, c.name
            order by sum(e.amount) desc
            """)
    List<CategoryAmount> sumByTripGroupByCategory(@Param("tripId") Long tripId);
}
