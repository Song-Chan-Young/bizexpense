import { useEffect, useState } from 'react'
import { SCHEDULE_STATUSES, SCHEDULE_TYPES, scheduleApi, typeColor } from '../../api/schedules'
import Pagination from '../../components/Pagination'
import { formatRange } from '../../utils/date'

const EMPTY_FILTER = { from: '', to: '', type: '', status: '', keyword: '' }

/** 내 일정 목록: 검색 조건 + 페이징 */
export default function ScheduleList({ reloadKey, onRowClick }) {
  const [filter, setFilter] = useState(EMPTY_FILTER)
  const [query, setQuery] = useState({ ...EMPTY_FILTER, page: 0 })
  const [result, setResult] = useState(null)
  const [error, setError] = useState('')

  useEffect(() => {
    // 빈 값은 보내지 않는다 (서버에서 조건 없음으로 처리)
    let ignore = false
    const params = Object.fromEntries(Object.entries(query).filter(([, v]) => v !== ''))
    scheduleApi
      .search({ ...params, size: 10 })
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
  }, [query, reloadKey])

  const set = (key) => (e) => setFilter((f) => ({ ...f, [key]: e.target.value }))

  const handleSearch = (e) => {
    e.preventDefault()
    setQuery({ ...filter, page: 0 })
  }

  const handleReset = () => {
    setFilter(EMPTY_FILTER)
    setQuery({ ...EMPTY_FILTER, page: 0 })
  }

  return (
    <>
      <form className="filter-bar" onSubmit={handleSearch}>
        <input type="date" value={filter.from} onChange={set('from')} aria-label="시작일" />
        <span className="muted">~</span>
        <input type="date" value={filter.to} onChange={set('to')} aria-label="종료일" />
        <select value={filter.type} onChange={set('type')} aria-label="종류">
          <option value="">전체 종류</option>
          {SCHEDULE_TYPES.map((t) => (
            <option key={t.value} value={t.value}>
              {t.label}
            </option>
          ))}
        </select>
        <select value={filter.status} onChange={set('status')} aria-label="상태">
          <option value="">전체 상태</option>
          {SCHEDULE_STATUSES.map((s) => (
            <option key={s.value} value={s.value}>
              {s.label}
            </option>
          ))}
        </select>
        <input placeholder="제목, 장소" value={filter.keyword} onChange={set('keyword')} />
        <button type="submit" className="btn btn-primary">
          검색
        </button>
        <button type="button" className="btn btn-ghost" onClick={handleReset}>
          초기화
        </button>
      </form>

      {error && <p className="form-error">{error}</p>}

      <div className="table-wrap">
        <table className="table">
          <thead>
            <tr>
              <th>종류</th>
              <th>제목</th>
              <th>일시</th>
              <th>장소</th>
              <th>상태</th>
            </tr>
          </thead>
          <tbody>
            {result?.content.map((s) => (
              <tr key={s.scheduleId} className="clickable" onClick={() => onRowClick(s)}>
                <td>
                  <span className="type-badge" style={{ background: typeColor(s.type) }}>
                    {s.typeLabel}
                  </span>
                </td>
                <td>{s.title}</td>
                <td className="nowrap">{formatRange(s.startAt, s.endAt)}</td>
                <td>{s.location || '-'}</td>
                <td>
                  <span className={`status-badge status-${s.status.toLowerCase()}`}>{s.statusLabel}</span>
                </td>
              </tr>
            ))}
            {result?.content.length === 0 && (
              <tr>
                <td colSpan={5} className="empty">
                  조건에 맞는 일정이 없습니다.
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
