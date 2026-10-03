import client from './client'

export const TRIP_STATUSES = [
  { value: 'DRAFT', label: '임시저장' },
  { value: 'REQUESTED', label: '신청' },
  { value: 'APPROVED', label: '승인' },
  { value: 'IN_PROGRESS', label: '진행중' },
  { value: 'COMPLETED', label: '완료' },
  { value: 'CANCELLED', label: '취소' },
  { value: 'REJECTED', label: '반려' },
]

export const tripApi = {
  search: (params) => client.get('/trips', { params }),
  schedulable: () => client.get('/trips/schedulable'),
  get: (id) => client.get(`/trips/${id}`),
  create: (body) => client.post('/trips', body),
  update: (id, body) => client.put(`/trips/${id}`, body),
  remove: (id) => client.delete(`/trips/${id}`),
  // action: request | cancel | start | complete
  action: (id, action) => client.post(`/trips/${id}/${action}`),
}
