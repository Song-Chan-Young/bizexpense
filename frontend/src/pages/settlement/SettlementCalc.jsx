import { formatWon } from '../../utils/format'

/**
 * 정산 금액 계산표
 *   총 경비      153,000원
 * − 법인카드      63,000원
 * = 개인 지급액   90,000원
 */
export default function SettlementCalc({ amounts }) {
  return (
    <dl className="settle-calc">
      <dt>총 경비 ({amounts.expenseCount}건)</dt>
      <dd>{formatWon(amounts.totalAmount)}</dd>
      <dt>
        <span className="op">−</span> 법인카드 (회사 결제)
      </dt>
      <dd>{formatWon(amounts.corporateAmount)}</dd>
      <dt className="result">
        <span className="op">=</span> 개인 지급액
      </dt>
      <dd className="result">{formatWon(amounts.payableAmount)}</dd>
    </dl>
  )
}
