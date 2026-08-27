import { Alert, Button, Empty, Skeleton, Space } from 'antd'
import type { ReactNode } from 'react'
import { classifyApiError } from '../api/errors'
import { useT } from '../i18n'

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
  const t = useT()

  if (loading) {
    return skeleton ?? <Skeleton active paragraph={{ rows: 5 }} />
  }

  if (error !== null && error !== undefined) {
    const classified = classifyApiError(error)
    const errorTitle = classified.type === 'permission'
      ? t('page.permission', classified.title)
      : classified.title
    const banner = (
      <Alert
        type={classified.type === 'permission' ? 'warning' : 'error'}
        showIcon
        message={showPartial ? `${t('page.partial', '部分数据加载失败')}: ${errorTitle}` : errorTitle}
        action={onRetry ? <Button size="small" onClick={onRetry}>{t('retry', '重试')}</Button> : undefined}
        style={{ marginBottom: showPartial ? 16 : 0 }}
      />
    )
    if (showPartial) {
      return <Space direction="vertical" style={{ width: '100%' }}>{banner}{children}</Space>
    }
    return banner
  }

  if (empty) {
    return <Empty description={emptyDescription ?? t('empty', '暂无数据')} />
  }

  return children
}
