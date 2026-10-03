import { useEffect, useState } from 'react'
import { useExpenseCodes } from '../../api/codes'
import { EXPENSE_STATUSES, expenseApi } from '../../api/expenses'
import { useAuth } from '../../auth/AuthContext'
import Pagination from '../../components/Pagination'
import ExpenseDetailModal from './ExpenseDetailModal'
import ExpenseFormModal from './ExpenseFormModal'
import ExpenseTable from './ExpenseTable'

const SCOPES = {
  USER: [],
  MANAGER: [
    { value: 'ME', label: '내 경비' },
    { value: 'TEAM', label: '팀 경비' },
  ],
  ADMIN: [
    { value: 'ME', label: '내 경비' },
    { value: 'TEAM', label: '팀 경비' },
    { value: 'ALL', label: '전체' },
  ],
}

const EMPTY_FILTER = {
  from: '',
  to: '',
  categoryId: '',
  paymentMethodId: '',
  status: '',
  minAmount: '',
  maxAmount: '',
  keyword: '',
  userName: '',
}

export default function ExpenseListPage() {
  const { user } = useAuth()
  const { categories, paymentMethods } = useExpenseCodes()
  const [scope, setScope] = useState('ME')
  const [filter, setFilter] = useState(EMPTY_FILTER)
  const [query, setQuery] = useState({ ...EMPTY_FILTER, page: 0 })
  const [result, setResult] = useState(null)
  const [error, setError] = useState('')
  // { mode: 'form' | 'detail', expense }
  const [modal, setModal] = useState(null)
  const [reloadKey, setReloadKey] = useState(0)

  useEffect(() => {
    let ignore = false
    const params = Object.fromEntries(Object.entries(query).filter(([, v]) => v !== ''))
    expenseApi
      .search({ ...params, scope, size: 15 })
      .then((data) => {
        if (ignore) return
        setResult(data)
        setError('')
      })
      .catch((err) => !ignore && setError(err.message))
    return () => {
      ignore = true
    }
  }, [query, scope, reloadKey])

  const set = (key) => (e) => setFilter((f) => ({ ...f, [key]: e.target.value }))
  const close = () => setModal(null)
  const reloadAndClose = () => {
    close()
    setReloadKey((k) => k + 1)
  }

  return (
    <>
      <div className="page-header">
        <h2 className="page-title">경비</h2>
        <button type="button" className="btn btn-primary" onClick={() => setModal({ mode: 'form', expense: {} })}>
          + 경비 등록
        </button>
      </div>

      {SCOPES[user.role].length > 0 && (
        <div className="toolbar">
          <div className="tabs">
            {SCOPES[user.role].map((s) => (
              <button
                key={s.value}
                type="button"
                className={scope === s.value ? 'active' : ''}
                onClick={() => {
                  setScope(s.value)
                  setQuery((q) => ({ ...q, page: 0 }))
                }}
              >
                {s.label}
              </button>
            ))}
          </div>
        </div>
      )}

      <form
        className="filter-bar"
        onSubmit={(e) => {
          e.preventDefault()
          setQuery({ ...filter, page: 0 })
        }}
      >
        <input type="date" value={filter.from} onChange={set('from')} aria-label="사용일 시작" />
        <span className="muted">~</span>
        <input type="date" value={filter.to} onChange={set('to')} aria-label="사용일 끝" />
        <select value={filter.categoryId} onChange={set('categoryId')} aria-label="비용 항목">
          <option value="">전체 항목</option>
          {categories.map((c) => (
            <option key={c.categoryId} value={c.categoryId}>
              {c.name}
            </option>
          ))}
        </select>
        <select value={filter.paymentMethodId} onChange={set('paymentMethodId')} aria-label="결제 수단">
          <option value="">전체 결제 수단</option>
          {paymentMethods.map((p) => (
            <option key={p.paymentMethodId} value={p.paymentMethodId}>
              {p.name}
            </option>
          ))}
        </select>
        <select value={filter.status} onChange={set('status')} aria-label="상태">
          <option value="">전체 상태</option>
          {EXPENSE_STATUSES.map((s) => (
            <option key={s.value} value={s.value}>
              {s.label}
            </option>
          ))}
        </select>
        <input
          type="number"
          className="amount-input"
          placeholder="최소 금액"
          value={filter.minAmount}
          onChange={set('minAmount')}
          min={0}
        />
        <span className="muted">~</span>
        <input
          type="number"
          className="amount-input"
          placeholder="최대 금액"
          value={filter.maxAmount}
          onChange={set('maxAmount')}
          min={0}
        />
        <input placeholder="사용처, 내용" value={filter.keyword} onChange={set('keyword')} />
        {scope !== 'ME' && <input placeholder="사원명" value={filter.userName} onChange={set('userName')} />}
        <button type="submit" className="btn btn-primary">
          검색
        </button>
        <button
          type="button"
          className="btn btn-ghost"
          onClick={() => {
            setFilter(EMPTY_FILTER)
            setQuery({ ...EMPTY_FILTER, page: 0 })
          }}
        >
          초기화
        </button>
      </form>

      {error && <p className="form-error">{error}</p>}

      {result && (
        <>
          <ExpenseTable
            expenses={result.page.content}
            showTrip
            showOwner={scope !== 'ME'}
            totalAmount={result.totalAmount}
            emptyText="조건에 맞는 경비가 없습니다."
            onRowClick={(e) => setModal({ mode: 'detail', expense: e })}
          />
          <Pagination
            page={result.page.page}
            totalPages={result.page.totalPages}
            totalElements={result.page.totalElements}
            onChange={(page) => setQuery((q) => ({ ...q, page }))}
          />
        </>
      )}

      {modal?.mode === 'form' && <ExpenseFormModal expense={modal.expense} onClose={close} onSaved={reloadAndClose} />}
      {modal?.mode === 'detail' && (
        <ExpenseDetailModal
          expense={modal.expense}
          onClose={close}
          onEdit={() => setModal({ mode: 'form', expense: modal.expense })}
          onDeleted={reloadAndClose}
        />
      )}
    </>
  )
}
