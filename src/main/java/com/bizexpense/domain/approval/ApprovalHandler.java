package com.bizexpense.domain.approval;

/**
 * 결재 결과를 대상 업무(출장, 정산 …)에 반영한다.
 * 결재 처리와 같은 트랜잭션에서 호출되므로, 대상 상태 변경이 실패하면 결재 처리도 함께 롤백된다.
 */
public interface ApprovalHandler {

    ApprovalTargetType targetType();

    void onApproved(Long targetId);

    void onRejected(Long targetId);
}
