import { useEffect, useState } from 'react'
import { APPROVAL_STATUSES, APPROVAL_TARGET_TYPES, approvalApi } from '../../api/approvals'
import { useAuth } from '../../auth/AuthContext'
import Pagination from '../../components/Pagination'
import StatusBadge from '../../components/StatusBadge'
import { formatTimestamp } from '../../utils/date'
import ApprovalModal from './ApprovalModal'

const BOXES = [
  { value: 'INBOX', label: '결재함' },
  { value: 'SENT', label: '내 신청' },
  { value: 'ALL', label: '전체', adminOnly: true },
]

const EMPTY_FILTER = { status: 'PENDING', targetType: '', requesterName: '', from: '', to: '' }

export default function ApprovalPage() {
  const { user } = useAuth()
  const [box, setBox] = useState('INBOX')
  const [filter, setFilter] = useState(EMPTY_FILTER)
  const [query, setQuery] = useState({ ...EMPTY_FILTER, page: 0 })
  const [result, setResult] = useState(null)
  const [selected, setSelected] = useState(null)
  const [reloadKey, setReloadKey] = useState(0)
  const [error, setError] = useState('')

  useEffect(() => {
    let ignore = false
    const params = Object.fromEntries(Object.entries(query).filter(([, v]) => v !== ''))
    approvalApi
      .search({ ...params, box, size: 10 })
      .then((data) => {
        if (ignore) return
        setResult(data)
        setError('')
      })
      .catch((err) => !ignore && setError(err.message))
    // 검색 조건을 빠르게 바꿀 때 늦게 도착한 이전 응답은 버린다
    return () => {
      ignore = true
    }
  }, [query, box, reloadKey])

  const set = (key) => (e) => setFilter((f) => ({ ...f, [key]: e.target.value }))

  return (
    <>
      <div className="page-header">
        <h2 className="page-title">결재</h2>
      </div>

      <div className="toolbar">
        <div className="tabs">
          {BOXES.filter((b) => !b.adminOnly || user.role === 'ADMIN').map((b) => (
            <button
              key={b.value}
              type="button"
              className={box === b.value ? 'active' : ''}
              onClick={() => {
                setBox(b.value)
                setQuery((q) => ({ ...q, page: 0 }))
              }}
            >
              {b.label}
            </button>
          ))}
        </div>
      </div>

      <form
        className="filter-bar"
        onSubmit={(e) => {
          e.preventDefault()
          setQuery({ ...filter, page: 0 })
        }}
      >
        <select value={filter.status} onChange={set('status')} aria-label="결재 상태">
          <option value="">전체 상태</option>
          {APPROVAL_STATUSES.map((s) => (
            <option key={s.value} value={s.value}>
              {s.label}
            </option>
          ))}
        </select>
        <select value={filter.targetType} onChange={set('targetType')} aria-label="구분">
          <option value="">전체 구분</option>
          {APPROVAL_TARGET_TYPES.map((t) => (
            <option key={t.value} value={t.value}>
              {t.label}
            </option>
          ))}
        </select>
        <input type="date" value={filter.from} onChange={set('from')} aria-label="신청일 시작" />
        <span className="muted">~</span>
        <input type="date" value={filter.to} onChange={set('to')} aria-label="신청일 끝" />
        {box !== 'SENT' && <input placeholder="신청자" value={filter.requesterName} onChange={set('requesterName')} />}
        <button type="submit" className="btn btn-primary">
          검색
        </button>
      </form>

      {error && <p className="form-error">{error}</p>}

      <div className="table-wrap">
        <table className="table">
          <thead>
            <tr>
              <th>구분</th>
              <th>제목</th>
              <th>신청자</th>
              <th>결재자</th>
              <th>신청일시</th>
              <th>상태</th>
            </tr>
          </thead>
          <tbody>
            {result?.content.map((a) => (
              <tr key={a.approvalId} className="clickable" onClick={() => setSelected(a)}>
                <td>{a.targetTypeLabel}</td>
                <td>{a.title}</td>
                <td className="nowrap">
                  {a.requesterName}
                  {a.requesterDepartmentName && <span className="muted"> · {a.requesterDepartmentName}</span>}
                </td>
                <td className="nowrap">{a.approverName}</td>
                <td className="nowrap">{formatTimestamp(a.requestedAt)}</td>
                <td>
                  <StatusBadge status={a.status} label={a.statusLabel} />
                </td>
              </tr>
            ))}
            {result?.content.length === 0 && (
              <tr>
                <td colSpan={6} className="empty">
                  {query.status === 'PENDING' && box === 'INBOX' ? '결재할 건이 없습니다.' : '결재 건이 없습니다.'}
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

      {selected && (
        <ApprovalModal
          approval={selected}
          onClose={() => setSelected(null)}
          onProcessed={() => {
            setSelected(null)
            setReloadKey((k) => k + 1)
          }}
        />
      )}
    </>
  )
}
