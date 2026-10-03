package com.bizexpense.domain.settlement;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SettlementRepository extends JpaRepository<Settlement, Long>, JpaSpecificationExecutor<Settlement> {

    @Query("""
            select s from Settlement s
            join fetch s.trip
            join fetch s.user u
            left join fetch u.department
            where s.id = :id
            """)
    Optional<Settlement> findDetailById(@Param("id") Long id);

    @EntityGraph(attributePaths = "trip")
    List<Settlement> findByTripIdOrderByIdDesc(Long tripId);

    boolean existsByTripIdAndStatusIn(Long tripId, Collection<SettlementStatus> statuses);
}
