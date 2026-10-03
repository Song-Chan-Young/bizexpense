package com.bizexpense.domain.schedule;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ScheduleRepository extends JpaRepository<Schedule, Long>, JpaSpecificationExecutor<Schedule> {

    @Query("""
            select s from Schedule s
            join fetch s.user u
            left join fetch u.department
            left join fetch s.trip
            where s.id = :id
            """)
    Optional<Schedule> findDetailById(@Param("id") Long id);

    /** 출장에 연결된 일정 (시간순) */
    @EntityGraph(attributePaths = "user")
    List<Schedule> findByTripIdOrderByStartAtAsc(Long tripId);

    /**
     * 캘린더 조회: [from, to) 기간과 겹치는 일정. userId / departmentId 가 null 이면 해당 조건을 쓰지 않는다.
     */
    @Query("""
            select s from Schedule s
            join fetch s.user u
            left join fetch s.trip
            where s.startAt < :to
              and s.endAt > :from
              and (:userId is null or u.id = :userId)
              and (:departmentId is null or u.department.id = :departmentId)
            order by s.startAt
            """)
    List<Schedule> findForCalendar(@Param("from") LocalDateTime from,
                                   @Param("to") LocalDateTime to,
                                   @Param("userId") Long userId,
                                   @Param("departmentId") Long departmentId);

    /**
     * 같은 사원의 일정 중 [startAt, endAt) 과 시간이 겹치는 일정.
     * 두 구간이 겹치는 조건: 기존.시작 < 신규.종료 AND 기존.종료 > 신규.시작
     * 끝나는 시각과 시작 시각이 같은 연속 일정(10:00~11:00, 11:00~12:00)은 겹치지 않는 것으로 본다.
     * 취소된 일정과 수정 중인 자기 자신(excludeId)은 제외한다.
     */
    @Query("""
            select s from Schedule s
            where s.user.id = :userId
              and s.status <> com.bizexpense.domain.schedule.ScheduleStatus.CANCELLED
              and s.startAt < :endAt
              and s.endAt > :startAt
              and (:excludeId is null or s.id <> :excludeId)
            order by s.startAt
            """)
    List<Schedule> findOverlapping(@Param("userId") Long userId,
                                   @Param("startAt") LocalDateTime startAt,
                                   @Param("endAt") LocalDateTime endAt,
                                   @Param("excludeId") Long excludeId);
}
