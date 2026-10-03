import client from './client'

export const AUDIT_TARGET_TYPES = [
  { value: 'AUTH', label: '인증' },
  { value: 'SCHEDULE', label: '일정' },
  { value: 'TRIP', label: '출장' },
  { value: 'EXPENSE', label: '경비' },
  { value: 'SETTLEMENT', label: '정산' },
  { value: 'APPROVAL', label: '결재' },
  { value: 'CODE', label: '기준 코드' },
]

export const auditApi = {
  // 응답: PageResponse<AuditLogResponse>
  search: (params) => client.get('/admin/audit-logs', { params }),
  actions: () => client.get('/admin/audit-logs/actions'),
}
