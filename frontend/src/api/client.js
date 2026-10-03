import axios from 'axios'

const TOKEN_KEY = 'bizexpense.accessToken'

export const tokenStorage = {
  get: () => localStorage.getItem(TOKEN_KEY),
  set: (token) => localStorage.setItem(TOKEN_KEY, token),
  clear: () => localStorage.removeItem(TOKEN_KEY),
}

const client = axios.create({ baseURL: '/api' })

client.interceptors.request.use((config) => {
  const token = tokenStorage.get()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// 백엔드 공통 응답 { success, data, error } 에서 data 만 꺼내 쓴다.
client.interceptors.response.use(
  (response) => response.data.data,
  (error) => {
    const status = error.response?.status
    const body = error.response?.data
    if (status === 401 && tokenStorage.get() && !error.config.url.endsWith('/auth/login')) {
      // 토큰 만료 등: 저장된 토큰을 지우고 로그인 화면으로 보낸다.
      tokenStorage.clear()
      window.location.assign('/login')
    }
    return Promise.reject({
      status,
      code: body?.error?.code ?? 'NETWORK_ERROR',
      message: body?.error?.message ?? '서버에 연결할 수 없습니다.',
    })
  },
)

export default client
