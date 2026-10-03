import client from './client'

export const dashboardApi = {
  // 응답: { scope, cards: [{ key, label, value, unit, hint, link }], monthly, categories }
  get: () => client.get('/dashboard'),
}
