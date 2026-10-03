const pad = (n) => String(n).padStart(2, '0')

/** Date -> 'YYYY-MM-DD' (로컬 시간 기준) */
export const toDateParam = (d) => `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`

/** Date -> 'YYYY-MM-DDTHH:mm' (<input type="datetime-local"> 값, 서버 LocalDateTime 형식) */
export const toLocalInput = (d) => `${toDateParam(d)}T${pad(d.getHours())}:${pad(d.getMinutes())}`

/** 서버 LocalDateTime 문자열 -> '10/12 (월) 14:00' */
export function formatDateTime(value) {
  const d = new Date(value)
  const day = '일월화수목금토'[d.getDay()]
  return `${d.getMonth() + 1}/${d.getDate()} (${day}) ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

/** 시작~종료 표시. 같은 날이면 종료는 시각만 */
export function formatRange(start, end) {
  const s = new Date(start)
  const e = new Date(end)
  const sameDay = s.toDateString() === e.toDateString()
  return `${formatDateTime(start)} ~ ${sameDay ? `${pad(e.getHours())}:${pad(e.getMinutes())}` : formatDateTime(end)}`
}

/** 'YYYY-MM-DD' -> '10/12 (월)' */
export function formatDate(value) {
  const d = new Date(`${value}T00:00:00`)
  return `${d.getMonth() + 1}/${d.getDate()} (${'일월화수목금토'[d.getDay()]})`
}

/** 서버 LocalDateTime -> '2026-10-03 11:20' */
export function formatTimestamp(value) {
  if (!value) return '-'
  const d = new Date(value)
  return `${toDateParam(d)} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}
