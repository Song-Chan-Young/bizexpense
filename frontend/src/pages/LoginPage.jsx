import { useEffect, useState } from 'react'
import { Navigate, useLocation, useNavigate } from 'react-router-dom'
import client from '../api/client'
import { useAuth } from '../auth/AuthContext'

export default function LoginPage() {
  const { user, login } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const [loginId, setLoginId] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [submitting, setSubmitting] = useState(false)
  // 데모 서버면 체험 계정 버튼을 보여준다 (서버의 demo 설정에 따름)
  const [demo, setDemo] = useState(null)

  useEffect(() => {
    let ignore = false
    client
      .get('/public/demo')
      .then((info) => !ignore && info.enabled && setDemo(info))
      .catch(() => {})
    return () => {
      ignore = true
    }
  }, [])

  if (user) return <Navigate to="/" replace />

  const doLogin = async (id, pw) => {
    setError('')
    setSubmitting(true)
    try {
      await login(id, pw)
      navigate(location.state?.from?.pathname ?? '/', { replace: true })
    } catch (err) {
      setError(err.message)
      setSubmitting(false)
    }
  }

  const handleSubmit = (e) => {
    e.preventDefault()
    doLogin(loginId, password)
  }

  return (
    <div className="login-page">
      <form className="login-card" onSubmit={handleSubmit}>
        <h1>BizExpense</h1>
        <p className="subtitle">출장 · 경비 · 일정 관리</p>

        <label>
          아이디
          <input value={loginId} onChange={(e) => setLoginId(e.target.value)} autoFocus autoComplete="username" />
        </label>
        <label>
          비밀번호
          <input
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            autoComplete="current-password"
          />
        </label>

        {error && <p className="form-error">{error}</p>}

        <button type="submit" className="btn btn-primary" disabled={submitting}>
          {submitting ? '로그인 중…' : '로그인'}
        </button>

        {demo && (
          <div className="demo-box">
            <p className="demo-title">체험 계정으로 바로 둘러보기</p>
            <div className="demo-buttons">
              {demo.accounts.map((a) => (
                <button
                  key={a.loginId}
                  type="button"
                  className="btn btn-ghost"
                  disabled={submitting}
                  onClick={() => doLogin(a.loginId, demo.password)}
                >
                  <strong>{a.roleLabel}</strong>
                  <span>
                    {a.name} · {a.departmentName}
                  </span>
                </button>
              ))}
            </div>
            <p className="hint">
              직접 입력: {demo.accounts.map((a) => a.loginId).join(' / ')} · 비밀번호 {demo.password}
            </p>
          </div>
        )}
      </form>
    </div>
  )
}
