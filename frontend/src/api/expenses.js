import client from './client'

export const EXPENSE_STATUSES = [
  { value: 'DRAFT', label: '임시저장' },
  { value: 'REQUESTED', label: '정산신청' },
  { value: 'APPROVED', label: '승인' },
  { value: 'REJECTED', label: '반려' },
  { value: 'SETTLED', label: '정산완료' },
]

export const expenseApi = {
  // 응답: { page: PageResponse, totalAmount }
  search: (params) => client.get('/expenses', { params }),
  get: (id) => client.get(`/expenses/${id}`),
  create: (body) => client.post('/expenses', body),
  update: (id, body) => client.put(`/expenses/${id}`, body),
  remove: (id) => client.delete(`/expenses/${id}`),
  tripSummary: (tripId) => client.get(`/trips/${tripId}/expense-summary`),
  expensableTrips: () => client.get('/trips/expensable'),
}

// 영수증 첨부: 이미지(JPG, PNG, GIF, WEBP) 또는 PDF, 1개 5MB, 경비 1건에 5개까지
export const RECEIPT_ACCEPT = '.jpg,.jpeg,.png,.gif,.webp,.pdf'
export const RECEIPT_MAX_SIZE = 5 * 1024 * 1024
export const RECEIPT_MAX_COUNT = 5

export const receiptApi = {
  list: (expenseId) => client.get(`/expenses/${expenseId}/files`),
  upload: (expenseId, file) => {
    const form = new FormData()
    form.append('file', file)
    return client.post(`/expenses/${expenseId}/files`, form)
  },
  // 인증 헤더가 필요해 <img src> 로 바로 쓰지 못하므로 blob 으로 받아 object URL 을 만든다
  blob: (expenseId, fileId) => client.get(`/expenses/${expenseId}/files/${fileId}`, { responseType: 'blob' }),
  remove: (expenseId, fileId) => client.delete(`/expenses/${expenseId}/files/${fileId}`),
}
