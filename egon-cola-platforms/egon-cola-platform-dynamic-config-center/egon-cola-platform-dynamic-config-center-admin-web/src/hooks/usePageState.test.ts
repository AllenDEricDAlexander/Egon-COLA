import { act, renderHook } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { usePageState } from './usePageState'

describe('usePageState', () => {
  it('resets page number when filters or page size change', () => {
    const { result } = renderHook(() => usePageState())

    act(() => result.current.onTableChange(3, 10))
    expect(result.current.page).toEqual({ pageNo: 3, pageSize: 10 })

    act(() => result.current.resetPage())
    expect(result.current.page).toEqual({ pageNo: 1, pageSize: 10 })

    act(() => result.current.onTableChange(2, 20))
    expect(result.current.page).toEqual({ pageNo: 1, pageSize: 20 })
  })
})
