package com.bizexpense.domain.dashboard;

import com.bizexpense.domain.approval.ApprovalStatus;
import com.bizexpense.domain.dashboard.dto.DashboardResponse;
import com.bizexpense.domain.dashboard.dto.MonthAmount;
import com.bizexpense.domain.dashboard.dto.StatCard;
import com.bizexpense.domain.expense.dto.CategoryAmount;
import com.bizexpense.domain.settlement.SettlementStatus;
import com.bizexpense.domain.trip.TripStatus;
import com.bizexpense.global.common.ViewScope;
import com.bizexpense.global.security.LoginUser;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 역할별 대시보드 통계. 여러 도메인을 가로지르는 읽기 전용 집계라
 * 각 저장소에 메서드를 늘리지 않고 이 서비스에서 JPQL 로 직접 집계한다.
 * 범위는 직원 = 본인, 팀장 = 같은 부서, 관리자 = 전체.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    /** 경비 추이에 보여줄 개월 수 (이번 달 포함) */
    static final int TREND_MONTHS = 6;

    private final EntityManager em;

    public DashboardResponse dashboard(LoginUser loginUser) {
        return dashboard(loginUser, LocalDate.now());
    }

    DashboardResponse dashboard(LoginUser loginUser, LocalDate today) {
        Scope scope = Scope.of(loginUser);
        YearMonth month = YearMonth.from(today);
        List<StatCard> cards = switch (scope.viewScope) {
            case ME -> userCards(scope, loginUser, month, today);
            case TEAM -> managerCards(scope, loginUser, month);
            case ALL -> adminCards(scope, month);
        };
        return new DashboardResponse(scope.viewScope, cards, monthly(scope, month), categories(scope, month));
    }

    // ---------- 역할별 카드 ----------

    private List<StatCard> userCards(Scope scope, LoginUser loginUser, YearMonth month, LocalDate today) {
        long[] expense = monthExpense(scope, month);
        long[] payable = settlementSum(scope, List.of(SettlementStatus.REQUESTED, SettlementStatus.APPROVED));
        long pending = count("""
                select count(a) from Approval a
                where a.requester.id = :userId and a.status = :status
                """, Map.of("userId", loginUser.userId(), "status", ApprovalStatus.PENDING));
        long trips = count("""
                select count(t) from Trip t
                where t.user.id = :userId and t.status in :statuses and t.endDate >= :today
                """, Map.of("userId", loginUser.userId(),
                "statuses", List.of(TripStatus.APPROVED, TripStatus.IN_PROGRESS), "today", today));
        return List.of(
                StatCard.won("monthExpense", "이번 달 내 경비", expense[1], expense[0] + "건", "/expenses"),
                StatCard.won("payable", "지급 예정액", payable[1], "정산 " + payable[0] + "건 진행 중", "/settlements"),
                StatCard.count("myPending", "결재 진행 중", pending, "출장·정산 신청", "/trips"),
                StatCard.count("upcomingTrips", "예정·진행 출장", trips, "승인 · 진행중", "/trips"));
    }

    private List<StatCard> managerCards(Scope scope, LoginUser loginUser, YearMonth month) {
        long inbox = count("""
                select count(a) from Approval a
                where a.approver.id = :userId and a.status = :status
                """, Map.of("userId", loginUser.userId(), "status", ApprovalStatus.PENDING));
        long[] expense = monthExpense(scope, month);
        long trips = count("""
                select count(t) from Trip t join t.user u
                where %s and t.status in :statuses and t.startDate <= :to and t.endDate >= :from
                """.formatted(scope.condition), scope.params(Map.of(
                "statuses", List.of(TripStatus.APPROVED, TripStatus.IN_PROGRESS, TripStatus.COMPLETED),
                "from", month.atDay(1), "to", month.atEndOfMonth())));
        long rejected = count("""
                select count(a) from Approval a
                where a.approver.id = :userId and a.status = :status
                  and a.processedAt >= :from and a.processedAt < :to
                """, Map.of("userId", loginUser.userId(), "status", ApprovalStatus.REJECTED,
                "from", start(month), "to", start(month.plusMonths(1))));
        return List.of(
                StatCard.count("approvalInbox", "결재 대기", inbox, "내가 처리할 건", "/approvals"),
                StatCard.won("monthExpense", "이번 달 팀 경비", expense[1], expense[0] + "건", "/expenses"),
                StatCard.count("monthTrips", "이번 달 팀 출장", trips, "승인 이후 출장", "/trips"),
                StatCard.count("monthRejected", "이번 달 반려", rejected, "내가 반려한 건", "/approvals"));
    }

    private List<StatCard> adminCards(Scope scope, YearMonth month) {
        long pending = count("select count(a) from Approval a where a.status = :status",
                Map.of("status", ApprovalStatus.PENDING));
        long[] expense = monthExpense(scope, month);
        long[] toPay = settlementSum(scope, List.of(SettlementStatus.APPROVED));
        long[] settled = sum("""
                select count(s), coalesce(sum(s.payableAmount), 0L) from Settlement s
                where s.status = :status and s.settledAt >= :from and s.settledAt < :to
                """, Map.of("status", SettlementStatus.SETTLED,
                "from", start(month), "to", start(month.plusMonths(1))));
        return List.of(
                StatCard.count("pendingApprovals", "미결재", pending, "전체 결재 대기", "/approvals"),
                StatCard.won("monthExpense", "이번 달 경비", expense[1], expense[0] + "건", "/expenses"),
                StatCard.won("toPay", "지급 대기", toPay[1], toPay[0] + "건 승인됨", "/settlements"),
                StatCard.won("monthSettled", "이번 달 지급 완료", settled[1], settled[0] + "건", "/settlements"));
    }

    // ---------- 공통 집계 ----------

    /** [건수, 금액] */
    private long[] monthExpense(Scope scope, YearMonth month) {
        return sum("""
                select count(e), coalesce(sum(e.amount), 0L) from Expense e join e.user u
                where %s and e.usedAt between :from and :to
                """.formatted(scope.condition),
                scope.params(Map.of("from", month.atDay(1), "to", month.atEndOfMonth())));
    }

    /** [건수, 지급액 합계] */
    private long[] settlementSum(Scope scope, List<SettlementStatus> statuses) {
        return sum("""
                select count(s), coalesce(sum(s.payableAmount), 0L) from Settlement s join s.user u
                where %s and s.status in :statuses
                """.formatted(scope.condition), scope.params(Map.of("statuses", statuses)));
    }

    private List<MonthAmount> monthly(Scope scope, YearMonth month) {
        YearMonth first = month.minusMonths(TREND_MONTHS - 1);
        TypedQuery<Object[]> query = em.createQuery("""
                select year(e.usedAt), month(e.usedAt), count(e), sum(e.amount)
                from Expense e join e.user u
                where %s and e.usedAt between :from and :to
                group by year(e.usedAt), month(e.usedAt)
                """.formatted(scope.condition), Object[].class);
        bind(query, scope.params(Map.of("from", first.atDay(1), "to", month.atEndOfMonth())));

        Map<YearMonth, Object[]> rows = new HashMap<>();
        for (Object[] row : query.getResultList()) {
            rows.put(YearMonth.of(((Number) row[0]).intValue(), ((Number) row[1]).intValue()), row);
        }
        List<MonthAmount> result = new ArrayList<>();
        for (YearMonth ym = first; !ym.isAfter(month); ym = ym.plusMonths(1)) {
            Object[] row = rows.get(ym);
            result.add(new MonthAmount(ym.getYear(), ym.getMonthValue(),
                    row == null ? 0 : ((Number) row[2]).longValue(),
                    row == null ? 0 : ((Number) row[3]).longValue()));
        }
        return result;
    }

    private List<CategoryAmount> categories(Scope scope, YearMonth month) {
        TypedQuery<CategoryAmount> query = em.createQuery("""
                select new com.bizexpense.domain.expense.dto.CategoryAmount(c.id, c.name, count(e), sum(e.amount))
                from Expense e join e.user u join e.category c
                where %s and e.usedAt between :from and :to
                group by c.id, c.name
                order by sum(e.amount) desc
                """.formatted(scope.condition), CategoryAmount.class);
        bind(query, scope.params(Map.of("from", month.atDay(1), "to", month.atEndOfMonth())));
        return query.getResultList();
    }

    private long count(String jpql, Map<String, Object> params) {
        TypedQuery<Long> query = em.createQuery(jpql, Long.class);
        bind(query, params);
        return query.getSingleResult();
    }

    private long[] sum(String jpql, Map<String, Object> params) {
        TypedQuery<Object[]> query = em.createQuery(jpql, Object[].class);
        bind(query, params);
        Object[] row = query.getSingleResult();
        return new long[] {((Number) row[0]).longValue(), ((Number) row[1]).longValue()};
    }

    private static void bind(TypedQuery<?> query, Map<String, Object> params) {
        params.forEach(query::setParameter);
    }

    private static LocalDateTime start(YearMonth month) {
        return month.atDay(1).atStartOfDay();
    }

    /** 역할에 따른 집계 범위. condition 은 사용자 별칭 u 에 대한 JPQL 조건이다. */
    private record Scope(ViewScope viewScope, String condition, Map<String, Object> base) {

        static Scope of(LoginUser loginUser) {
            if (loginUser.isAdmin()) {
                return new Scope(ViewScope.ALL, "1 = 1", Map.of());
            }
            if (loginUser.isManager() && loginUser.departmentId() != null) {
                return new Scope(ViewScope.TEAM, "u.department.id = :deptId",
                        Map.of("deptId", loginUser.departmentId()));
            }
            return new Scope(ViewScope.ME, "u.id = :userId", Map.of("userId", loginUser.userId()));
        }

        Map<String, Object> params(Map<String, Object> extra) {
            Map<String, Object> all = new HashMap<>(base);
            all.putAll(extra);
            return all;
        }
    }
}
