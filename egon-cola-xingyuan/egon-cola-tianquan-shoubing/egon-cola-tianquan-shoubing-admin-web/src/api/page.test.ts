import { describe, expect, it } from 'vitest'
import { normalizePage } from './page'

describe('normalizePage', () => {
  it('normalizes a legacy array into the first page', () => {
    expect(normalizePage([{ subject: 'u-1' }])).toEqual({
      content: [{ subject: 'u-1' }],
      page: 0,
      size: 1,
      totalElements: 1,
      totalPages: 1,
    })
  })

  it('keeps server page metadata and empty content', () => {
    const result = normalizePage({
      content: [],
      page: 2,
      size: 20,
      totalElements: 40,
      totalPages: 2,
    })

    expect(result.page).toBe(2)
    expect(result.totalElements).toBe(40)
    expect(result.content).toEqual([])
  })

  it('rejects an invalid page envelope', () => {
    expect(() => normalizePage({ content: 'not-an-array' } as never)).toThrow('IDP 分页响应格式无效')
  })
})
