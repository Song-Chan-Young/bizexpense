import { useEffect, useState } from 'react'
import { SCHEDULE_STATUSES, SCHEDULE_TYPES, scheduleApi } from '../../api/schedules'
import { tripApi } from '../../api/trips'
import Modal from '../../components/Modal'
import { formatDate, formatRange } from '../../utils/date'

/**
 * 일정 등록/수정.
 * schedule 이 scheduleId 를 가지면 수정, 아니면 등록(startAt/endAt 만 채워진 초기값).
 * 같은 시간대 일정이 있으면 서버가 409 로 경고하고, 사용자가 확인하면 allowOverlap 으로 다시 저장한다.
 */
export default function ScheduleFormModal({ schedule, onClose, onSaved }) {
  const isEdit = Boolean(schedule.scheduleId)
  const [form, setForm] = useState({
    type: schedule.type ?? 'MEETING',
    title: schedule.title ?? '',
    startAt: schedule.startAt?.slice(0, 16) ?? '',
    endAt: schedule.endAt?.slice(0, 16) ?? '',
    location: schedule.location ?? '',
    content: schedule.content ?? '',
    status: schedule.status ?? 'PLANNED',
    tripId: schedule.tripId ? String(schedule.tripId) : '',
  })
  const [trips, setTrips] = useState([])
  const [error, setError] = useState('')
  const [conflicts, setConflicts] = useState(null)
  const [saving, setSaving] = useState(false)

  // 연결할 수 있는 내 출장 목록. 이미 연결된 출장이 목록에 없으면(완료 등) 현재 값으로 보여준다.
  useEffect(() => {
    tripApi
      .schedulable()
      .then((list) => {
        if (schedule.tripId && !list.some((t) => t.tripId === schedule.tripId)) {
          list = [{ tripId: schedule.tripId, title: schedule.tripTitle }, ...list]
        }
        setTrips(list)
      })
      .catch(() => setTrips([]))
  }, [schedule.tripId, schedule.tripTitle])

  const set = (key) => (e) => {
    setForm((f) => ({ ...f, [key]: e.target.value }))
    // 시간을 바꾸면 이전 충돌 경고는 더 이상 맞지 않는다
    if (key === 'startAt' || key === 'endAt') setConflicts(null)
  }

  const save = async (allowOverlap) => {
    setError('')
    setSaving(true)
    const body = { ...form, tripId: form.tripId ? Number(form.tripId) : null, allowOverlap }
    try {
      const saved = isEdit
        ? await scheduleApi.update(schedule.scheduleId, body)
        : await scheduleApi.create(body)
      onSaved(saved)
    } catch (err) {
      if (err.code === 'SCHEDULE_CONFLICT') {
        setConflicts(await scheduleApi.conflicts(form.startAt, form.endAt, schedule.scheduleId))
      } else {
        setError(err.message)
      }
    } finally {
      setSaving(false)
    }
  }

  const handleSubmit = (e) => {
    e.preventDefault()
    save(false)
  }

  return (
    <Modal
      title={isEdit ? '일정 수정' : '일정 등록'}
      onClose={onClose}
      footer={
        conflicts ? (
          <>
            <button type="button" className="btn btn-ghost" onClick={() => setConflicts(null)}>
              시간 다시 정하기
            </button>
            <button type="button" className="btn btn-warning" disabled={saving} onClick={() => save(true)}>
              겹쳐도 저장
            </button>
          </>
        ) : (
          <>
            <button type="button" className="btn btn-ghost" onClick={onClose}>
              취소
            </button>
            <button type="submit" form="schedule-form" className="btn btn-primary" disabled={saving}>
              {saving ? '저장 중…' : '저장'}
            </button>
          </>
        )
      }
    >
      <form id="schedule-form" className="form-grid" onSubmit={handleSubmit}>
        <label>
          종류
          <select value={form.type} onChange={set('type')}>
            {SCHEDULE_TYPES.map((t) => (
              <option key={t.value} value={t.value}>
                {t.label}
              </option>
            ))}
          </select>
        </label>
        {isEdit ? (
          <label>
            상태
            <select value={form.status} onChange={set('status')}>
              {SCHEDULE_STATUSES.map((s) => (
                <option key={s.value} value={s.value}>
                  {s.label}
                </option>
              ))}
            </select>
          </label>
        ) : (
          <span />
        )}

        <label className="span-2">
          제목
          <input value={form.title} onChange={set('title')} maxLength={200} required autoFocus />
        </label>
        <label>
          시작
          <input type="datetime-local" value={form.startAt} onChange={set('startAt')} required />
        </label>
        <label>
          종료
          <input type="datetime-local" value={form.endAt} onChange={set('endAt')} min={form.startAt} required />
        </label>
        <label className="span-2">
          출장 연결 <span className="muted">(선택 · 출장 기간 안의 일정만)</span>
          <select value={form.tripId} onChange={set('tripId')}>
            <option value="">연결 안 함</option>
            {trips.map((t) => (
              <option key={t.tripId} value={t.tripId}>
                {t.title}
                {t.startDate && ` (${formatDate(t.startDate)} ~ ${formatDate(t.endDate)})`}
              </option>
            ))}
          </select>
        </label>
        <label className="span-2">
          장소
          <input value={form.location} onChange={set('location')} maxLength={200} />
        </label>
        <label className="span-2">
          내용
          <textarea value={form.content} onChange={set('content')} rows={4} maxLength={2000} />
        </label>
      </form>

      {error && <p className="form-error">{error}</p>}

      {conflicts && (
        <div className="alert alert-warning">
          <strong>같은 시간대에 다른 일정이 있습니다.</strong>
          <ul>
            {conflicts.map((c) => (
              <li key={c.scheduleId}>
                {c.title} · {formatRange(c.startAt, c.endAt)}
              </li>
            ))}
          </ul>
          그래도 저장하시겠어요?
        </div>
      )}
    </Modal>
  )
}
