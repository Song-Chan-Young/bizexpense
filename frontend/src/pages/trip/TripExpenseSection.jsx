import { useEffect, useState } from 'react'
import { expenseApi } from '../../api/expenses'
import { useAuth } from '../../auth/AuthContext'
import { formatWon } from '../../utils/format'
import ExpenseDetailModal from '../expense/ExpenseDetailModal'
import ExpenseFormModal from '../expense/ExpenseFormModal'
import ExpenseTable from '../expense/ExpenseTable'

/** 출장 상세의 경비 영역: 요약(총액/법인/개인 부담/항목별) + 경비 목록 + 등록 */
export default function TripExpenseSection({ trip }) {
  const { user } = useAuth()
  const [summary, setSummary] = useState(null)
  const [expenses, setExpenses] = useState([])
  const [modal, setModal] = useState(null)
  const [reloadKey, setReloadKey] = useState(0)

  // 목록 조회 범위: 본인 출장이면 ME, 아니면 팀장 TEAM / 관리자 ALL
  const scope = trip.userId === user.userId ? 'ME' : user.role === 'ADMIN' ? 'ALL' : 'TEAM'

  useEffect(() => {
    let ignore = false
    Promise.all([
      expenseApi.tripSummary(trip.tripId),
      expenseApi.search({ tripId: trip.tripId, scope, size: 100, sort: 'usedAt,asc' }),
    ])
      .then(([s, list]) => {
        if (ignore) return
        setSummary(s)
        setExpenses(list.page.content)
      })
      .catch(() => {})
    return () => {
      ignore = true
    }
  }, [trip.tripId, scope, reloadKey])

  const close = () => setModal(null)
  const reloadAndClose = () => {
    close()
    setReloadKey((k) => k + 1)
  }

  if (!summary) return null
  const usage = summary.expectedAmount > 0 ? Math.round((summary.totalAmount / summary.expectedAmount) * 100) : null

  return (
    <section className="panel">
      <div className="panel-header">
        <h3>경비</h3>
        {trip.actions.addExpense && (
          <button
            type="button"
            className="btn btn-ghost"
            onClick={() => setModal({ mode: 'form', expense: { tripId: trip.tripId, usedAt: trip.startDate } })}
          >
            + 경비 등록
          </button>
        )}
      </div>

      <div className="stat-row">
        <div className="stat">
          <span className="stat-label">사용 합계</span>
          <strong>{formatWon(summary.totalAmount)}</strong>
          {usage != null && (
            <span className={`stat-sub ${usage > 100 ? 'danger-text' : ''}`}>
              예상 {formatWon(summary.expectedAmount)} 대비 {usage}%
            </span>
          )}
        </div>
        <div className="stat">
          <span className="stat-label">법인카드</span>
          <strong>{formatWon(summary.corporateAmount)}</strong>
          <span className="stat-sub">회사 결제분</span>
        </div>
        <div className="stat">
          <span className="stat-label">개인 부담</span>
          <strong>{formatWon(summary.personalAmount)}</strong>
          <span className="stat-sub">정산 시 지급 예정</span>
        </div>
      </div>

      {summary.byCategory.length > 0 && (
        <ul className="category-bars">
          {summary.byCategory.map((c) => (
            <li key={c.categoryId}>
              <span className="bar-label">{c.categoryName}</span>
              <span className="bar-track">
                <span className="bar-fill" style={{ width: `${(c.amount / summary.totalAmount) * 100}%` }} />
              </span>
              <span className="bar-value">
                {formatWon(c.amount)} <span className="muted">· {c.count}건</span>
              </span>
            </li>
          ))}
        </ul>
      )}

      <ExpenseTable
        expenses={expenses}
        emptyText={
          trip.actions.addExpense ? '등록된 경비가 없습니다.' : '경비는 출장을 시작한 뒤에 등록할 수 있습니다.'
        }
        onRowClick={(e) => setModal({ mode: 'detail', expense: e })}
      />

      {modal?.mode === 'form' && <ExpenseFormModal expense={modal.expense} onClose={close} onSaved={reloadAndClose} />}
      {modal?.mode === 'detail' && (
        <ExpenseDetailModal
          expense={modal.expense}
          onClose={close}
          onEdit={() => setModal({ mode: 'form', expense: modal.expense })}
          onDeleted={reloadAndClose}
        />
      )}
    </section>
  )
}
