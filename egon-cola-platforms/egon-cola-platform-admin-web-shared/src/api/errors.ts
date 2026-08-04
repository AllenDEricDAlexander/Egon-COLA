export class ApiError extends Error {
  readonly status: number
  readonly code: string
  readonly requestId?: string
  readonly retryable: boolean

  constructor(
    message: string,
    status: number,
    code: string,
    options?: { requestId?: string; retryable?: boolean },
  ) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.code = code
    this.requestId = options?.requestId
    this.retryable = options?.retryable ?? (status >= 500 || status === 0 || status === 429)
  }
}

export interface ErrorClassification {
  readonly type: 'auth' | 'permission' | 'validation' | 'server' | 'network'
  readonly title: string
  readonly retryable: boolean
}

export const classifyApiError = (error: unknown): ErrorClassification => {
  if (error instanceof ApiError) {
    return classifyByStatus(error.status)
  }
  if (error instanceof TypeError && error.message === 'Failed to fetch') {
    return { type: 'network', title: '网络连接失败，请检查网络后重试', retryable: true }
  }
  if (error instanceof DOMException && error.name === 'AbortError') {
    return { type: 'network', title: '请求已取消', retryable: false }
  }
  return { type: 'server', title: '未知错误', retryable: false }
}

const classifyByStatus = (status: number): ErrorClassification => {
  switch (true) {
    case status === 401:
      return { type: 'auth', title: '登录已过期，请重新登录', retryable: false }
    case status === 403:
      return { type: 'permission', title: '无权访问', retryable: false }
    case status === 409:
      return { type: 'validation', title: '数据已发生变化，请刷新后重试', retryable: false }
    case status === 422:
      return { type: 'validation', title: '输入未通过业务校验', retryable: false }
    case status === 429:
      return { type: 'server', title: '请求过于频繁，请稍后重试', retryable: true }
    case status >= 500:
      return { type: 'server', title: '服务暂时不可用，请稍后重试', retryable: true }
    default:
      return { type: 'server', title: `请求失败 (${status})`, retryable: false }
  }
}
