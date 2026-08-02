import { useMutation, useQuery } from '@tanstack/react-query'
import { Alert, Button, Card, Space, Typography } from 'antd'
import { useNavigate } from 'react-router-dom'
import { gatewayApi } from '../../api/gatewayApi'
import { useCapability } from '../../app/capabilities'
import { JsonPanel } from '../../components/JsonPanel'
import { LoadingBlock, QueryFailure } from '../../components/QueryState'

export const McpCapabilityPreview = ({ serverId, gatewayGroupId }: {
  serverId: string
  gatewayGroupId: string
}) => {
  const navigate = useNavigate()
  const canRelease = useCapability('gateway:mcp:release')
  const canPublish = useCapability('gateway:releases:write')
  const preview = useQuery({
    queryKey: ['mcp-capability-preview', serverId],
    queryFn: ({ signal }) => gatewayApi.previewMcpServer(serverId, signal),
  })
  const validate = useMutation({
    mutationFn: () => gatewayApi.validateMcpServer(serverId),
  })
  const report = validate.data ?? preview.data?.validation

  if (preview.isLoading) return <LoadingBlock />
  if (preview.error) return <QueryFailure error={preview.error} retry={() => void preview.refetch()} />

  return (
    <section>
      <Space style={{ marginBottom: 16 }} wrap>
        <Button loading={validate.isPending} onClick={() => validate.mutate()}>
          重新校验
        </Button>
        <Button
          type="primary"
          disabled={!canRelease || !canPublish || report?.valid !== true}
          onClick={() => navigate(`/gateway-groups/${gatewayGroupId}/releases`)}
        >
          发布
        </Button>
        <Typography.Text type="secondary">
          MCP 与 HTTP/RPC 共用一个 Gateway Draft 和 Release。
        </Typography.Text>
      </Space>
      {validate.error && <QueryFailure error={validate.error} />}
      <Card title={report?.valid ? 'MCP 校验通过' : 'MCP 校验未通过'}>
        {(report?.findings ?? []).map((finding) => (
          <Alert
            key={`${finding.path}:${finding.code}`}
            type="error"
            showIcon
            title={finding.code}
            description={`${finding.path} · ${finding.message}`}
            style={{ marginBottom: 8 }}
          />
        ))}
      </Card>
      {preview.data && <JsonPanel title="完整 MCP Capability Preview" value={preview.data.content} />}
    </section>
  )
}
