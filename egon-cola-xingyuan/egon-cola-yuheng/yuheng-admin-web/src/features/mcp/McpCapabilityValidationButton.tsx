import {useMutation} from '@tanstack/react-query'
import {Alert, Button, Space, Typography} from 'antd'
import {gatewayApi} from '../../api/gatewayApi'
import type {McpCapabilityPlural} from '../../api/types'
import {useCapability} from '../../app/capabilities'

export interface McpCapabilityValidationButtonProps {
  readonly plural: McpCapabilityPlural
  readonly capabilityId: string
  readonly gatewayGroupId: string
}

export const McpCapabilityValidationButton = ({
  plural,
  capabilityId,
  gatewayGroupId,
}: McpCapabilityValidationButtonProps) => {
  const canTest = useCapability('yuheng:mcp:test')
  const validation = useMutation({
    mutationFn: () => gatewayApi.validateMcpCapability(plural, capabilityId, gatewayGroupId),
  })
  const disabled = !canTest || !capabilityId.trim() || !gatewayGroupId.trim()

  return (
    <Space orientation="vertical" size="small">
      <Button
        size="small"
        disabled={disabled || validation.isPending}
        loading={validation.isPending}
        onClick={() => validation.mutate()}
      >
        校验
      </Button>
      {validation.error && (
        <Alert
          type="error"
          showIcon
          title={validation.error instanceof Error ? validation.error.message : 'MCP 能力校验失败'}
        />
      )}
      {validation.data && (
        <Alert
          type={validation.data.valid ? 'success' : 'error'}
          showIcon
          title={validation.data.valid ? '校验通过' : '校验失败'}
          description={validation.data.findings.length === 0 ? undefined : (
            <Space orientation="vertical" size={0}>
              {validation.data.findings.map((finding) => (
                <Typography.Text key={`${finding.code}:${finding.path}`}>
                  {finding.code} · {finding.path} · {finding.message}
                </Typography.Text>
              ))}
            </Space>
          )}
        />
      )}
    </Space>
  )
}
