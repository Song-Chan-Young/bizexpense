import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { settlementApi } from '../../api/settlements'
import ConfirmModal from '../../components/ConfirmModal'
import StatusBadge from '../../components/StatusBadge'
import { formatTimestamp } from '../../utils/date'
import { formatWon } from '../../utils/format'
import SettlementCalc from '../settlement/SettlementCalc'

/**
 * 출장 상세의 정산 영역: 지금 신청하면 받을 금액 미리보기 + 정산 신청 + 정산 이력.
 * reloadKey 가 바뀌면(경비 변경 등) 다시 조회하고, 정산을 신청하면 onChanged 로 알린다.
 */
export default function TripSettlementSection({ tripId, reloadKey, onChanged }) {
  const [data, setData] = useState(null)
  const [confirming, setConfirming] = useState(false)

  useEffect(() => {
    let ignore = false
    settlementApi
      .forTrip(tripId)
      .then((d) => !ignore && setData(d))
      .catch(() => {})
    return () => {
      ignore = true
    }
  }, [tripId, reloadKey])

  if (!data) return null
  const hasPreview = data.preview.expenseCount > 0

  return (
    <section className="panel">
      <div className="panel-header">
        <h3>정산</h3>
        {data.canRequest && (
          <button type="button" className="btn btn-primary" onClick={() => setConfirming(true)}>
            정산 신청
          </button>
        )}
      </div>

      {hasPreview ? (
        <>
          <p className="muted section-note">
            {data.canRequest ? '지금 신청하면 아래 금액으로 정산됩니다.' : '아직 정산에 포함되지 않은 경비입니다.'}
          </p>
          <SettlementCalc amounts={data.preview} />
        </>
      ) : null}
      {data.reason && <p className="muted section-note">{data.reason}</p>}

      {data.settlements.length > 0 && (
        <ul className="settle-history">
          {data.settlements.map((s) => (
            <li key={s.settlementId}>
              <Link to={`/settlements/${s.settlementId}`} className="settle-history-link">
                <span>
                  {formatTimestamp(s.requestedAt)} 신청 · {s.expenseCount}건
                </span>
                <strong>지급 {formatWon(s.payableAmount)}</strong>
                <StatusBadge status={s.status} label={s.statusLabel} />
              </Link>
            </li>
          ))}
        </ul>
      )}

      {confirming && (
        <ConfirmModal
          title="정산 신청"
          message={`경비 ${data.preview.expenseCount}건, 개인 지급액 ${formatWon(data.preview.payableAmount)}으로 팀장에게 정산을 신청할까요? 신청한 경비는 결재가 끝날 때까지 수정할 수 없습니다.`}
          confirmLabel="정산 신청"
          onConfirm={async () => {
            await settlementApi.request(tripId)
            setConfirming(false)
            onChanged()
          }}
          onClose={() => setConfirming(false)}
        />
      )}
    </section>
  )
}
