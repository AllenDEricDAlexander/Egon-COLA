import { useQuery } from '@tanstack/react-query'
import { Button, Card, Descriptions, Space, Tabs, Tag, Typography } from 'antd'
import { useNavigate, useParams } from 'react-router-dom'
import { gatewayApi } from '../../api/gatewayApi'
import { LoadingBlock, QueryFailure } from '../../components/QueryState'
import { McpProtocolInspector } from './McpProtocolInspector'
import { McpToolsPanel } from './McpToolsPanel'

export const McpServerWorkbenchPage = () => {
  const { serverId = '' } = useParams()
  const navigate = useNavigate()
  const server = useQuery({
    queryKey: ['mcp-server', serverId],
    queryFn: ({ signal }) => gatewayApi.mcpServer(serverId, signal),
    enabled: Boolean(serverId),
  })
  const gatewayGroupId = server.data?.gatewayGroupId ?? ''
  const draft = useQuery({
    queryKey: ['gateway-draft', gatewayGroupId],
    queryFn: ({ signal }) => gatewayApi.draft(gatewayGroupId, signal),
    enabled: Boolean(gatewayGroupId),
  })

  if (server.isLoading) return <LoadingBlock />
  if (server.error) return <QueryFailure error={server.error} retry={() => void server.refetch()} />
  if (!server.data) return <QueryFailure error={new Error('MCP Server 不存在')} />

  return (
    <section>
      <Space className="page-title" align="center" wrap>
        <Button onClick={() => navigate('/mcp/servers')}>返回</Button>
        <Typography.Title level={2}>{server.data.displayName}</Typography.Title>
        <Typography.Text code>{server.data.serverCode}</Typography.Text>
        <Tag color={server.data.enabled ? 'green' : 'default'}>
          {server.data.enabled ? 'ENABLED' : 'DISABLED'}
        </Tag>
      </Space>
      <Card style={{ marginBottom: 16 }}>
        <Descriptions size="small" column={{ xs: 1, md: 3 }}>
          <Descriptions.Item label="OAuth Audience">{server.data.oauthAudience}</Descriptions.Item>
          <Descriptions.Item label="Server Revision">{server.data.revision}</Descriptions.Item>
          <Descriptions.Item label="Draft Revision">{draft.data?.revision ?? '-'}</Descriptions.Item>
          <Descriptions.Item label="协议">
            <Space wrap>{server.data.dialects.map((dialect) => <Tag key={dialect}>{dialect}</Tag>)}</Space>
          </Descriptions.Item>
        </Descriptions>
      </Card>
      {draft.error && <QueryFailure error={draft.error} retry={() => void draft.refetch()} />}
      <Tabs
        destroyOnHidden={false}
        items={[
          {
            key: 'tools',
            label: 'Tools',
            children: (
              <McpToolsPanel
                serverId={server.data.id}
                gatewayGroupId={server.data.gatewayGroupId}
                draftRevision={draft.data?.revision ?? 0}
              />
            ),
          },
          {
            key: 'protocol-inspector',
            label: 'Protocol Inspector',
            children: (
              <McpProtocolInspector serverId={server.data.id} dialects={server.data.dialects} />
            ),
          },
        ]}
      />
    </section>
  )
}
