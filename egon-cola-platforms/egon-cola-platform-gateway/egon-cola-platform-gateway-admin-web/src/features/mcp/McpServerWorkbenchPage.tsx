import { useQuery } from '@tanstack/react-query'
import { Button, Card, Descriptions, Space, Tabs, Tag, Typography } from 'antd'
import { useNavigate, useParams } from 'react-router-dom'
import { gatewayApi } from '../../api/gatewayApi'
import { LoadingBlock, QueryFailure } from '../../components/QueryState'
import { McpProtocolInspector } from './McpProtocolInspector'
import { McpRemoteToolsPanel } from './McpRemoteToolsPanel'
import { McpToolsPanel } from './McpToolsPanel'
import { McpAppsPanel } from './McpAppsPanel'
import { McpApprovalPanel } from './McpApprovalPanel'
import { McpCapabilityPreview } from './McpCapabilityPreview'
import { McpPromptsPanel } from './McpPromptsPanel'
import { McpRemoteMountsPanel } from './McpRemoteMountsPanel'
import { McpResourcesPanel } from './McpResourcesPanel'
import { McpRuntimeStatus } from './McpRuntimeStatus'
import { McpTasksPanel } from './McpTasksPanel'

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
  if (draft.isLoading) return <LoadingBlock />
  if (draft.error || !draft.data) {
    return <QueryFailure error={draft.error ?? new Error('Gateway Draft 不存在')} />
  }

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
          <Descriptions.Item label="Resource URI">{server.data.resourceUri}</Descriptions.Item>
          <Descriptions.Item label="Server Revision">{server.data.revision}</Descriptions.Item>
          <Descriptions.Item label="Draft Revision">{draft.data.revision}</Descriptions.Item>
          <Descriptions.Item label="协议">
            <Space wrap>{server.data.dialects.map((dialect) => <Tag key={dialect}>{dialect}</Tag>)}</Space>
          </Descriptions.Item>
        </Descriptions>
      </Card>
      <Tabs
        destroyOnHidden={false}
        items={[
          {
            key: 'tools',
            label: 'Managed Tools',
            children: (
              <McpToolsPanel
                serverId={server.data.id}
                gatewayGroupId={server.data.gatewayGroupId}
                draftRevision={draft.data.revision}
              />
            ),
          },
          {
            key: 'remote-tools',
            label: 'Remote Tools',
            children: (
              <McpRemoteToolsPanel
                serverId={server.data.id}
                gatewayGroupId={server.data.gatewayGroupId}
                draftRevision={draft.data.revision}
              />
            ),
          },
          {
            key: 'resources',
            label: 'Resources',
            children: (
              <McpResourcesPanel
                serverId={server.data.id}
                gatewayGroupId={server.data.gatewayGroupId}
                draftRevision={draft.data.revision}
              />
            ),
          },
          {
            key: 'prompts',
            label: 'Prompts',
            children: (
              <McpPromptsPanel
                serverId={server.data.id}
                gatewayGroupId={server.data.gatewayGroupId}
                draftRevision={draft.data.revision}
              />
            ),
          },
          {
            key: 'tasks',
            label: 'Tasks',
            children: (
              <McpTasksPanel
                serverId={server.data.id}
                gatewayGroupId={server.data.gatewayGroupId}
                draftRevision={draft.data.revision}
              />
            ),
          },
          {
            key: 'apps',
            label: 'Apps',
            children: (
              <McpAppsPanel
                serverId={server.data.id}
                gatewayGroupId={server.data.gatewayGroupId}
                draftRevision={draft.data.revision}
              />
            ),
          },
          {
            key: 'remote-mounts',
            label: 'Remote Mounts',
            children: (
              <McpRemoteMountsPanel
                serverId={server.data.id}
                gatewayGroupId={server.data.gatewayGroupId}
                draftRevision={draft.data.revision}
              />
            ),
          },
          {
            key: 'preview',
            label: 'Preview & Release',
            children: (
              <McpCapabilityPreview
                serverId={server.data.id}
                gatewayGroupId={server.data.gatewayGroupId}
              />
            ),
          },
          {
            key: 'runtime',
            label: 'Runtime',
            children: <McpRuntimeStatus gatewayGroupId={server.data.gatewayGroupId} />,
          },
          {
            key: 'approvals',
            label: 'Approvals',
            children: (
              <McpApprovalPanel
                serverId={server.data.id}
                gatewayGroupId={server.data.gatewayGroupId}
                serverCode={server.data.serverCode}
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
