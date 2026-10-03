import { useEffect, useState } from 'react'
import { useExpenseCodes } from '../../api/codes'
import { expenseApi } from '../../api/expenses'
import Modal from '../../components/Modal'
import { formatDate, toDateParam } from '../../utils/date'

/**
 * 경비 등록/수정.
 * expense.expenseId 가 있으면 수정. 출장 상세에서 열면 expense.tripId 가 미리 채워진다.
 */
export default function ExpenseFormModal({ expense, onClose, onSaved }) {
  const isEdit = Boolean(expense.expenseId)
  const { categories, paymentMethods } = useExpenseCodes()
  const [trips, setTrips] = useState([])
  const [form, setForm] = useState(() => ({
    tripId: expense.tripId ? String(expense.tripId) : '',
    usedAt: expense.usedAt ?? toDateParam(new Date()),
    categoryId: expense.categoryId ? String(expense.categoryId) : '',
    paymentMethodId: expense.paymentMethodId ? String(expense.paymentMethodId) : '',
    storeName: expense.storeName ?? '',
    amount: expense.amount ? String(expense.amount) : '',
    description: expense.description ?? '',
  }))
  const [error, setError] = useState('')
  const [saving, setSaving] = useState(false)

  useEffect(() => {
    let ignore = false
    expenseApi
      .expensableTrips()
      .then((list) => {
        if (ignore) return
        setTrips(list)
        // 출장이 하나뿐이면 바로 선택
        if (!expense.tripId && list.length === 1) setForm((f) => ({ ...f, tripId: String(list[0].tripId) }))
      })
      .catch(() => {})
    return () => {
      ignore = true
    }
  }, [expense.tripId])

  const set = (key) => (e) => setForm((f) => ({ ...f, [key]: e.target.value }))
  const selectedTrip = trips.find((t) => String(t.tripId) === form.tripId)

  const handleSubmit = async (e) => {
    e.preventDefault()
    setError('')
    setSaving(true)
    const body = {
      ...form,
      tripId: Number(form.tripId),
      categoryId: Number(form.categoryId),
      paymentMethodId: Number(form.paymentMethodId),
      amount: Number(form.amount),
    }
    try {
      const saved = isEdit ? await expenseApi.update(expense.expenseId, body) : await expenseApi.create(body)
      onSaved(saved)
    } catch (err) {
      setError(err.message)
      setSaving(false)
    }
  }

  const noTrips = trips.length === 0 && !expense.tripId

  return (
    <Modal
      title={isEdit ? '경비 수정' : '경비 등록'}
      onClose={onClose}
      footer={
        <>
          <button type="button" className="btn btn-ghost" onClick={onClose}>
            취소
          </button>
          <button type="submit" form="expense-form" className="btn btn-primary" disabled={saving || noTrips}>
            {saving ? '저장 중…' : '저장'}
          </button>
        </>
      }
    >
      {noTrips && (
        <div className="alert alert-warning no-margin-top">
          경비를 등록할 수 있는 출장이 없습니다. 출장이 승인된 뒤 <strong>출장 시작</strong>을 하면 경비를 등록할 수 있습니다.
        </div>
      )}
      <form id="expense-form" className="form-grid" onSubmit={handleSubmit}>
        <label className="span-2">
          출장
          <select value={form.tripId} onChange={set('tripId')} required>
            <option value="">출장 선택</option>
            {trips.map((t) => (
              <option key={t.tripId} value={t.tripId}>
                {t.title} ({formatDate(t.startDate)} ~ {formatDate(t.endDate)}) · {t.statusLabel}
              </option>
            ))}
            {isEdit && !trips.some((t) => t.tripId === expense.tripId) && (
              <option value={expense.tripId}>{expense.tripTitle}</option>
            )}
          </select>
        </label>
        <label>
          사용일
          <input type="date" value={form.usedAt} onChange={set('usedAt')} max={selectedTrip?.endDate} required />
        </label>
        <label>
          금액 (원)
          <input type="number" min={1} value={form.amount} onChange={set('amount')} required />
        </label>
        <label>
          비용 항목
          <select value={form.categoryId} onChange={set('categoryId')} required>
            <option value="">선택</option>
            {categories.map((c) => (
              <option key={c.categoryId} value={c.categoryId}>
                {c.name}
              </option>
            ))}
          </select>
        </label>
        <label>
          결제 수단
          <select value={form.paymentMethodId} onChange={set('paymentMethodId')} required>
            <option value="">선택</option>
            {paymentMethods.map((p) => (
              <option key={p.paymentMethodId} value={p.paymentMethodId}>
                {p.name}
                {p.corporate ? ' (법인)' : ''}
              </option>
            ))}
          </select>
        </label>
        <label className="span-2">
          사용처
          <input value={form.storeName} onChange={set('storeName')} maxLength={100} required placeholder="상호명" />
        </label>
        <label className="span-2">
          내용
          <textarea value={form.description} onChange={set('description')} rows={3} maxLength={1000} />
        </label>
      </form>
      {error && <p className="form-error">{error}</p>}
    </Modal>
  )
}
