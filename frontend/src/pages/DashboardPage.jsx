import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { dashboardApi } from '../api/dashboard'
import { useAuth } from '../auth/AuthContext'
import { formatWon } from '../utils/format'
import ScheduleAgenda from './schedule/ScheduleAgenda'

const SCOPE_LABEL = { ME: '내 경비', TEAM: '팀 경비', ALL: '전체 경비' }

/** 차트 축·라벨용 짧은 금액: 1,234,000 -> 123만, 980,000 -> 98만, 9,000 -> 9천 */
const shortWon = (n) => {
  if (n >= 100_000_000) return `${(n / 100_000_000).toFixed(1).replace(/\.0$/, '')}억`
  if (n >= 10_000) return `${Math.round(n / 10_000).toLocaleString('ko-KR')}만`
  if (n >= 1_000) return `${Math.round(n / 1_000)}천`
  return String(n)
}

export default function DashboardPage() {
  const { user } = useAuth()
  const [stats, setStats] = useState(null)
  const [error, setError] = useState('')

  useEffect(() => {
    let ignore = false
    dashboardApi
      .get()
      .then((data) => !ignore && setStats(data))
      .catch((err) => !ignore && setError(err.message))
    return () => {
      ignore = true
    }
  }, [])

  return (
    <>
      {/* 일정이 대시보드 맨 위: 오늘부터 7일 */}
      <ScheduleAgenda />

      <div className="dash-head">
        <h2 className="page-title">대시보드</h2>
        <span className="muted">
          {user.name}님 · {user.departmentName ?? '부서 없음'} · {user.roleLabel}
        </span>
      </div>

      {error && <p className="form-error">{error}</p>}

      <div className="cards">
        {stats
          ? stats.cards.map((c) => (
              <Link key={c.key} to={c.link} className="card card-link">
                <div className="card-title">{c.label}</div>
                <div className="card-value">
                  {c.unit === 'WON' ? formatWon(c.value) : `${c.value.toLocaleString('ko-KR')}건`}
                </div>
                <div className="card-hint">{c.hint}</div>
              </Link>
            ))
          : [0, 1, 2, 3].map((i) => (
              <div key={i} className="card loading">
                <div className="card-title">&nbsp;</div>
                <div className="card-value">–</div>
                <div className="card-hint">&nbsp;</div>
              </div>
            ))}
      </div>

      {stats && (
        <div className="dash-charts">
          <MonthlyChart title={`최근 6개월 ${SCOPE_LABEL[stats.scope]}`} months={stats.monthly} />
          <CategoryChart title={`이번 달 항목별 ${SCOPE_LABEL[stats.scope]}`} categories={stats.categories} />
        </div>
      )}
    </>
  )
}

/** 월별 경비 세로 막대. 이번 달은 진하게, 각 막대에 마우스를 올리면 금액·건수를 보여준다. */
function MonthlyChart({ title, months }) {
  const max = Math.max(...months.map((m) => m.amount), 1)
  const last = months.length - 1
  return (
    <section className="panel chart-panel">
      <h3 className="chart-title">{title}</h3>
      <div className="trend-chart" role="img" aria-label={`${title} 막대 차트`}>
        {months.map((m, i) => (
          <div key={`${m.year}-${m.month}`} className={`trend-col${i === last ? ' current' : ''}`} tabIndex={0}>
            <span className="trend-value">{m.amount > 0 ? shortWon(m.amount) : ''}</span>
            <span className="trend-track">
              <span className="trend-bar" style={{ height: `${(m.amount / max) * 100}%` }} />
            </span>
            <span className="trend-label">{m.month}월</span>
            <span className="chart-tip" role="tooltip">
              <strong>
                {m.year}년 {m.month}월
              </strong>
              {formatWon(m.amount)} · {m.count}건
            </span>
          </div>
        ))}
      </div>
      {/* 화면 읽기 프로그램용 표 */}
      <table className="sr-only">
        <caption>{title}</caption>
        <tbody>
          {months.map((m) => (
            <tr key={`${m.year}-${m.month}`}>
              <th>
                {m.year}년 {m.month}월
              </th>
              <td>{formatWon(m.amount)}</td>
              <td>{m.count}건</td>
            </tr>
          ))}
        </tbody>
      </table>
    </section>
  )
}

/** 항목별 가로 막대 (금액 큰 순). 값은 막대 옆에 직접 표시한다. */
function CategoryChart({ title, categories }) {
  const max = Math.max(...categories.map((c) => c.amount), 1)
  const total = categories.reduce((sum, c) => sum + c.amount, 0)
  return (
    <section className="panel chart-panel">
      <h3 className="chart-title">{title}</h3>
      {categories.length === 0 ? (
        <p className="muted small">이번 달 경비가 없습니다.</p>
      ) : (
        <ul className="hbar-list">
          {categories.map((c) => (
            <li key={c.categoryId} title={`${c.categoryName} ${formatWon(c.amount)} · ${c.count}건`}>
              <span className="hbar-name">{c.categoryName}</span>
              <span className="hbar-track">
                <span className="hbar" style={{ width: `${(c.amount / max) * 100}%` }} />
              </span>
              <span className="hbar-value">
                {formatWon(c.amount)}
                <span className="muted"> {Math.round((c.amount / total) * 100)}%</span>
              </span>
            </li>
          ))}
        </ul>
      )}
    </section>
  )
}
