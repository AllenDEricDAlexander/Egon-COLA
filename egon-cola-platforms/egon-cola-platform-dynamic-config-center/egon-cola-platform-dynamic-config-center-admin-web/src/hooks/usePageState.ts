import { useCallback, useState } from 'react'

export function usePageState(initialPageSize = 10) {
  const [page, setPage] = useState({ pageNo: 1, pageSize: initialPageSize })

  const resetPage = useCallback(() => {
    setPage((current) => ({ ...current, pageNo: 1 }))
  }, [])

  const onTableChange = useCallback((pageNo: number, pageSize: number) => {
    setPage((current) => ({
      pageNo: current.pageSize === pageSize ? pageNo : 1,
      pageSize,
    }))
  }, [])

  return { page, resetPage, onTableChange }
}
