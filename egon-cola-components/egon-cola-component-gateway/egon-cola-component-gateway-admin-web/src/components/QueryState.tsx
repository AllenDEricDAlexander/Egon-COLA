import { Alert, Button, Empty, Skeleton, Space, Typography } from 'antd'
import { GatewayApiError } from '../api/client'

export const QueryFailure = ({
  error,
  retry,
}: {
  error: unknown
  retry?: () => void
}) => {
  const apiError = error instanceof GatewayApiError ? error : undefined
  return (
    <Alert
      type="error"
      showIcon
      message={apiError?.message ?? '数据加载失败'}
      description={
        <Space direction="vertical">
          <Typography.Text code>{apiError?.code ?? 'UNKNOWN_ERROR'}</Typography.Text>
          {apiError?.traceId && (
            <Typography.Text copyable={{ text: apiError.traceId }}>
              Trace ID：{apiError.traceId}
            </Typography.Text>
          )}
          {retry && <Button onClick={retry}>重试</Button>}
        </Space>
      }
    />
  )
}

export const LoadingBlock = () => <Skeleton active paragraph={{ rows: 6 }} />

export const EmptyBlock = ({ description = '暂无数据' }: { description?: string }) => (
  <Empty description={description} />
)
