import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  Button,
  Descriptions,
  Form,
  Input,
  Modal,
  Popconfirm,
  Select,
  Space,
  Switch,
  Table,
  Tag,
  Typography,
  message,
} from 'antd'
import { useState } from 'react'
import { gatewayApi } from '../../api/gatewayApi'
import type { McpManagedTool, McpToolRiskLevel } from '../../api/types'
import { useCapability } from '../../app/capabilities'
import { QueryFailure } from '../../components/QueryState'

type OverrideForm = {
  serverId: string
  additionalPermissions: string[]
  minimumRiskLevel: McpToolRiskLevel
  disabled: boolean
  changeReason: string
}

const riskLevels: McpToolRiskLevel[] = ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL']
const riskRank = (riskLevel: McpToolRiskLevel): number => riskLevels.indexOf(riskLevel)
const riskColor = (riskLevel: McpToolRiskLevel): string => ({
  LOW: 'green',
  MEDIUM: 'orange',
  HIGH: 'red',
  CRITICAL: 'magenta',
})[riskLevel]

const permissionTags = (permissions: string[]) => permissions.length > 0
  ? <Space wrap size={[0, 4]}>{permissions.map((permission) => <Tag key={permission}>{permission}</Tag>)}</Space>
  : <Typography.Text type="secondary">无</Typography.Text>

