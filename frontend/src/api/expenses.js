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
