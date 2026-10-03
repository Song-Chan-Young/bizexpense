import { createContext, useCallback, useContext, useEffect, useState } from 'react'
import client, { tokenStorage } from '../api/client'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null)
  // 저장된 토큰으로 내 정보를 확인하는 동안에는 화면을 그리지 않는다.
  const [loading, setLoading] = useState(() => Boolean(tokenStorage.get()))

  useEffect(() => {
    if (!tokenStorage.get()) return
    client
      .get('/auth/me')
      .then(setUser)
      .catch(() => tokenStorage.clear())
      .finally(() => setLoading(false))
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
