import { NavLink, Outlet, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'

// ready: false 인 메뉴는 이후 Phase 에서 구현 예정
const MENUS = [
  { to: '/', label: '대시보드', ready: true },
  { to: '/schedules', label: '일정', ready: true },
  { to: '/trips', label: '출장', ready: true },
  { to: '/expenses', label: '경비', ready: true },
  { to: '/settlements', label: '정산', ready: true },
  { to: '/approvals', label: '결재', ready: true, roles: ['MANAGER', 'ADMIN'] },
  { to: '/admin', label: '기준 코드', ready: true, roles: ['ADMIN'] },
]

export default function Layout() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()

  const handleLogout = async () => {
    await logout()
    navigate('/login', { replace: true })
  }

  const menus = MENUS.filter((m) => !m.roles || m.roles.includes(user.role))

  return (
    <div className="app">
      <aside className="sidebar">
        <div className="brand">BizExpense</div>
        <nav>
          {menus.map((m) =>
            m.ready ? (
              <NavLink key={m.to} to={m.to} end={m.to === '/'} className="nav-item">
                {m.label}
              </NavLink>
            ) : (
              <span key={m.to} className="nav-item disabled" title="준비 중">
                {m.label}
                <small>준비 중</small>
              </span>
            ),
          )}
        </nav>
      </aside>

      <div className="main">
        <header className="topbar">
          <span className="who">
            {user.departmentName && <span className="dept">{user.departmentName}</span>}
            <strong>{user.name}</strong>
            <span className={`role role-${user.role.toLowerCase()}`}>{user.roleLabel}</span>
          </span>
          <button type="button" className="btn btn-ghost" onClick={handleLogout}>
            로그아웃
          </button>
        </header>
        <main className="content">
          <Outlet />
        </main>
      </div>
    </div>
  )
}
