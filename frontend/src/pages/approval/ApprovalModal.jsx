import { useState } from 'react'
import { Link } from 'react-router-dom'
import { approvalApi } from '../../api/approvals'
import Modal from '../../components/Modal'
import StatusBadge from '../../components/StatusBadge'
import { formatTimestamp } from '../../utils/date'

// 결재 대상 상세 화면 경로 (정산은 Phase 8 에서 추가)
const TARGET_PATH = {
  TRIP: (id) => `/trips/${id}`,
}

/** 결재 상세 + 승인/반려. 반려 시 사유 필수. */
export default function ApprovalModal({ approval, onClose, onProcessed }) {
  const [comment, setComment] = useState('')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  const process = async (kind) => {
    if (kind === 'reject' && !comment.trim()) {
      setError('반려 사유를 입력하세요.')
      return
    }
    setBusy(true)
    setError('')
    try {
      await approvalApi[kind](approval.approvalId, comment.trim() || null)
      onProcessed()
    } catch (err) {
      setError(err.message)
      setBusy(false)
    }
  }

  const targetPath = TARGET_PATH[approval.targetType]?.(approval.targetId)

  return (
    <Modal
      title="결재"
      onClose={onClose}
      footer={
        approval.processable ? (
          <>
            <button type="button" className="btn btn-ghost danger-text" disabled={busy} onClick={() => process('reject')}>
              반려
            </button>
            <button type="button" className="btn btn-primary" disabled={busy} onClick={() => process('approve')}>
              승인
            </button>
          </>
        ) : null
      }
    >
      <div className="detail-head">
        <span className="status-badge">{approval.targetTypeLabel}</span>
        <StatusBadge status={approval.status} label={approval.statusLabel} />
      </div>
      <h4 className="detail-title">{approval.title}</h4>
      <dl className="detail-list">
        <dt>신청자</dt>
        <dd>
          {approval.requesterName}
          {approval.requesterDepartmentName && <span className="muted"> · {approval.requesterDepartmentName}</span>}
        </dd>
        <dt>신청일시</dt>
        <dd>{formatTimestamp(approval.requestedAt)}</dd>
        <dt>결재자</dt>
        <dd>{approval.approverName}</dd>
        {approval.processedAt && (
          <>
            <dt>처리일시</dt>
            <dd>{formatTimestamp(approval.processedAt)}</dd>
            <dt>의견</dt>
            <dd className="pre">{approval.comment || '-'}</dd>
          </>
        )}
      </dl>

      {targetPath && (
        <p>
          <Link to={targetPath} className="link">
            {approval.targetTypeLabel} 상세 보기 →
          </Link>
        </p>
      )}

      {approval.processable && (
        <label className="field">
          결재 의견 <span className="muted">(반려 시 필수)</span>
          <textarea value={comment} onChange={(e) => setComment(e.target.value)} rows={3} maxLength={1000} />
        </label>
      )}
      {error && <p className="form-error">{error}</p>}
    </Modal>
  )
}
