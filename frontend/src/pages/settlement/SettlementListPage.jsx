import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { SETTLEMENT_STATUSES, settlementApi } from '../../api/settlements'
import { useAuth } from '../../auth/AuthContext'
import Pagination from '../../components/Pagination'
import StatusBadge from '../../components/StatusBadge'
import { formatDate, formatTimestamp } from '../../utils/date'
import { formatWon } from '../../utils/format'

const SCOPES = {
  USER: [],
  MANAGER: [
    { value: 'ME', label: '내 정산' },
    { value: 'TEAM', label: '팀 정산' },
  ],
  ADMIN: [
    { value: 'ME', label: '내 정산' },
    { value: 'TEAM', label: '팀 정산' },
    { value: 'ALL', label: '전체' },
  ],
}

const EMPTY_FILTER = { status: '', userName: '', from: '', to: '' }

export default function SettlementListPage() {
  const { user } = useAuth()
  const navigate = useNavigate()
  // 관리자는 지급 처리할 건(승인 완료)을 바로 볼 수 있게 전체 · 승인 상태로 시작
  const isAdmin = user.role === 'ADMIN'
  const [scope, setScope] = useState(isAdmin ? 'ALL' : 'ME')
  const [filter, setFilter] = useState(() => ({ ...EMPTY_FILTER, status: isAdmin ? 'APPROVED' : '' }))
  const [query, setQuery] = useState(() => ({ ...filter, page: 0 }))
  const [result, setResult] = useState(null)
  const [error, setError] = useState('')

  useEffect(() => {
    let ignore = false
    const params = Object.fromEntries(Object.entries(query).filter(([, v]) => v !== ''))
    settlementApi
      .search({ ...params, scope, size: 10 })
      .then((data) => {
        if (ignore) return
        setResult(data)
        setError('')
      })
      .catch((err) => !ignore && setError(err.message))
    return () => {
      ignore = true
    }
  }, [query, scope])

  const set = (key) => (e) => setFilter((f) => ({ ...f, [key]: e.target.value }))
  const showOwner = scope !== 'ME'

  return (
    <>
      <div className="page-header">
        <h2 className="page-title">정산</h2>
      </div>
      <p className="muted">정산 신청은 완료된 출장의 상세 화면에서 할 수 있습니다.</p>

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
        <select value={filter.status} onChange={set('status')} aria-label="상태">
          <option value="">전체 상태</option>
          {SETTLEMENT_STATUSES.map((s) => (
            <option key={s.value} value={s.value}>
              {s.label}
            </option>
          ))}
        </select>
        <input type="date" value={filter.from} onChange={set('from')} aria-label="신청일 시작" />
        <span className="muted">~</span>
        <input type="date" value={filter.to} onChange={set('to')} aria-label="신청일 끝" />
        {showOwner && <input placeholder="신청자" value={filter.userName} onChange={set('userName')} />}
        <button type="submit" className="btn btn-primary">
          검색
        </button>
      </form>

      {error && <p className="form-error">{error}</p>}

      <div className="table-wrap">
        <table className="table">
          <thead>
            <tr>
              {showOwner && <th>신청자</th>}
              <th>출장</th>
              <th>신청일시</th>
              <th className="num">총 경비</th>
              <th className="num">법인카드</th>
              <th className="num">개인 지급액</th>
              <th>상태</th>
            </tr>
          </thead>
          <tbody>
            {result?.content.map((s) => (
              <tr key={s.settlementId} className="clickable" onClick={() => navigate(`/settlements/${s.settlementId}`)}>
                {showOwner && (
                  <td className="nowrap">
                    {s.userName}
                    {s.departmentName && <span className="muted"> · {s.departmentName}</span>}
                  </td>
                )}
                <td>
                  {s.tripTitle}
                  <span className="muted">
                    {' '}
                    ({formatDate(s.tripStartDate)} ~ {formatDate(s.tripEndDate)})
                  </span>
                </td>
                <td className="nowrap">{formatTimestamp(s.requestedAt)}</td>
                <td className="num nowrap">{formatWon(s.totalAmount)}</td>
                <td className="num nowrap">{formatWon(s.corporateAmount)}</td>
                <td className="num nowrap">
                  <strong>{formatWon(s.payableAmount)}</strong>
                </td>
                <td>
                  <StatusBadge status={s.status} label={s.statusLabel} />
                </td>
              </tr>
            ))}
            {result?.content.length === 0 && (
              <tr>
                <td colSpan={showOwner ? 7 : 6} className="empty">
                  조건에 맞는 정산이 없습니다.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>

      {result && (
        <Pagination
          page={result.page}
          totalPages={result.totalPages}
          totalElements={result.totalElements}
          onChange={(page) => setQuery((q) => ({ ...q, page }))}
        />
      )}
    </>
  )
}
