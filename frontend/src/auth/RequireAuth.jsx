import { Navigate, useLocation } from 'react-router-dom'
import { useAuth } from './AuthContext'

/** 로그인하지 않았으면 로그인 화면으로, roles 가 주어지면 해당 권한만 통과시킨다. */
export default function RequireAuth({ roles, children }) {
  const { user, loading } = useAuth()
  const location = useLocation()

  if (loading) return null
  if (!user) return <Navigate to="/login" replace state={{ from: location }} />
  if (roles && !roles.includes(user.role)) return <Navigate to="/" replace />
  return children
}
