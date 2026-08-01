import { Alert, Empty, Skeleton } from 'antd'
import type { ReactNode } from 'react'

export interface PageStateProps {
  readonly loading: boolean
  readonly error: unknown
  readonly empty: boolean
  readonly emptyDescription?: string
  readonly children: ReactNode
}

export const PageState = ({
  loading,
  error,
  empty,
  emptyDescription = '暂无数据',
  children,
}: PageStateProps) => {
  if (loading) {
    return <Skeleton active paragraph={{ rows: 5 }} />
  }
  if (error !== null && error !== undefined) {
    const state = classifyFeatureError(error)
    return <Alert type={state.type} showIcon message={state.title} description={state.code} />
  }
  if (empty) {
    return <Empty description={emptyDescription} />
  }
  return children
}

const classifyFeatureError = (error: unknown): {
  readonly type: 'error' | 'warning'
  readonly title: string
  readonly code: string
} => {
  const value = error as { status?: number; code?: string }
  const code = typeof value?.code === 'string' ? value.code : 'REQUEST_FAILED'
  switch (value?.status) {
    case 403:
      return { type: 'warning', title: '无权访问', code }
    case 409:
      return { type: 'warning', title: '数据已发生变化，请保留输入并刷新比较', code }
    case 422:
      return { type: 'warning', title: '输入未通过业务校验', code }
    case 503:
      return { type: 'warning', title: '权限运行时暂不可用，可稍后重试', code }
    default:
      return { type: 'error', title: '加载失败', code }
  }
}
