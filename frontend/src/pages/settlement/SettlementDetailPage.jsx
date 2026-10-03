import { useCallback, useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { settlementApi } from '../../api/settlements'
import ConfirmModal from '../../components/ConfirmModal'
import StatusBadge from '../../components/StatusBadge'
import { formatDate, formatTimestamp } from '../../utils/date'
import { formatWon } from '../../utils/format'
import ExpenseDetailModal from '../expense/ExpenseDetailModal'
import ExpenseTable from '../expense/ExpenseTable'
import SettlementCalc from './SettlementCalc'

export default function SettlementDetailPage() {
  const { id } = useParams()
  const [detail, setDetail] = useState(null)
  const [error, setError] = useState('')
  // { kind: 'resubmit' | 'complete' } | { kind: 'expense', expense }
  const [modal, setModal] = useState(null)

  const load = useCallback(() => {
    let ignore = false
    settlementApi
      .get(id)
      .then((d) => !ignore && setDetail(d))
      .catch((err) => !ignore && setError(err.message))
    return () => {
      ignore = true
    }
  }, [id])

  useEffect(load, [load])

  if (error) return <p className="form-error">{error}</p>
  if (!detail) return null

  const { settlement: s, expenses, approvals } = detail
  const lastRejected = s.status === 'REJECTED' ? approvals.find((a) => a.status === 'REJECTED') : null
  const close = () => setModal(null)

  return (
    <>
      <div className="page-header">
        <div>
          <Link to="/settlements" className="back-link">
            ← 정산 목록
          </Link>
          <h2 className="page-title">
            {s.tripTitle} 정산 <StatusBadge status={s.status} label={s.statusLabel} />
          </h2>
        </div>
        <div className="button-row">
          <Link to={`/trips/${s.tripId}`} className="btn btn-ghost">
            출장 보기
          </Link>
          {s.actions.resubmit && (
            <button type="button" className="btn btn-primary" onClick={() => setModal({ kind: 'resubmit' })}>
              재신청
            </button>
          )}
          {s.actions.complete && (
            <button type="button" className="btn btn-primary" onClick={() => setModal({ kind: 'complete' })}>
              지급 완료 처리
            </button>
          )}
        </div>
      </div>

      {lastRejected && (
        <div className="alert alert-danger">
          <strong>반려되었습니다 · {lastRejected.approverName}</strong>
          <p>{lastRejected.comment}</p>
          <Link to={`/trips/${s.tripId}`} className="link">
            출장 화면
          </Link>
          에서 경비를 수정·삭제·추가한 뒤 <strong>재신청</strong>하세요. 금액은 재신청할 때 다시 계산됩니다.
        </div>
      )}

      <div className="settle-top">
        <section className="panel">
          <div className="panel-header">
            <h3>정산 금액</h3>
          </div>
          <SettlementCalc amounts={s} />
        </section>
        <section className="panel">
          <dl className="info-grid">
            <dt>신청자</dt>
            <dd>
              {s.userName}
              {s.departmentName && <span className="muted"> · {s.departmentName}</span>}
            </dd>
            <dt>출장 기간</dt>
            <dd>
              {formatDate(s.tripStartDate)} ~ {formatDate(s.tripEndDate)}
            </dd>
            <dt>신청</dt>
            <dd>{formatTimestamp(s.requestedAt)}</dd>
            <dt>승인</dt>
            <dd>{formatTimestamp(s.approvedAt)}</dd>
            <dt>지급 완료</dt>
            <dd>{formatTimestamp(s.settledAt)}</dd>
          </dl>
        </section>
      </div>

      <section className="panel">
        <div className="panel-header">
          <h3>포함된 경비</h3>
        </div>
        <ExpenseTable expenses={expenses} onRowClick={(e) => setModal({ kind: 'expense', expense: e })} />
      </section>

      <section className="panel">
        <div className="panel-header">
          <h3>결재 이력</h3>
        </div>
        <div className="table-wrap">
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
                  <td className="wrap">{a.comment || '-'}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>

      {modal?.kind === 'resubmit' && (
        <ConfirmModal
          title="정산 재신청"
          message="수정한 경비로 금액을 다시 계산해 팀장에게 재신청할까요?"
          confirmLabel="재신청"
          onConfirm={async () => {
            await settlementApi.resubmit(s.settlementId)
            close()
            load()
          }}
          onClose={close}
        />
      )}
      {modal?.kind === 'complete' && (
        <ConfirmModal
          title="지급 완료 처리"
          message={`${s.userName}님에게 ${formatWon(s.payableAmount)} 지급을 완료했나요? 처리하면 포함된 경비가 모두 '정산완료'로 바뀝니다.`}
          confirmLabel="지급 완료"
          onConfirm={async () => {
            await settlementApi.complete(s.settlementId)
            close()
            load()
          }}
          onClose={close}
        />
      )}
      {modal?.kind === 'expense' && (
        <ExpenseDetailModal expense={modal.expense} onClose={close} onEdit={close} onDeleted={close} />
      )}
    </>
  )
}
