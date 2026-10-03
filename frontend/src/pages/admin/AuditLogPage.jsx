import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { AUDIT_TARGET_TYPES, auditApi } from '../../api/audit'
import Pagination from '../../components/Pagination'
import StatusBadge from '../../components/StatusBadge'
import { formatTimestamp } from '../../utils/date'

const EMPTY_FILTER = { actor: '', targetType: '', action: '', result: '', from: '', to: '' }
const TARGET_LABEL = Object.fromEntries(AUDIT_TARGET_TYPES.map((t) => [t.value, t.label]))
// 대상 종류별 상세 화면 (화면이 없는 대상은 번호만 표시)
const TARGET_LINK = { TRIP: (id) => `/trips/${id}`, SETTLEMENT: (id) => `/settlements/${id}` }

/** 관리자: 감사 로그 (누가 · 언제 · 어디서 · 무엇을 · 결과) */
export default function AuditLogPage() {
  const [actions, setActions] = useState([])
  const [filter, setFilter] = useState(EMPTY_FILTER)
  const [query, setQuery] = useState({ ...EMPTY_FILTER, page: 0 })
  const [result, setResult] = useState(null)
  const [error, setError] = useState('')

  useEffect(() => {
    let ignore = false
    auditApi
      .actions()
      .then((data) => !ignore && setActions(data))
      .catch(() => {})
    return () => {
      ignore = true
    }
  }, [])

  useEffect(() => {
    let ignore = false
    const params = Object.fromEntries(Object.entries(query).filter(([, v]) => v !== ''))
    auditApi
      .search({ ...params, size: 20 })
      .then((data) => {
        if (ignore) return
        setResult(data)
        setError('')
      })
      .catch((err) => !ignore && setError(err.message))
    return () => {
      ignore = true
    }
  }, [query])

  const set = (key) => (e) => setFilter((f) => ({ ...f, [key]: e.target.value }))
  // 대상 종류를 고르면 작업 목록도 그 종류로 좁힌다
  const visibleActions = filter.targetType ? actions.filter((a) => a.targetType === filter.targetType) : actions

  return (
    <>
      <div className="page-header">
        <h2 className="page-title">감사 로그</h2>
      </div>
      <p className="muted">등록 · 수정 · 삭제 · 결재 · 로그인 요청의 결과를 기록합니다. (조회 요청은 남기지 않음)</p>

      <form
        className="filter-bar"
        onSubmit={(e) => {
          e.preventDefault()
          setQuery({ ...filter, page: 0 })
        }}
      >
        <input placeholder="이름 또는 아이디" value={filter.actor} onChange={set('actor')} />
        <select
          value={filter.targetType}
          onChange={(e) => setFilter((f) => ({ ...f, targetType: e.target.value, action: '' }))}
          aria-label="대상"
        >
          <option value="">전체 대상</option>
          {AUDIT_TARGET_TYPES.map((t) => (
            <option key={t.value} value={t.value}>
              {t.label}
            </option>
          ))}
        </select>
        <select value={filter.action} onChange={set('action')} aria-label="작업">
          <option value="">전체 작업</option>
          {visibleActions.map((a) => (
            <option key={a.value} value={a.value}>
              {a.label}
            </option>
          ))}
        </select>
        <select value={filter.result} onChange={set('result')} aria-label="결과">
          <option value="">전체 결과</option>
          <option value="SUCCESS">성공</option>
          <option value="FAILURE">실패</option>
        </select>
        <input type="date" value={filter.from} onChange={set('from')} aria-label="기록일 시작" />
        <span className="muted">~</span>
        <input type="date" value={filter.to} onChange={set('to')} aria-label="기록일 끝" />
        <button type="submit" className="btn btn-primary">
          검색
        </button>
      </form>

      {error && <p className="form-error">{error}</p>}

      <div className="table-wrap">
        <table className="table">
          <thead>
            <tr>
              <th>일시</th>
              <th>수행자</th>
              <th>작업</th>
              <th>대상</th>
              <th>결과</th>
              <th>요청</th>
              <th>IP</th>
            </tr>
          </thead>
          <tbody>
            {result?.content.map((a) => (
              <tr key={a.auditLogId}>
                <td className="nowrap">{formatTimestamp(a.createdAt)}</td>
                <td className="nowrap">
                  {a.actorName ?? <span className="muted">(알 수 없음)</span>}
                  {a.actorLoginId && <span className="muted"> · {a.actorLoginId}</span>}
                </td>
                <td className="nowrap">{a.actionLabel}</td>
                <td className="nowrap">
                  {TARGET_LABEL[a.targetType] ?? a.targetType}
                  {a.targetId != null &&
                    (TARGET_LINK[a.targetType] && a.result === 'SUCCESS' && !a.action.endsWith('DELETE') ? (
                      <Link className="link" to={TARGET_LINK[a.targetType](a.targetId)}>
                        {' '}
                        #{a.targetId}
                      </Link>
                    ) : (
                      <span className="muted"> #{a.targetId}</span>
                    ))}
                </td>
                <td className="nowrap">
                  <StatusBadge status={a.result} label={a.result === 'SUCCESS' ? '성공' : '실패'} />
                  {a.errorCode && <span className="muted audit-error"> {a.errorCode}</span>}
                </td>
                <td className="nowrap audit-uri">
                  <span className="muted">{a.httpMethod}</span> {a.uri}
                </td>
                <td className="nowrap muted">{a.ip}</td>
              </tr>
            ))}
            {result?.content.length === 0 && (
              <tr>
                <td colSpan={7} className="empty">
                  조건에 맞는 기록이 없습니다.
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
