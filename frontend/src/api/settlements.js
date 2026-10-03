import client from './client'

export const SETTLEMENT_STATUSES = [
  { value: 'REQUESTED', label: '정산신청' },
  { value: 'APPROVED', label: '승인' },
  { value: 'REJECTED', label: '반려' },
  { value: 'SETTLED', label: '정산완료' },
]

export const settlementApi = {
  search: (params) => client.get('/settlements', { params }),
  get: (id) => client.get(`/settlements/${id}`),
  // 출장 상세의 정산 영역: { canRequest, reason, preview, settlements }
  forTrip: (tripId) => client.get(`/trips/${tripId}/settlement`),
  request: (tripId) => client.post(`/trips/${tripId}/settlement`),
  resubmit: (id) => client.post(`/settlements/${id}/request`),
  complete: (id) => client.post(`/settlements/${id}/complete`),
}
