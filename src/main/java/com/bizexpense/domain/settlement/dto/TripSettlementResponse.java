package com.bizexpense.domain.settlement.dto;

import com.bizexpense.domain.settlement.SettlementAmounts;
import java.util.List;

/**
 * 출장 상세의 정산 영역.
 *
 * @param canRequest  지금 정산을 신청할 수 있는지
 * @param reason      신청할 수 없으면 그 이유 (화면 안내용)
 * @param preview     지금 신청하면 포함될 경비의 금액 계산
 * @param settlements 이 출장의 정산 이력 (최근 순)
 */
public record TripSettlementResponse(
        Long tripId,
        boolean canRequest,
        String reason,
        SettlementAmounts preview,
        List<SettlementResponse> settlements) {
}
