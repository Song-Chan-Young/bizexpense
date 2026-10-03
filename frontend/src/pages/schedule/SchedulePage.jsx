import { useState } from 'react'
import { SCHEDULE_TYPES } from '../../api/schedules'
import { useAuth } from '../../auth/AuthContext'
import { toLocalInput } from '../../utils/date'
import ScheduleCalendar from './ScheduleCalendar'
import ScheduleDetailModal from './ScheduleDetailModal'
import ScheduleFormModal from './ScheduleFormModal'
import ScheduleList from './ScheduleList'

const SCOPES = {
  USER: [{ value: 'ME', label: '내 일정' }],
  MANAGER: [
    { value: 'ME', label: '내 일정' },
    { value: 'TEAM', label: '팀 일정' },
  ],
  ADMIN: [
    { value: 'ME', label: '내 일정' },
    { value: 'TEAM', label: '팀 일정' },
    { value: 'ALL', label: '전체' },
  ],
}

/** 새 일정 기본값: 다음 정각부터 1시간 */
function defaultRange() {
  const start = new Date()
  start.setHours(start.getHours() + 1, 0, 0, 0)
  const end = new Date(start)
  end.setHours(end.getHours() + 1)
  return { startAt: toLocalInput(start), endAt: toLocalInput(end) }
}

export default function SchedulePage() {
  const { user } = useAuth()
  const [tab, setTab] = useState('calendar')
  const [scope, setScope] = useState('ME')
  // { mode: 'form' | 'detail', schedule }
  const [modal, setModal] = useState(null)
  const [reloadKey, setReloadKey] = useState(0)

  const reload = () => setReloadKey((k) => k + 1)
  const close = () => setModal(null)

  const handleSelectRange = (start, end, allDay) => {
    if (allDay) {
      // 월간 화면에서 날짜를 고르면 그 날 09:00 ~ 10:00 로 시작한다
      const s = new Date(start)
      s.setHours(9)
      const e = new Date(s)
      e.setHours(10)
      setModal({ mode: 'form', schedule: { startAt: toLocalInput(s), endAt: toLocalInput(e) } })
    } else {
      setModal({ mode: 'form', schedule: { startAt: toLocalInput(start), endAt: toLocalInput(end) } })
    }
  }

  return (
    <>
      <div className="page-header">
        <h2 className="page-title">일정</h2>
        <button
          type="button"
          className="btn btn-primary"
          onClick={() => setModal({ mode: 'form', schedule: defaultRange() })}
        >
          + 일정 등록
        </button>
      </div>

      <div className="toolbar">
        <div className="tabs" role="tablist">
          <button type="button" className={tab === 'calendar' ? 'active' : ''} onClick={() => setTab('calendar')}>
            캘린더
          </button>
          <button type="button" className={tab === 'list' ? 'active' : ''} onClick={() => setTab('list')}>
            목록
          </button>
        </div>

        {tab === 'calendar' && SCOPES[user.role].length > 1 && (
          <div className="tabs tabs-sm">
            {SCOPES[user.role].map((s) => (
              <button
                key={s.value}
                type="button"
                className={scope === s.value ? 'active' : ''}
                onClick={() => setScope(s.value)}
              >
                {s.label}
              </button>
            ))}
          </div>
        )}

        <div className="legend">
          {SCHEDULE_TYPES.map((t) => (
            <span key={t.value}>
              <i style={{ background: t.color }} />
              {t.label}
            </span>
          ))}
        </div>
      </div>

      {tab === 'calendar' ? (
        <ScheduleCalendar
          scope={scope}
          reloadKey={reloadKey}
          onSelectRange={handleSelectRange}
          onEventClick={(s) => setModal({ mode: 'detail', schedule: s })}
        />
      ) : (
        <ScheduleList reloadKey={reloadKey} onRowClick={(s) => setModal({ mode: 'detail', schedule: s })} />
      )}

      {modal?.mode === 'form' && (
        <ScheduleFormModal
          schedule={modal.schedule}
          onClose={close}
          onSaved={() => {
            close()
            reload()
          }}
        />
      )}
      {modal?.mode === 'detail' && (
        <ScheduleDetailModal
          schedule={modal.schedule}
          onClose={close}
          onEdit={() => setModal({ mode: 'form', schedule: modal.schedule })}
          onDeleted={() => {
            close()
            reload()
          }}
        />
      )}
    </>
  )
}
