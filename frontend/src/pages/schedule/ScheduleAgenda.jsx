import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { scheduleApi, typeColor } from '../../api/schedules'
import { useAuth } from '../../auth/AuthContext'
import StatusBadge from '../../components/StatusBadge'
import { toDateParam } from '../../utils/date'
import ScheduleDetailModal from './ScheduleDetailModal'
import ScheduleFormModal from './ScheduleFormModal'

const DAYS = 7
const pad = (n) => String(n).padStart(2, '0')
const hhmm = (d) => `${pad(d.getHours())}:${pad(d.getMinutes())}`

/**
 * 오늘부터 7일간 일정을 날짜별로 한눈에 보여준다 (대시보드 상단).
 * 여러 날에 걸친 일정은 해당하는 날마다 표시한다.
 */
export default function ScheduleAgenda() {
  const { user } = useAuth()
  const canSeeTeam = user.role === 'MANAGER' || user.role === 'ADMIN'
  const [scope, setScope] = useState('ME')
  const [items, setItems] = useState(null)
  const [modal, setModal] = useState(null)
  const [reloadKey, setReloadKey] = useState(0)
  const [today] = useState(() => {
    const d = new Date()
    d.setHours(0, 0, 0, 0)
    return d
  })

  useEffect(() => {
    let ignore = false
    const end = new Date(today)
    end.setDate(end.getDate() + DAYS)
    scheduleApi
      .calendar(toDateParam(today), toDateParam(end), scope)
      .then((data) => !ignore && setItems(data))
      .catch(() => !ignore && setItems([]))
    return () => {
      ignore = true
    }
  }, [today, scope, reloadKey])

  const close = () => setModal(null)
  const reloadAndClose = () => {
    close()
    setReloadKey((k) => k + 1)
  }

  const days = Array.from({ length: DAYS }, (_, i) => {
    const start = new Date(today)
    start.setDate(start.getDate() + i)
    const end = new Date(start)
    end.setDate(end.getDate() + 1)
    const list = (items ?? []).filter((s) => new Date(s.startAt) < end && new Date(s.endAt) > start)
    return { date: start, list }
  })
  const total = items?.length ?? 0

  return (
    <section className="panel agenda">
      <div className="panel-header">
        <h3>
          이번 주 일정 <span className="muted">· {total}건</span>
        </h3>
        <div className="button-row">
          {canSeeTeam && (
            <div className="tabs tabs-sm">
              <button type="button" className={scope === 'ME' ? 'active' : ''} onClick={() => setScope('ME')}>
                내 일정
              </button>
              <button type="button" className={scope === 'TEAM' ? 'active' : ''} onClick={() => setScope('TEAM')}>
                팀 일정
              </button>
            </div>
          )}
          <Link to="/schedules" className="btn btn-ghost btn-sm">
            캘린더 →
          </Link>
        </div>
      </div>

      {items && (
        <ol className="agenda-days">
          {days.map(({ date, list }, i) => (
            <li key={date.toISOString()} className={i === 0 ? 'today' : ''}>
              <div className="agenda-date">
                <strong>
                  {date.getMonth() + 1}/{date.getDate()}
                </strong>
                <span>{i === 0 ? '오늘' : `${'일월화수목금토'[date.getDay()]}요일`}</span>
              </div>
              {list.length === 0 ? (
                <p className="agenda-empty">일정 없음</p>
              ) : (
                <ul className="agenda-items">
                  {list.map((s) => (
                    <li
                      key={s.scheduleId}
                      className={s.status === 'CANCELLED' ? 'cancelled' : ''}
                      onClick={() => setModal({ mode: 'detail', schedule: s })}
                    >
                      <i style={{ background: typeColor(s.type) }} />
                      <span className="agenda-time">{timeLabel(s, date)}</span>
                      <span className="agenda-body">
                        <span className="agenda-title">
                          {scope !== 'ME' && <span className="muted">[{s.userName}] </span>}
                          {s.title}
                        </span>
                        <span className="agenda-meta">
                          {s.typeLabel}
                          {s.location && ` · ${s.location}`}
                          {s.tripTitle && <span className="tag">{s.tripTitle}</span>}
                        </span>
                      </span>
                      {s.status !== 'PLANNED' && <StatusBadge status={s.status} label={s.statusLabel} />}
                    </li>
                  ))}
                </ul>
              )}
            </li>
          ))}
        </ol>
      )}

      {modal?.mode === 'detail' && (
        <ScheduleDetailModal
          schedule={modal.schedule}
          onClose={close}
          onEdit={() => setModal({ mode: 'form', schedule: modal.schedule })}
          onDeleted={reloadAndClose}
        />
      )}
      {modal?.mode === 'form' && (
        <ScheduleFormModal schedule={modal.schedule} onClose={close} onSaved={reloadAndClose} />
      )}
    </section>
  )
}

/** 그 날 기준 시간 표시. 전날부터 이어지거나 다음날까지 가는 일정은 '종일'/'~' 로 표시 */
function timeLabel(s, day) {
  const start = new Date(s.startAt)
  const end = new Date(s.endAt)
  const dayEnd = new Date(day)
  dayEnd.setDate(dayEnd.getDate() + 1)
  const startsBefore = start < day
  const endsAfter = end > dayEnd
  if (startsBefore && endsAfter) return '종일'
  if (startsBefore) return `~ ${hhmm(end)}`
  if (endsAfter) return `${hhmm(start)} ~`
  return `${hhmm(start)} ~ ${hhmm(end)}`
}
