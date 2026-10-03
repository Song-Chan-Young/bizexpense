import { useEffect, useState } from 'react'
import client from './client'

export const codeApi = {
  categories: (all = false) => client.get('/codes/expense-categories', { params: { all } }),
  paymentMethods: (all = false) => client.get('/codes/payment-methods', { params: { all } }),
  createCategory: (body) => client.post('/admin/expense-categories', body),
  updateCategory: (id, body) => client.put(`/admin/expense-categories/${id}`, body),
  createPaymentMethod: (body) => client.post('/admin/payment-methods', body),
  updatePaymentMethod: (id, body) => client.put(`/admin/payment-methods/${id}`, body),
}

/** 사용 중인 비용 항목 / 결제 수단 (검색 조건, 등록 화면 선택 목록) */
export function useExpenseCodes() {
  const [codes, setCodes] = useState({ categories: [], paymentMethods: [] })

  useEffect(() => {
    let ignore = false
    Promise.all([codeApi.categories(), codeApi.paymentMethods()])
      .then(([categories, paymentMethods]) => !ignore && setCodes({ categories, paymentMethods }))
      .catch(() => {})
    return () => {
      ignore = true
    }
  }, [])

  return codes
}
