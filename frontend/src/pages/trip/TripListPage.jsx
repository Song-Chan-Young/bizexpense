import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { TRIP_STATUSES, tripApi } from '../../api/trips'
import { useAuth } from '../../auth/AuthContext'
import Pagination from '../../components/Pagination'
import StatusBadge from '../../components/StatusBadge'
import { formatDate } from '../../utils/date'
import { formatWon } from '../../utils/format'

const SCOPES = {
  USER: [],
  MANAGER: [
    { value: 'ME', label: '내 출장' },
    { value: 'TEAM', label: '팀 출장' },
  ],
  ADMIN: [
    { value: 'ME', label: '내 출장' },
    { value: 'TEAM', label: '팀 출장' },
    { value: 'ALL', label: '전체' },
  ],
}

const EMPTY_FILTER = { from: '', to: '', status: '', keyword: '', userName: '' }

export default function TripListPage() {
  const { user } = useAuth()
  const navigate = useNavigate()
  const [scope, setScope] = useState('ME')
  const [filter, setFilter] = useState(EMPTY_FILTER)
  const [query, setQuery] = useState({ ...EMPTY_FILTER, page: 0 })
  const [result, setResult] = useState(null)
  const [error, setError] = useState('')

  useEffect(() => {
    let ignore = false
    const params = Object.fromEntries(Object.entries(query).filter(([, v]) => v !== ''))
    tripApi
      .search({ ...params, scope, size: 10 })
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
  }, [query, scope])

  const set = (key) => (e) => setFilter((f) => ({ ...f, [key]: e.target.value }))
  const showOwner = scope !== 'ME'

  return (
    <>
      <div className="page-header">
        <h2 className="page-title">출장</h2>
        <Link to="/trips/new" className="btn btn-primary">
          + 출장 등록
        </Link>
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
        <input type="date" value={filter.from} onChange={set('from')} aria-label="기간 시작" />
        <span className="muted">~</span>
        <input type="date" value={filter.to} onChange={set('to')} aria-label="기간 끝" />
        <select value={filter.status} onChange={set('status')} aria-label="상태">
          <option value="">전체 상태</option>
          {TRIP_STATUSES.map((s) => (
            <option key={s.value} value={s.value}>
              {s.label}
            </option>
          ))}
        </select>
        <input placeholder="제목, 출장지" value={filter.keyword} onChange={set('keyword')} />
        {showOwner && <input placeholder="사원명" value={filter.userName} onChange={set('userName')} />}
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

      <div className="table-wrap">
        <table className="table">
          <thead>
            <tr>
              {showOwner && <th>신청자</th>}
              <th>출장명</th>
              <th>출장지</th>
              <th>기간</th>
              <th className="num">예상 경비</th>
              <th>상태</th>
            </tr>
          </thead>
          <tbody>
            {result?.content.map((t) => (
              <tr key={t.tripId} className="clickable" onClick={() => navigate(`/trips/${t.tripId}`)}>
                {showOwner && <td className="nowrap">{t.userName}</td>}
                <td>{t.title}</td>
                <td>{t.destination}</td>
                <td className="nowrap">
                  {formatDate(t.startDate)} ~ {formatDate(t.endDate)}
                  <span className="muted"> · {t.days}일</span>
                </td>
                <td className="num nowrap">{formatWon(t.expectedAmount)}</td>
                <td>
                  <StatusBadge status={t.status} label={t.statusLabel} />
                </td>
              </tr>
            ))}
            {result?.content.length === 0 && (
              <tr>
                <td colSpan={showOwner ? 6 : 5} className="empty">
                  조건에 맞는 출장이 없습니다.
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
