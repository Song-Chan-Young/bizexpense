import StatusBadge from '../../components/StatusBadge'
import { formatDate } from '../../utils/date'
import { formatWon } from '../../utils/format'

/**
 * 경비 목록 표 (경비 목록 화면, 출장 상세에서 공통 사용)
 * showTrip: 출장 열 표시, showOwner: 등록자 열 표시, totalAmount: 있으면 합계 행 표시
 */
export default function ExpenseTable({ expenses, showTrip, showOwner, totalAmount, onRowClick, emptyText }) {
  const columns = 6 + (showTrip ? 1 : 0) + (showOwner ? 1 : 0)

  return (
    <div className="table-wrap">
      <table className="table">
        <thead>
          <tr>
            <th>사용일</th>
            {showOwner && <th>등록자</th>}
            {showTrip && <th>출장</th>}
            <th>비용 항목</th>
            <th>사용처</th>
            <th>결제 수단</th>
            <th className="num">금액</th>
            <th>상태</th>
          </tr>
        </thead>
        <tbody>
          {expenses.map((e) => (
            <tr key={e.expenseId} className="clickable" onClick={() => onRowClick(e)}>
              <td className="nowrap">{formatDate(e.usedAt)}</td>
              {showOwner && <td className="nowrap">{e.userName}</td>}
              {showTrip && <td>{e.tripTitle}</td>}
              <td className="nowrap">{e.categoryName}</td>
              <td>{e.storeName}</td>
              <td className="nowrap">
                {e.paymentMethodName}
                {e.corporate && <span className="tag">법인</span>}
              </td>
              <td className="num nowrap">{formatWon(e.amount)}</td>
              <td>
                <StatusBadge status={e.status} label={e.statusLabel} />
              </td>
            </tr>
          ))}
          {expenses.length === 0 && (
            <tr>
              <td colSpan={columns} className="empty">
                {emptyText ?? '경비가 없습니다.'}
              </td>
            </tr>
          )}
        </tbody>
        {totalAmount != null && expenses.length > 0 && (
          <tfoot>
            <tr>
              <td colSpan={columns - 2}>합계 (검색 조건 전체)</td>
              <td className="num nowrap">{formatWon(totalAmount)}</td>
              <td />
            </tr>
          </tfoot>
        )}
      </table>
    </div>
  )
}
