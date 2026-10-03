import { useCallback, useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { typeColor } from '../../api/schedules'
import { tripApi } from '../../api/trips'
import ConfirmModal from '../../components/ConfirmModal'
import StatusBadge from '../../components/StatusBadge'
import { formatDate, formatRange, formatTimestamp } from '../../utils/date'
import { formatWon } from '../../utils/format'
import ScheduleDetailModal from '../schedule/ScheduleDetailModal'
import ScheduleFormModal from '../schedule/ScheduleFormModal'

// 상태 변경 버튼: actions 의 키 → [버튼 문구, 확인 메시지, API action, 위험 여부]
const ACTIONS = {
  request: ['결재 신청', '팀장에게 결재를 신청할까요? 신청 후에는 수정할 수 없습니다.', 'request', false],
  start: ['출장 시작', '출장을 시작 상태로 바꿀까요?', 'start', false],
  complete: ['출장 완료', '출장을 완료 처리할까요?', 'complete', false],
  cancel: ['출장 취소', '출장을 취소할까요? 연결된 일정도 함께 취소됩니다.', 'cancel', true],
}

export default function TripDetailPage() {
  const { id } = useParams()
  const navigate = useNavigate()
  const [detail, setDetail] = useState(null)
  const [error, setError] = useState('')
  // { kind: 'action', key } | { kind: 'delete' } | { kind: 'scheduleForm', schedule } | { kind: 'scheduleDetail', schedule }
  const [modal, setModal] = useState(null)

  const load = useCallback(() => {
    let ignore = false
    tripApi
      .get(id)
      .then((data) => !ignore && setDetail(data))
      .catch((err) => !ignore && setError(err.message))
    return () => {
      ignore = true
    }
  }, [id])

  useEffect(load, [load])

  if (error) return <p className="form-error">{error}</p>
  if (!detail) return null

  const { trip, schedules, approvals } = detail
  const close = () => setModal(null)
  const reloadAndClose = () => {
    close()
    load()
  }
  const lastRejected = trip.status === 'REJECTED' ? approvals.find((a) => a.status === 'REJECTED') : null

  // 출장 일정 추가: 첫날 09:00~10:00 으로 시작
  const newSchedule = {
    type: 'CLIENT_VISIT',
    tripId: trip.tripId,
    startAt: `${trip.startDate}T09:00`,
    endAt: `${trip.startDate}T10:00`,
  }

  return (
    <>
      <div className="page-header">
        <div>
          <Link to="/trips" className="back-link">
            ← 출장 목록
          </Link>
          <h2 className="page-title">
            {trip.title} <StatusBadge status={trip.status} label={trip.statusLabel} />
          </h2>
        </div>
        <div className="button-row">
          {trip.actions.delete && (
            <button type="button" className="btn btn-ghost" onClick={() => setModal({ kind: 'delete' })}>
              삭제
            </button>
          )}
          {trip.actions.edit && (
            <Link to={`/trips/${trip.tripId}/edit`} className="btn btn-ghost">
              수정
            </Link>
          )}
          {Object.entries(ACTIONS)
            .filter(([key]) => trip.actions[key])
            .map(([key, [label, , , danger]]) => (
              <button
                key={key}
                type="button"
                className={`btn ${danger ? 'btn-ghost danger-text' : 'btn-primary'}`}
                onClick={() => setModal({ kind: 'action', key })}
              >
                {label}
              </button>
            ))}
        </div>
      </div>

      {lastRejected && (
        <div className="alert alert-danger">
          <strong>반려되었습니다 · {lastRejected.approverName}</strong>
          <p>{lastRejected.comment}</p>
          내용을 수정한 뒤 다시 신청하세요.
        </div>
      )}

      <section className="panel">
        <dl className="info-grid">
          <dt>신청자</dt>
          <dd>
            {trip.userName}
            {trip.departmentName && <span className="muted"> · {trip.departmentName}</span>}
          </dd>
          <dt>출장지</dt>
          <dd>{trip.destination}</dd>
          <dt>기간</dt>
          <dd>
            {formatDate(trip.startDate)} ~ {formatDate(trip.endDate)} <span className="muted">({trip.days}일)</span>
          </dd>
          <dt>예상 경비</dt>
          <dd>{formatWon(trip.expectedAmount)}</dd>
          <dt>출장 목적</dt>
          <dd className="pre">{trip.purpose || '-'}</dd>
        </dl>
      </section>

      <section className="panel">
        <div className="panel-header">
          <h3>출장 일정</h3>
          {trip.actions.addSchedule && (
            <button
              type="button"
              className="btn btn-ghost"
              onClick={() => setModal({ kind: 'scheduleForm', schedule: newSchedule })}
            >
              + 일정 추가
            </button>
          )}
        </div>
        {schedules.length === 0 ? (
          <p className="muted empty-inline">연결된 일정이 없습니다.</p>
        ) : (
          <ul className="timeline">
            {schedules.map((s) => (
              <li
                key={s.scheduleId}
                className={s.status === 'CANCELLED' ? 'cancelled' : ''}
                onClick={() => setModal({ kind: 'scheduleDetail', schedule: s })}
              >
                <i style={{ background: typeColor(s.type) }} />
                <span className="timeline-time">{formatRange(s.startAt, s.endAt)}</span>
                <span className="timeline-title">{s.title}</span>
                {s.location && <span className="muted">{s.location}</span>}
                <StatusBadge status={s.status} label={s.statusLabel} />
              </li>
            ))}
          </ul>
        )}
      </section>

      <section className="panel">
        <div className="panel-header">
          <h3>결재 이력</h3>
        </div>
        {approvals.length === 0 ? (
          <p className="muted empty-inline">아직 신청하지 않았습니다.</p>
        ) : (
          <table className="table compact">
            <thead>
              <tr>
                <th>신청일시</th>
                <th>결재자</th>
                <th>결과</th>
                <th>처리일시</th>
                <th>의견</th>
              </tr>
            </thead>
            <tbody>
              {approvals.map((a) => (
                <tr key={a.approvalId}>
                  <td className="nowrap">{formatTimestamp(a.requestedAt)}</td>
                  <td>{a.approverName}</td>
                  <td>
                    <StatusBadge status={a.status} label={a.statusLabel} />
                  </td>
                  <td className="nowrap">{formatTimestamp(a.processedAt)}</td>
                  <td>{a.comment || '-'}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </section>

      {modal?.kind === 'action' && (
        <ConfirmModal
          title={ACTIONS[modal.key][0]}
          message={ACTIONS[modal.key][1]}
          confirmLabel={ACTIONS[modal.key][0]}
          danger={ACTIONS[modal.key][3]}
          onConfirm={async () => {
            await tripApi.action(trip.tripId, ACTIONS[modal.key][2])
            reloadAndClose()
          }}
          onClose={close}
        />
      )}
      {modal?.kind === 'delete' && (
        <ConfirmModal
          title="출장 삭제"
          message="임시저장한 출장을 삭제할까요? 연결된 일정은 남고 연결만 해제됩니다."
          confirmLabel="삭제"
          danger
          onConfirm={async () => {
            await tripApi.remove(trip.tripId)
            navigate('/trips', { replace: true })
          }}
          onClose={close}
        />
      )}
      {modal?.kind === 'scheduleForm' && (
        <ScheduleFormModal schedule={modal.schedule} onClose={close} onSaved={reloadAndClose} />
      )}
      {modal?.kind === 'scheduleDetail' && (
        <ScheduleDetailModal
          schedule={modal.schedule}
          onClose={close}
          onEdit={() => setModal({ kind: 'scheduleForm', schedule: modal.schedule })}
          onDeleted={reloadAndClose}
        />
      )}
    </>
  )
}
