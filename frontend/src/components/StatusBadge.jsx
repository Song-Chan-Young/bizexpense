/** 상태 코드별 색은 index.css 의 .status-{code} 에 정의 */
export default function StatusBadge({ status, label }) {
  return <span className={`status-badge status-${status.toLowerCase()}`}>{label}</span>
}
