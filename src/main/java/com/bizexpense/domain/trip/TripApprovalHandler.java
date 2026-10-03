package com.bizexpense.domain.trip;

import com.bizexpense.domain.approval.ApprovalHandler;
import com.bizexpense.domain.approval.ApprovalTargetType;
import com.bizexpense.global.error.BusinessException;
import com.bizexpense.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** 출장 결재 결과를 출장 상태에 반영한다. */
@Component
@RequiredArgsConstructor
public class TripApprovalHandler implements ApprovalHandler {

    private final TripRepository tripRepository;

    @Override
    public ApprovalTargetType targetType() {
        return ApprovalTargetType.TRIP;
    }

    @Override
    public void onApproved(Long targetId) {
        find(targetId).approve();
    }

    @Override
    public void onRejected(Long targetId) {
        find(targetId).reject();
    }

    private Trip find(Long tripId) {
        return tripRepository.findById(tripId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TRIP_NOT_FOUND));
    }
}
