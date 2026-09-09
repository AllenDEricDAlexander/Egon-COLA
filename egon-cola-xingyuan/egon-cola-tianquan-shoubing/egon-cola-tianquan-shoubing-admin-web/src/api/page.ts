export interface PageEnvelope<T> {
  readonly content: readonly T[]
  readonly page: number
  readonly size: number
  readonly totalElements: number
  readonly totalPages: number
}

export type IdentityPage<T> = PageEnvelope<T>

const isPageEnvelope = <T>(value: unknown): value is PageEnvelope<T> => {
  if (value === null || typeof value !== 'object' || Array.isArray(value)) return false

  const record = value as Record<string, unknown>
  return Array.isArray(record.content)
    && typeof record.page === 'number'
    && typeof record.size === 'number'
    && typeof record.totalElements === 'number'
    && typeof record.totalPages === 'number'
}

export const normalizePage = <T>(value: readonly T[] | PageEnvelope<T>): IdentityPage<T> => {
  if (Array.isArray(value)) {
    return {
      content: value,
      page: 0,
      size: value.length,
      totalElements: value.length,
      totalPages: value.length === 0 ? 0 : 1,
    }
  }

  if (!isPageEnvelope<T>(value)) throw new Error('IDP 分页响应格式无效')
  return value
}
