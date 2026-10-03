package com.bizexpense.domain.trip;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TripRepository extends JpaRepository<Trip, Long>, JpaSpecificationExecutor<Trip> {

    @Query("""
            select t from Trip t
            join fetch t.user u
            left join fetch u.department
            where t.id = :id
            """)
    Optional<Trip> findDetailById(@Param("id") Long id);

    /** 일정 등록 화면에서 고를 수 있는 내 출장 목록 */
    @EntityGraph(attributePaths = "user")
    List<Trip> findByUserIdAndStatusInOrderByStartDateDesc(Long userId, Collection<TripStatus> statuses);
}
