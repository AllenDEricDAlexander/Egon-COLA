import { Alert, Button, Empty, Skeleton, Space } from 'antd'
import type { ReactNode } from 'react'
import { classifyApiError } from '../api/errors'

export interface PageStateProps {
  readonly loading: boolean
  readonly error: unknown
  readonly empty: boolean
  readonly emptyDescription?: string
  readonly skeleton?: ReactNode
  readonly showPartial?: boolean
  readonly onRetry?: () => void
  readonly children: ReactNode
}

export const PageState = ({
  loading,
  error,
  empty,
  emptyDescription = '暂无数据',
  skeleton,
  showPartial = false,
  onRetry,
  children,
}: PageStateProps) => {
  if (loading) {
    return skeleton ?? <Skeleton active paragraph={{ rows: 5 }} />
  }

  if (error !== null && error !== undefined) {
    const classified = classifyApiError(error)
    const banner = (
      <Alert
        type={classified.type === 'permission' ? 'warning' : 'error'}
        showIcon
        message={classified.title}
        action={onRetry ? <Button size="small" onClick={onRetry}>重试</Button> : undefined}
        style={{ marginBottom: showPartial ? 16 : 0 }}
      />
    )
    if (showPartial) {
      return <Space direction="vertical" style={{ width: '100%' }}>{banner}{children}</Space>
    }
    return banner
  }

  if (empty) {
    return <Empty description={emptyDescription} />
  }

  return children
}
