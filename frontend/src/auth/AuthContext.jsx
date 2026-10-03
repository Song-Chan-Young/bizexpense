import { createContext, useCallback, useContext, useEffect, useState } from 'react'
import client, { tokenStorage } from '../api/client'

const AuthContext = createContext(null)

// 사용자가 직접 로그아웃하면 이 브라우저에서는 더 이상 자동 로그인하지 않는다 (다른 계정으로 체험할 수 있게)
const AUTO_LOGIN_OFF_KEY = 'bizexpense.autoLoginOff'
const AUTO_LOGIN_ID = 'admin'

const autoLoginOff = {
  get: () => {
    try {
      return localStorage.getItem(AUTO_LOGIN_OFF_KEY) === '1'
    } catch {
      return false
    }
  },
  set: () => {
    try {
      localStorage.setItem(AUTO_LOGIN_OFF_KEY, '1')
    } catch {
      // 저장소를 쓸 수 없는 브라우저(사생활 보호 모드 등)는 무시
    }
  },
}

/**
 * 로그인 상태 관리.
 * - 저장된 토큰이 있으면 내 정보를 확인한다.
 * - 토큰이 없고 데모 서버면(서버 demo 설정) 처음 접속한 사람을 관리자로 자동 로그인한다.
 */
export function AuthProvider({ children }) {
  const [user, setUser] = useState(null)
  // 로그인 상태를 확인(또는 자동 로그인)하는 동안에는 화면을 그리지 않는다.
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    let ignore = false

    async function init() {
      if (tokenStorage.get()) {
        const me = await client.get('/auth/me')
        if (!ignore) setUser(me)
        return
      }
      if (autoLoginOff.get()) return

      const demo = await client.get('/public/demo')
      const account = demo.enabled && demo.accounts.find((a) => a.loginId === AUTO_LOGIN_ID)
      if (!account || ignore) return
      const result = await client.post('/auth/login', { loginId: account.loginId, password: demo.password })
      if (ignore) return
      tokenStorage.set(result.accessToken)
      setUser(result.user)
    }

    init()
      .catch(() => !ignore && tokenStorage.clear())
      .finally(() => !ignore && setLoading(false))
    return () => {
      ignore = true
    }
  }, [])

  const login = useCallback(async (loginId, password) => {
    const result = await client.post('/auth/login', { loginId, password })
    tokenStorage.set(result.accessToken)
    setUser(result.user)
    return result.user
  }, [])

  const logout = useCallback(async () => {
    try {
      await client.post('/auth/logout')
    } finally {
      tokenStorage.clear()
      autoLoginOff.set()
      setUser(null)
    }
  }, [])

  return (
    <AuthContext.Provider value={{ user, loading, login, logout }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  return useContext(AuthContext)
}
