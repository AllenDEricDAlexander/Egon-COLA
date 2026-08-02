import { useMutation, useQuery } from '@tanstack/react-query'
import { Alert, Button, Form, Input, InputNumber, Space, Typography } from 'antd'
import { useState } from 'react'
import { gatewayApi } from '../../api/gatewayApi'
import { useCapability } from '../../app/capabilities'
import { QueryFailure } from '../../components/QueryState'
import { parseJsonObject } from './mcpValidation'

type ApprovalForm = {
  toolName: string
  arguments: string
  ttlSeconds: number
}

export const McpApprovalPanel = ({
  serverId,
  gatewayGroupId,
  serverCode,
}: {
  serverId: string
  gatewayGroupId: string
  serverCode: string
}) => {
  const canApprove = useCapability('gateway:mcp:approve')
  const [form] = Form.useForm<ApprovalForm>()
  const [visibleToken, setVisibleToken] = useState<string>()
  const tools = useQuery({
    queryKey: ['mcp-capabilities', gatewayGroupId, serverId, 'tools'],
    queryFn: ({ signal }) => gatewayApi.mcpCapabilities(
      gatewayGroupId,
      serverId,
      'tools',
      signal,
    ),
  })
  const issue = useMutation({
    mutationFn: (values: ApprovalForm) => gatewayApi.issueMcpApproval({
      serverCode,
      toolName: values.toolName,
      arguments: parseJsonObject(values.arguments, 'Tool Arguments'),
      ttlSeconds: values.ttlSeconds,
    }),
    onSuccess: (approval) => setVisibleToken(approval.approvalToken),
  })

  return (
    <section>
      <Alert
        type="warning"
        showIcon
        title="Approval Token 与当前用户、租户、客户端、Tool 和参数摘要绑定，最长有效 300 秒。"
        description="Token 只显示一次；本页面不会把它写入日志或浏览器持久存储。"
        style={{ marginBottom: 16 }}
      />
      {!canApprove ? (
        <Typography.Text type="secondary">当前账号缺少 gateway:mcp:approve 能力。</Typography.Text>
      ) : (
        <Form
          form={form}
          layout="vertical"
          initialValues={{ arguments: '{}', ttlSeconds: 120 }}
          onFinish={(values) => {
            setVisibleToken(undefined)
            issue.mutate(values)
          }}
        >
          <Form.Item name="toolName" label="High-Risk Tool" rules={[{ required: true }]}>
            <Input list="mcp-approval-tools" />
          </Form.Item>
          <datalist id="mcp-approval-tools">
            {(tools.data ?? [])
              .filter((tool) => tool.content.riskLevel === 'HIGH')
              .map((tool) => <option key={tool.name} value={tool.name} />)}
          </datalist>
          <Form.Item name="arguments" label="Tool Arguments JSON" rules={[{ required: true }]}>
            <Input.TextArea rows={8} />
          </Form.Item>
          <Form.Item name="ttlSeconds" label="TTL（秒）" rules={[{ required: true }]}>
            <InputNumber min={1} max={300} />
          </Form.Item>
          <Button type="primary" htmlType="submit" loading={issue.isPending}>签发 Approval</Button>
        </Form>
      )}
      {issue.error && <QueryFailure error={issue.error} />}
      {issue.data && visibleToken && (
        <Alert
          type="success"
          showIcon
          title="Approval Token（仅显示一次）"
          description={(
            <Space orientation="vertical">
              <Typography.Text copyable={{ text: visibleToken }} code>{visibleToken}</Typography.Text>
              <Typography.Text>过期时间：{issue.data.expiresAt}</Typography.Text>
              <Button onClick={() => setVisibleToken(undefined)}>已安全保存，隐藏 Token</Button>
            </Space>
          )}
          style={{ marginTop: 16 }}
        />
      )}
    </section>
  )
}
