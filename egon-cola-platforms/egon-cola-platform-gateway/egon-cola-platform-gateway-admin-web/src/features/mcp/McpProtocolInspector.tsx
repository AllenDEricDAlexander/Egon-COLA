import { useMutation } from '@tanstack/react-query'
import { Alert, Button, Input, Select, Space, Tag, Typography } from 'antd'
import { useMemo, useState } from 'react'
import { gatewayApi } from '../../api/gatewayApi'
import type { McpProtocolDialect } from '../../api/types'
import { useCapability } from '../../app/capabilities'
import { QueryFailure } from '../../components/QueryState'
import { formatJson, parseJsonObject, sanitizeInspection } from './mcpValidation'

const labels: Record<McpProtocolDialect, string> = {
  STABLE_2025_11_25: 'Stable 2025-11-25',
  RC_2026_07_28: 'RC 2026-07-28（候选协议）',
  LEGACY_2024_SSE: 'Legacy 2024 SSE',
}

const paramsTemplate = (dialect: McpProtocolDialect, method: string): Record<string, unknown> => {
  if (method === 'initialize' || method === 'discover') {
    return {
      protocolVersion: dialect === 'RC_2026_07_28' ? '2026-07-28' : '2025-11-25',
      capabilities: {},
      clientInfo: { name: 'gateway-admin-inspector', version: '1.0.0' },
    }
  }
  if (method === 'tools/call') return { name: 'tool_name', arguments: {} }
  return {}
}

export const McpProtocolInspector = ({ serverId, dialects }: {
  serverId: string
  dialects: McpProtocolDialect[]
}) => {
  const canTest = useCapability('gateway:mcp:test')
  const initialDialect = dialects[0] ?? 'STABLE_2025_11_25'
  const [dialect, setDialect] = useState<McpProtocolDialect>(initialDialect)
  const [method, setMethod] = useState('initialize')
  const [params, setParams] = useState(formatJson(paramsTemplate(initialDialect, 'initialize')))
  const inspect = useMutation({
    mutationFn: () => gatewayApi.inspectMcpProtocol(serverId, {
      dialect,
      method,
      params: parseJsonObject(params, 'Params'),
    }),
  })
  const result = useMemo(
    () => inspect.data ? sanitizeInspection(inspect.data) : undefined,
    [inspect.data],
  )

  const changeTemplate = (nextDialect: McpProtocolDialect, nextMethod: string) => {
    setDialect(nextDialect)
    setMethod(nextMethod)
    setParams(formatJson(paramsTemplate(nextDialect, nextMethod)))
    inspect.reset()
  }

  return (
    <section>
      <Alert
        type="info"
        showIcon
        title="协议检查器只生成请求模板，不会调用业务 Provider"
        description="输出会自动遮蔽 Authorization、Cookie、Token、Secret 和 Password 字段。"
        style={{ marginBottom: 16 }}
      />
      <Space wrap style={{ marginBottom: 16 }}>
        {dialects.map((item) => (
          <Tag key={item} color={item === 'RC_2026_07_28' ? 'orange' : 'blue'}>
            {labels[item]}
          </Tag>
        ))}
      </Space>
      <Space orientation="vertical" size="middle" style={{ width: '100%' }}>
        <Select<McpProtocolDialect>
          aria-label="协议版本"
          value={dialect}
          style={{ width: 300 }}
          options={dialects.map((value) => ({ value, label: labels[value] }))}
          onChange={(value) => changeTemplate(value, method)}
        />
        <Select
          aria-label="MCP Method"
          value={method}
          style={{ width: 300 }}
          options={[
            { value: 'initialize', label: 'initialize' },
            { value: 'discover', label: 'discover (RC)' },
            { value: 'tools/list', label: 'tools/list' },
            { value: 'tools/call', label: 'tools/call' },
            { value: 'resources/list', label: 'resources/list' },
            { value: 'prompts/list', label: 'prompts/list' },
            { value: 'ping', label: 'ping' },
          ]}
          onChange={(value) => changeTemplate(dialect, value)}
        />
        <Input.TextArea
          aria-label="Params JSON"
          value={params}
          rows={10}
          onChange={(event) => setParams(event.target.value)}
        />
        <Button
          type="primary"
          disabled={!canTest}
          loading={inspect.isPending}
          onClick={() => inspect.mutate()}
        >
          生成请求
        </Button>
        {inspect.error && <QueryFailure error={inspect.error} />}
        {result && (
          <div>
            <Space>
              <Typography.Title level={4}>Request Preview</Typography.Title>
              {result.releaseCandidate && <Tag color="orange">RC</Tag>}
            </Space>
            <Typography.Paragraph copyable={{ text: result.path }}>
              <Typography.Text code>{result.path}</Typography.Text>
            </Typography.Paragraph>
            <pre className="json-panel">{formatJson({
              headers: result.headers,
              body: result.body,
            })}</pre>
          </div>
        )}
      </Space>
    </section>
  )
}
