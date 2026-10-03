import { useState } from 'react'
import { scheduleApi, typeColor } from '../../api/schedules'
import Modal from '../../components/Modal'
import { formatRange } from '../../utils/date'

export default function ScheduleDetailModal({ schedule, onClose, onEdit, onDeleted }) {
  const [confirming, setConfirming] = useState(false)
  const [error, setError] = useState('')

  const handleDelete = async () => {
    try {
      await scheduleApi.remove(schedule.scheduleId)
      onDeleted()
    } catch (err) {
      setError(err.message)
    }
  }

  let footer = null
  if (schedule.editable) {
    footer = confirming ? (
      <>
        <span className="confirm-text">이 일정을 삭제할까요?</span>
        <button type="button" className="btn btn-ghost" onClick={() => setConfirming(false)}>
          아니오
        </button>
        <button type="button" className="btn btn-danger" onClick={handleDelete}>
          삭제
        </button>
      </>
    ) : (
      <>
        <button type="button" className="btn btn-ghost" onClick={() => setConfirming(true)}>
          삭제
        </button>
        <button type="button" className="btn btn-primary" onClick={onEdit}>
          수정
        </button>
      </>
    )
  }

  return (
    <Modal title="일정 상세" onClose={onClose} footer={footer}>
      <div className="detail-head">
        <span className="type-badge" style={{ background: typeColor(schedule.type) }}>
          {schedule.typeLabel}
        </span>
        <span className={`status-badge status-${schedule.status.toLowerCase()}`}>{schedule.statusLabel}</span>
      </div>
      <h4 className="detail-title">{schedule.title}</h4>
      <dl className="detail-list">
        <dt>일시</dt>
        <dd>{formatRange(schedule.startAt, schedule.endAt)}</dd>
        <dt>장소</dt>
        <dd>{schedule.location || '-'}</dd>
        <dt>등록자</dt>
        <dd>{schedule.userName}</dd>
        <dt>내용</dt>
        <dd className="pre">{schedule.content || '-'}</dd>
      </dl>
      {!schedule.editable && <p className="muted small">다른 직원의 일정은 조회만 할 수 있습니다.</p>}
      {error && <p className="form-error">{error}</p>}
    </Modal>
  )
}
