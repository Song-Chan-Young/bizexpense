import { useEffect, useRef, useState } from 'react'
import { RECEIPT_ACCEPT, RECEIPT_MAX_COUNT, RECEIPT_MAX_SIZE, receiptApi } from '../../api/expenses'

const formatSize = (bytes) =>
  bytes >= 1024 * 1024 ? `${(bytes / 1024 / 1024).toFixed(1)}MB` : `${Math.max(1, Math.round(bytes / 1024))}KB`

const isImage = (file) => file.contentType.startsWith('image/')

/**
 * 경비 상세의 영수증 영역: 목록(이미지는 미리보기) + 첨부/삭제.
 * 파일 API 는 인증 헤더가 필요해 blob 으로 받아 object URL 로 보여 준다.
 * editable 이면 첨부/삭제할 수 있고, 첨부 목록이 바뀌면 onChanged 로 알린다.
 */
export default function ReceiptSection({ expenseId, editable, onChanged }) {
  const [files, setFiles] = useState(null)
  const [thumbs, setThumbs] = useState({}) // fileId -> object URL
  const [uploading, setUploading] = useState(false)
  const [confirmingId, setConfirmingId] = useState(null) // 삭제 확인 중인 파일
  const [error, setError] = useState('')
  const [reloadKey, setReloadKey] = useState(0)
  const inputRef = useRef(null)

  useEffect(() => {
    let ignore = false
    const urls = []
    receiptApi
      .list(expenseId)
      .then(async (list) => {
        if (ignore) return
        setFiles(list)
        const entries = await Promise.all(
          list.filter(isImage).map((f) =>
            receiptApi
              .blob(expenseId, f.fileId)
              .then((blob) => [f.fileId, URL.createObjectURL(blob)])
              .catch(() => null),
          ),
        )
        const loaded = entries.filter(Boolean)
        loaded.forEach(([, url]) => urls.push(url))
        if (!ignore) setThumbs(Object.fromEntries(loaded))
      })
      .catch((err) => !ignore && setError(err.message))
    return () => {
      ignore = true
      urls.forEach((url) => URL.revokeObjectURL(url))
    }
  }, [expenseId, reloadKey])

  const changed = () => {
    setReloadKey((k) => k + 1)
    onChanged?.()
  }

  const handleSelect = async (e) => {
    const selected = [...e.target.files]
    e.target.value = ''
    if (selected.length === 0) return
    setError('')
    const remaining = RECEIPT_MAX_COUNT - files.length
    if (selected.length > remaining) {
      setError(`영수증은 경비 1건에 ${RECEIPT_MAX_COUNT}개까지 첨부할 수 있습니다. (남은 수: ${remaining}개)`)
      return
    }
    const tooLarge = selected.find((f) => f.size > RECEIPT_MAX_SIZE)
    if (tooLarge) {
      setError(`'${tooLarge.name}' 파일이 5MB 를 넘습니다.`)
      return
    }
    setUploading(true)
    const failed = []
    for (const file of selected) {
      try {
        await receiptApi.upload(expenseId, file)
      } catch (err) {
        failed.push(`${file.name}: ${err.message}`)
      }
    }
    setUploading(false)
    if (failed.length) setError(failed.join('\n'))
    if (failed.length < selected.length) changed()
  }

  const handleDelete = async (file) => {
    setConfirmingId(null)
    setError('')
    try {
      await receiptApi.remove(expenseId, file.fileId)
      changed()
    } catch (err) {
      setError(err.message)
    }
  }

  // 팝업 차단을 피하려고 창을 먼저 연 뒤, 파일을 받아 주소를 넣는다
  const handleOpen = async (file) => {
    const win = window.open('', '_blank')
    try {
      let url = thumbs[file.fileId]
      if (!url) {
        url = URL.createObjectURL(await receiptApi.blob(expenseId, file.fileId))
        setTimeout(() => URL.revokeObjectURL(url), 60_000)
      }
      if (win) win.location.href = url
      else window.location.assign(url)
    } catch (err) {
      win?.close()
      setError(err.message)
    }
  }

  return (
    <section className="receipts">
      <div className="receipts-head">
        <h5>
          영수증 <span className="muted">{files ? `${files.length}/${RECEIPT_MAX_COUNT}` : ''}</span>
        </h5>
        {editable && files && files.length < RECEIPT_MAX_COUNT && (
          <>
            <input
              ref={inputRef}
              type="file"
              accept={RECEIPT_ACCEPT}
              multiple
              hidden
              onChange={handleSelect}
            />
            <button
              type="button"
              className="btn btn-ghost btn-sm"
              disabled={uploading}
              onClick={() => inputRef.current.click()}
            >
              {uploading ? '올리는 중…' : '파일 첨부'}
            </button>
          </>
        )}
      </div>
      {files === null && !error && <p className="muted small">불러오는 중…</p>}
      {files?.length === 0 && (
        <p className="muted small">
          첨부된 영수증이 없습니다.
          {editable && ' 이미지(JPG, PNG, GIF, WEBP) 또는 PDF, 5MB 까지 첨부할 수 있습니다.'}
        </p>
      )}
      {files?.length > 0 && (
        <ul className="receipt-list">
          {files.map((f) => (
            <li key={f.fileId} className="receipt-item">
              <button type="button" className="receipt-thumb" onClick={() => handleOpen(f)} title="새 창에서 보기">
                {thumbs[f.fileId] ? (
                  <img src={thumbs[f.fileId]} alt={f.originalName} />
                ) : (
                  <span className="receipt-icon">{isImage(f) ? 'IMG' : 'PDF'}</span>
                )}
              </button>
              <div className="receipt-meta">
                <span className="receipt-name" title={f.originalName}>
                  {f.originalName}
                </span>
                <span className="muted">{formatSize(f.size)}</span>
              </div>
              {editable &&
                (confirmingId === f.fileId ? (
                  <span className="receipt-confirm">
                    <button type="button" className="btn btn-ghost btn-sm" onClick={() => setConfirmingId(null)}>
                      취소
                    </button>
                    <button type="button" className="btn btn-danger btn-sm" onClick={() => handleDelete(f)}>
                      삭제
                    </button>
                  </span>
                ) : (
                  <button
                    type="button"
                    className="icon-btn receipt-remove"
                    aria-label={`${f.originalName} 삭제`}
                    onClick={() => setConfirmingId(f.fileId)}
                  >
                    ×
                  </button>
                ))}
            </li>
          ))}
        </ul>
      )}
      {error && <p className="form-error pre">{error}</p>}
    </section>
  )
}
