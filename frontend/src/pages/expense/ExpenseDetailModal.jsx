import { useState } from 'react'
import { Link } from 'react-router-dom'
import { expenseApi } from '../../api/expenses'
import Modal from '../../components/Modal'
import StatusBadge from '../../components/StatusBadge'
import { formatDate } from '../../utils/date'
import { formatWon } from '../../utils/format'
import ReceiptSection from './ReceiptSection'

/** onChanged: 영수증 첨부/삭제로 경비(증빙 여부)가 바뀌었을 때 목록을 다시 불러오도록 알린다 */
export default function ExpenseDetailModal({ expense, onClose, onEdit, onDeleted, onChanged }) {
  const [confirming, setConfirming] = useState(false)
  const [error, setError] = useState('')

  const handleDelete = async () => {
    try {
      await expenseApi.remove(expense.expenseId)
      onDeleted()
    } catch (err) {
      setError(err.message)
    }
  }

  let footer = null
  if (expense.editable) {
    footer = confirming ? (
      <>
        <span className="confirm-text">이 경비를 삭제할까요?</span>
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
    <Modal title="경비 상세" onClose={onClose} footer={footer}>
      <div className="detail-head">
        <span className="status-badge">{expense.categoryName}</span>
        <StatusBadge status={expense.status} label={expense.statusLabel} />
      </div>
      <h4 className="detail-title">
        {expense.storeName} <span className="amount">{formatWon(expense.amount)}</span>
      </h4>
      <dl className="detail-list">
        <dt>사용일</dt>
        <dd>{formatDate(expense.usedAt)}</dd>
        <dt>결제 수단</dt>
        <dd>
          {expense.paymentMethodName}
          {expense.corporate && <span className="tag">법인</span>}
        </dd>
        <dt>출장</dt>
        <dd>
          <Link to={`/trips/${expense.tripId}`} className="link" onClick={onClose}>
            {expense.tripTitle}
          </Link>
        </dd>
        <dt>등록자</dt>
        <dd>{expense.userName}</dd>
        <dt>내용</dt>
        <dd className="pre">{expense.description || '-'}</dd>
      </dl>
      <ReceiptSection expenseId={expense.expenseId} editable={expense.editable} onChanged={onChanged} />
      {error && <p className="form-error">{error}</p>}
    </Modal>
  )
}
