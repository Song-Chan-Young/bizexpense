import client from './client'

export const APPROVAL_STATUSES = [
  { value: 'PENDING', label: '대기' },
  { value: 'APPROVED', label: '승인' },
  { value: 'REJECTED', label: '반려' },
  { value: 'CANCELLED', label: '취소' },
]

export const APPROVAL_TARGET_TYPES = [
  { value: 'TRIP', label: '출장' },
  { value: 'SETTLEMENT', label: '정산' },
]

export const approvalApi = {
  search: (params) => client.get('/approvals', { params }),
  approve: (id, comment) => client.post(`/approvals/${id}/approve`, { comment }),
  reject: (id, comment) => client.post(`/approvals/${id}/reject`, { comment }),
}
