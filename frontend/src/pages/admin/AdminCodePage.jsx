import { useCallback, useEffect, useState } from 'react'
import { codeApi } from '../../api/codes'

/** 관리자: 비용 항목 / 결제 수단 관리. 삭제 대신 사용 중지로 기존 경비 데이터를 보존한다. */
export default function AdminCodePage() {
  return (
    <>
      <div className="page-header">
        <h2 className="page-title">기준 코드 관리</h2>
      </div>
      <div className="two-columns">
        <CodePanel
          title="비용 항목"
          idKey="categoryId"
          load={() => codeApi.categories(true)}
          create={(body) => codeApi.createCategory(body)}
          update={(id, body) => codeApi.updateCategory(id, body)}
        />
        <CodePanel
          title="결제 수단"
          idKey="paymentMethodId"
          withCorporate
          load={() => codeApi.paymentMethods(true)}
          create={(body) => codeApi.createPaymentMethod(body)}
          update={(id, body) => codeApi.updatePaymentMethod(id, body)}
        />
      </div>
    </>
  )
}

function CodePanel({ title, idKey, withCorporate, load, create, update }) {
  const [items, setItems] = useState([])
  const [newName, setNewName] = useState('')
  const [newCorporate, setNewCorporate] = useState(false)
  // { id, name } 이름 수정 중인 행
  const [editing, setEditing] = useState(null)
  const [error, setError] = useState('')

  const reload = useCallback(() => {
    load()
      .then(setItems)
      .catch((err) => setError(err.message))
    // load 는 렌더마다 새로 만들어지지만 같은 API 를 부르므로 최초 1회만 연결한다
    // oxlint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  useEffect(reload, [reload])

  const run = async (action) => {
    setError('')
    try {
      await action()
      reload()
      return true
    } catch (err) {
      setError(err.message)
      return false
    }
  }

  const handleAdd = async (e) => {
    e.preventDefault()
    if (await run(() => create({ name: newName, corporate: newCorporate }))) {
      setNewName('')
      setNewCorporate(false)
    }
  }

  const save = (item, patch) =>
    run(() =>
      update(item[idKey], {
        name: item.name,
        active: item.active,
        ...(withCorporate ? { corporate: item.corporate } : {}),
        ...patch,
      }),
    )

  return (
    <section className="panel">
      <div className="panel-header">
        <h3>{title}</h3>
      </div>
      <table className="table compact">
        <thead>
          <tr>
            <th>이름</th>
            {withCorporate && <th>법인 결제</th>}
            <th>사용</th>
            <th />
          </tr>
        </thead>
        <tbody>
          {items.map((item) => (
            <tr key={item[idKey]} className={item.active ? '' : 'inactive'}>
              <td>
                {editing?.id === item[idKey] ? (
                  <input
                    className="inline-input"
                    value={editing.name}
                    onChange={(e) => setEditing({ ...editing, name: e.target.value })}
                    autoFocus
                  />
                ) : (
                  item.name
                )}
              </td>
              {withCorporate && (
                <td>
                  <input
                    type="checkbox"
                    checked={item.corporate}
                    onChange={(e) => save(item, { corporate: e.target.checked })}
                    aria-label={`${item.name} 법인 결제`}
                  />
                </td>
              )}
              <td>
                <input
                  type="checkbox"
                  checked={item.active}
                  onChange={(e) => save(item, { active: e.target.checked })}
                  aria-label={`${item.name} 사용`}
                />
              </td>
              <td className="num nowrap">
                {editing?.id === item[idKey] ? (
                  <>
                    <button type="button" className="btn btn-ghost btn-sm" onClick={() => setEditing(null)}>
                      취소
                    </button>
                    <button
                      type="button"
                      className="btn btn-primary btn-sm"
                      onClick={async () => (await save(item, { name: editing.name })) && setEditing(null)}
                    >
                      저장
                    </button>
                  </>
                ) : (
                  <button
                    type="button"
                    className="btn btn-ghost btn-sm"
                    onClick={() => setEditing({ id: item[idKey], name: item.name })}
                  >
                    이름 변경
                  </button>
                )}
              </td>
            </tr>
          ))}
        </tbody>
      </table>

      <form className="inline-form" onSubmit={handleAdd}>
        <input placeholder={`새 ${title}`} value={newName} onChange={(e) => setNewName(e.target.value)} required />
        {withCorporate && (
          <label className="checkbox">
            <input type="checkbox" checked={newCorporate} onChange={(e) => setNewCorporate(e.target.checked)} />
            법인 결제
          </label>
        )}
        <button type="submit" className="btn btn-primary btn-sm">
          추가
        </button>
      </form>
      {error && <p className="form-error">{error}</p>}
    </section>
  )
}
