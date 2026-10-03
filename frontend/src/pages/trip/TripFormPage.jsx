import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { tripApi } from '../../api/trips'

const EMPTY = { title: '', purpose: '', destination: '', startDate: '', endDate: '', expectedAmount: '' }

/** 출장 등록(/trips/new) · 수정(/trips/:id/edit). 수정은 임시저장/반려 상태에서만 가능하다. */
export default function TripFormPage() {
  const { id } = useParams()
  const isEdit = Boolean(id)
  const navigate = useNavigate()
  const [form, setForm] = useState(EMPTY)
  // 수정 화면은 기존 값을 불러온 뒤에 폼을 보여준다 (불러오기 전 입력이 덮어써지지 않도록)
  const [loaded, setLoaded] = useState(!isEdit)
  const [rejectComment, setRejectComment] = useState(null)
  const [error, setError] = useState('')
  const [saving, setSaving] = useState(false)

  useEffect(() => {
    if (!isEdit) return
    let ignore = false
    tripApi
      .get(id)
      .then(({ trip, approvals }) => {
        if (ignore) return
        if (!trip.actions.edit) {
          navigate(`/trips/${id}`, { replace: true })
          return
        }
        setForm({
          title: trip.title,
          purpose: trip.purpose ?? '',
          destination: trip.destination,
          startDate: trip.startDate,
          endDate: trip.endDate,
          expectedAmount: String(trip.expectedAmount),
        })
        // 반려 건이면 수정하면서 반려 사유를 볼 수 있게 한다
        if (trip.status === 'REJECTED') setRejectComment(approvals.find((a) => a.status === 'REJECTED')?.comment)
        setLoaded(true)
      })
      .catch((err) => !ignore && setError(err.message))
    return () => {
      ignore = true
    }
  }, [id, isEdit, navigate])

  const set = (key) => (e) => setForm((f) => ({ ...f, [key]: e.target.value }))

  const save = async (submit) => {
    setError('')
    setSaving(true)
    const body = { ...form, expectedAmount: Number(form.expectedAmount || 0) }
    try {
      let tripId = id
      if (isEdit) {
        await tripApi.update(id, body)
        if (submit) await tripApi.action(id, 'request')
      } else {
        tripId = (await tripApi.create({ ...body, submit })).tripId
      }
      navigate(`/trips/${tripId}`, { replace: true })
    } catch (err) {
      setError(err.message)
      setSaving(false)
    }
  }

  if (!loaded) return error ? <p className="form-error">{error}</p> : null

  return (
    <>
      <div className="page-header">
        <h2 className="page-title">{isEdit ? '출장 수정' : '출장 등록'}</h2>
      </div>

      {rejectComment && (
        <div className="alert alert-danger">
          <strong>반려 사유</strong>
          <p>{rejectComment}</p>
        </div>
      )}

      <form
        className="panel form-grid"
        onSubmit={(e) => {
          e.preventDefault()
          save(true)
        }}
      >
        <label className="span-2">
          출장명
          <input value={form.title} onChange={set('title')} maxLength={200} required placeholder="부산 거래처 방문" />
        </label>
        <label>
          출장지
          <input value={form.destination} onChange={set('destination')} maxLength={200} required />
        </label>
        <label>
          예상 경비 (원)
          <input
            type="number"
            min={0}
            step={1000}
            value={form.expectedAmount}
            onChange={set('expectedAmount')}
            required
          />
        </label>
        <label>
          시작일
          <input type="date" value={form.startDate} onChange={set('startDate')} required />
        </label>
        <label>
          종료일
          <input type="date" value={form.endDate} onChange={set('endDate')} min={form.startDate} required />
        </label>
        <label className="span-2">
          출장 목적
          <textarea value={form.purpose} onChange={set('purpose')} rows={4} maxLength={1000} />
        </label>

        {error && <p className="form-error span-2">{error}</p>}

        <div className="form-actions span-2">
          <Link to={isEdit ? `/trips/${id}` : '/trips'} className="btn btn-ghost">
            취소
          </Link>
          <button type="button" className="btn btn-ghost" disabled={saving} onClick={() => save(false)}>
            {isEdit ? '저장' : '임시저장'}
          </button>
          <button type="submit" className="btn btn-primary" disabled={saving}>
            {isEdit ? '저장 후 신청' : '신청'}
          </button>
        </div>
      </form>
    </>
  )
}
