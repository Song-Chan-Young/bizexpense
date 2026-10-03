/** page 는 0부터 시작 (서버 PageResponse 와 동일) */
export default function Pagination({ page, totalPages, totalElements, onChange }) {
  if (totalPages <= 1) {
    return <div className="pagination muted">총 {totalElements}건</div>
  }

  // 현재 페이지 주변 최대 5개 번호만 보여준다
  const start = Math.max(0, Math.min(page - 2, totalPages - 5))
  const pages = Array.from({ length: Math.min(5, totalPages) }, (_, i) => start + i)

  return (
    <div className="pagination">
      <span className="muted">총 {totalElements}건</span>
      <button type="button" className="page-btn" disabled={page === 0} onClick={() => onChange(page - 1)}>
        ‹
      </button>
      {pages.map((p) => (
        <button
          key={p}
          type="button"
          className={`page-btn ${p === page ? 'active' : ''}`}
          onClick={() => onChange(p)}
        >
          {p + 1}
        </button>
      ))}
      <button
        type="button"
        className="page-btn"
        disabled={page >= totalPages - 1}
        onClick={() => onChange(page + 1)}
      >
        ›
      </button>
    </div>
  )
}
