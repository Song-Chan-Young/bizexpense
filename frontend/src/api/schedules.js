import client from './client'

export const SCHEDULE_TYPES = [
  { value: 'TRIP', label: '출장', color: '#7c3aed' },
  { value: 'MEETING', label: '회의', color: '#2563eb' },
  { value: 'OUTSIDE_WORK', label: '외근', color: '#0891b2' },
  { value: 'CLIENT_VISIT', label: '고객 방문', color: '#059669' },
  { value: 'TRAINING', label: '교육', color: '#d97706' },
  { value: 'PERSONAL', label: '개인 업무', color: '#64748b' },
  { value: 'ETC', label: '기타', color: '#9ca3af' },
]

export const SCHEDULE_STATUSES = [
  { value: 'PLANNED', label: '예정' },
  { value: 'ONGOING', label: '진행중' },
  { value: 'DONE', label: '완료' },
  { value: 'CANCELLED', label: '취소' },
]

export const typeColor = (type) => SCHEDULE_TYPES.find((t) => t.value === type)?.color ?? '#9ca3af'

export const scheduleApi = {
  search: (params) => client.get('/schedules', { params }),
  calendar: (from, to, scope) => client.get('/schedules/calendar', { params: { from, to, scope } }),
  conflicts: (startAt, endAt, excludeId) =>
    client.get('/schedules/conflicts', { params: { startAt, endAt, excludeId } }),
  get: (id) => client.get(`/schedules/${id}`),
  create: (body) => client.post('/schedules', body),
  update: (id, body) => client.put(`/schedules/${id}`, body),
  remove: (id) => client.delete(`/schedules/${id}`),
}