export const McpToolsPanel = ({ serverId, gatewayGroupId, draftRevision }: {
  serverId: string
  gatewayGroupId: string
  draftRevision: number
}) => {
  const canWrite = useCapability('gateway:mcp:write')
  const queryClient = useQueryClient()
  const [messageApi, messageContext] = message.useMessage()
  const [form] = Form.useForm<OverrideForm>()
  const [editing, setEditing] = useState<McpManagedTool>()
  const tools = useQuery({
    queryKey: ['mcp-managed-tools', gatewayGroupId, serverId],
    queryFn: ({ signal }) => gatewayApi.mcpManagedTools(gatewayGroupId, serverId, signal),
  })
  const servers = useQuery({
    queryKey: ['mcp-servers', gatewayGroupId],
    queryFn: ({ signal }) => gatewayApi.mcpServers(gatewayGroupId, signal),
  })

  const invalidateTools = async () => {
    await Promise.all([
      queryClient.invalidateQueries({ queryKey: ['mcp-managed-tools', gatewayGroupId] }),
      queryClient.invalidateQueries({ queryKey: ['mcp-tool-references', gatewayGroupId] }),
      queryClient.invalidateQueries({ queryKey: ['gateway-draft', gatewayGroupId] }),
      queryClient.invalidateQueries({ queryKey: ['mcp-capability-preview', serverId] }),
    ])
  }
  const save = useMutation({
    mutationFn: (values: OverrideForm) => {
      if (!editing) throw new Error('Managed Tool 不存在')
      const additionalPermissions = [...new Set(values.additionalPermissions ?? [])]
        .filter((permission) => !editing.codePermissions.includes(permission))
        .sort()
      const removedPermission = editing.additionalPermissions.find(
        (permission) => !additionalPermissions.includes(permission),
      )
      if (removedPermission) throw new Error(`Override 只能追加权限，不能移除 ${removedPermission}`)
      const currentMinimumRisk = editing.minimumRiskLevel ?? editing.codeRiskLevel
      if (riskRank(values.minimumRiskLevel) < riskRank(currentMinimumRisk)) {
        throw new Error('Override 只能提高最低风险，不能降低当前最低风险')
      }
      const serverOverride = values.serverId === editing.codeServerId ? undefined : values.serverId
      const riskOverride = values.minimumRiskLevel === editing.codeRiskLevel
        ? undefined
        : values.minimumRiskLevel
      if (!serverOverride && additionalPermissions.length === 0 && !riskOverride && !values.disabled) {
        throw new Error('请至少设置一项严格 Override，或使用“恢复注解默认”')
      }
      return gatewayApi.updateMcpManagedToolOverride(editing.toolId, {
        gatewayGroupId,
        serverId: serverOverride,
        additionalPermissions,
        minimumRiskLevel: riskOverride,
        enabled: values.disabled ? false : undefined,
        expectedRevision: editing.overrideRevision,
        expectedDraftRevision: draftRevision,
        changeReason: values.changeReason,
      })
    },
    onSuccess: async () => {
      setEditing(undefined)
      form.resetFields()
      await invalidateTools()
      void messageApi.success('Managed Tool Override 已保存')
    },
  })
  const reset = useMutation({
    mutationFn: (tool: McpManagedTool) => gatewayApi.deleteMcpManagedToolOverride(tool.toolId, {
      gatewayGroupId,
      expectedRevision: tool.overrideRevision,
      expectedDraftRevision: draftRevision,
      changeReason: 'Restore annotation-defined Managed Tool defaults from Admin Web',
    }),
    onSuccess: async () => {
      await invalidateTools()
      void messageApi.success('Managed Tool 已恢复注解默认')
    },
  })

  const openOverride = (tool: McpManagedTool) => {
    save.reset()
    setEditing(tool)
    form.setFieldsValue({
      serverId: tool.serverId,
      additionalPermissions: tool.additionalPermissions,
      minimumRiskLevel: tool.minimumRiskLevel ?? tool.codeRiskLevel,
      disabled: !tool.enabled,
      changeReason: '',
    })
  }

  return (
    <section>
      {messageContext}
      {tools.error && <QueryFailure error={tools.error} retry={() => void tools.refetch()} />}
      {reset.error && <QueryFailure error={reset.error} />}
      <Table<McpManagedTool>
        rowKey="toolId"
        loading={tools.isLoading}
        dataSource={tools.data ?? []}
        scroll={{ x: 1240 }}
        columns={[
          {
            title: 'Tool',
            width: 220,
            fixed: 'start',
            render: (_, tool) => (
              <Space orientation="vertical" size={0}>
                <Typography.Text strong>{tool.name}</Typography.Text>
                <Typography.Text type="secondary">{tool.description || '无描述'}</Typography.Text>
              </Space>
            ),
          },
          {
            title: 'Protocol / Operation',
            width: 260,
            render: (_, tool) => (
              <Space orientation="vertical" size={0}>
                <Tag>{tool.operationProtocol}</Tag>
                <Typography.Text code>{tool.operationKey}</Typography.Text>
              </Space>
            ),
          },
          {
            title: 'Server',
            width: 180,
            render: (_, tool) => (
              <Space orientation="vertical" size={0}>
                <Typography.Text>{tool.serverCode}</Typography.Text>
                {tool.serverId !== tool.codeServerId && (
                  <Typography.Text type="secondary">代码默认：{tool.codeServerCode}</Typography.Text>
                )}
              </Space>
            ),
          },
          {
            title: 'Permissions',
            width: 320,
            render: (_, tool) => (
              <Space orientation="vertical" size={4}>
                <Typography.Text type="secondary">Code</Typography.Text>
                {permissionTags(tool.codePermissions)}
                <Typography.Text type="secondary">Effective</Typography.Text>
                {permissionTags(tool.effectivePermissions)}
              </Space>
            ),
          },
          {
            title: 'Risk',
            width: 180,
            render: (_, tool) => (
              <Space orientation="vertical" size={2}>
                <span>Code <Tag color={riskColor(tool.codeRiskLevel)}>{tool.codeRiskLevel}</Tag></span>
                <span>Effective <Tag color={riskColor(tool.effectiveRiskLevel)}>{tool.effectiveRiskLevel}</Tag></span>
              </Space>
            ),
          },
          {
            title: 'Runtime',
            width: 140,
            render: (_, tool) => (
              <Space orientation="vertical" size={2}>
                <Tag color={tool.enabled ? 'green' : 'default'}>
                  {tool.enabled ? 'ENABLED' : 'DISABLED'}
                </Tag>
                <Typography.Text type="secondary">
                  {tool.idempotent ? 'Idempotent' : 'Non-idempotent'}
                </Typography.Text>
              </Space>
            ),
          },
          {
            title: '操作',
            width: 200,
            fixed: 'end',
            render: (_, tool) => (
              <Space>
                <Button disabled={!canWrite} onClick={() => openOverride(tool)}>严格 Override</Button>
                <Popconfirm
                  title="恢复注解默认？"
                  description="这会清除该 Tool 的全部 Admin Override。"
                  onConfirm={() => reset.mutate(tool)}
                >
                  <Button disabled={!canWrite || tool.overrideRevision === 0}>恢复默认</Button>
                </Popconfirm>
              </Space>
            ),
          },
        ]}
      />
      <Modal
        title="Managed Tool 严格 Override"
        open={Boolean(editing)}
        width={680}
        onCancel={() => setEditing(undefined)}
        onOk={() => form.submit()}
        confirmLoading={save.isPending}
        destroyOnHidden
      >
        {save.error && <QueryFailure error={save.error} />}
        {editing && (
          <>
            <Descriptions size="small" column={1} style={{ marginBottom: 16 }}>
              <Descriptions.Item label="Tool">{editing.name}</Descriptions.Item>
              <Descriptions.Item label="Operation">{editing.operationKey}</Descriptions.Item>
              <Descriptions.Item label="代码权限">
                {permissionTags(editing.codePermissions)}
              </Descriptions.Item>
              <Descriptions.Item label="代码风险">
                <Tag color={riskColor(editing.codeRiskLevel)}>{editing.codeRiskLevel}</Tag>
              </Descriptions.Item>
            </Descriptions>
            <Form form={form} layout="vertical" onFinish={(values) => save.mutate(values)}>
              <Form.Item name="serverId" label="MCP Server" rules={[{ required: true }]}>
                <Select
                  loading={servers.isLoading}
                  options={(servers.data ?? []).map((server) => ({
                    value: server.id,
                    label: `${server.displayName} · ${server.serverCode}`,
                  }))}
                />
              </Form.Item>
              <Form.Item name="additionalPermissions" label="追加权限">
                <Select
                  mode="tags"
                  tokenSeparators={[',']}
                  options={editing.additionalPermissions.map((permission) => ({
                    value: permission,
                    label: permission,
                  }))}
                />
              </Form.Item>
              <Form.Item name="minimumRiskLevel" label="最低风险" rules={[{ required: true }]}>
                <Select options={riskLevels
                  .filter((riskLevel) => riskRank(riskLevel) >= riskRank(
                    editing.minimumRiskLevel ?? editing.codeRiskLevel,
                  ))
                  .map((riskLevel) => ({ value: riskLevel, label: riskLevel }))}
                />
              </Form.Item>
              <Form.Item name="disabled" label="禁用 Tool" valuePropName="checked">
                <Switch disabled={!editing.enabled} />
              </Form.Item>
              <Form.Item name="changeReason" label="变更原因" rules={[{ required: true }]}>
                <Input.TextArea />
              </Form.Item>
            </Form>
          </>
        )}
      </Modal>
    </section>
  )
}
