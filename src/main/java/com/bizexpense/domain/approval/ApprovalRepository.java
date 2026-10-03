package com.bizexpense.domain.approval;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ApprovalRepository extends JpaRepository<Approval, Long>, JpaSpecificationExecutor<Approval> {

    /** 대상별 결재 이력 (최근 순) */
    @EntityGraph(attributePaths = {"requester", "approver"})
    List<Approval> findByTargetTypeAndTargetIdOrderByIdDesc(ApprovalTargetType targetType, Long targetId);

    Optional<Approval> findByTargetTypeAndTargetIdAndStatus(ApprovalTargetType targetType, Long targetId,
                                                            ApprovalStatus status);
}
