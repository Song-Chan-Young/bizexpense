import { useAuth } from '../auth/AuthContext'

// 통계 수치는 Phase 9(대시보드)에서 /api/dashboard/* 와 연결한다.
const CARDS = {
  USER: ['이번 달 내 경비', '정산 예정액', '결재 대기', '출장 예정', '오늘 일정'],
  MANAGER: ['결재 대기', '이번 달 팀 경비', '이번 달 출장', '반려 건수'],
  ADMIN: ['전체 경비', '이번 달 경비', '미결재', '정산 완료'],
}

export default function DashboardPage() {
  const { user } = useAuth()

  return (
    <>
      <h2 className="page-title">대시보드</h2>
      <p className="muted">
        {user.name}님, 안녕하세요. ({user.departmentName ?? '부서 없음'} · {user.roleLabel})
      </p>

      <div className="cards">
        {CARDS[user.role].map((title) => (
          <div key={title} className="card">
            <div className="card-title">{title}</div>
            <div className="card-value">–</div>
          </div>
        ))}
      </div>
    </>
  )
}
