import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { message } from 'antd'
import { gatewayApi } from '../../api/gatewayApi'
import type {
  McpCapabilityDraft,
  McpCapabilityMutation,
  McpCapabilityPlural,
} from '../../api/types'

export type McpCapabilitySave = {
  editing?: McpCapabilityDraft
  name: string
  content: Record<string, unknown>
  enabled: boolean
  changeReason: string
}

export const useMcpCapabilityCollection = (
  plural: McpCapabilityPlural,
  serverId: string,
  gatewayGroupId: string,
  draftRevision: number,
) => {
  const queryClient = useQueryClient()
  const queryKey = ['mcp-capabilities', gatewayGroupId, serverId, plural] as const
  const query = useQuery({
    queryKey,
    queryFn: ({ signal }) => gatewayApi.mcpCapabilities(
      gatewayGroupId,
      serverId,
      plural,
      signal,
    ),
  })
  const save = useMutation({
    mutationFn: ({ editing, name, content, enabled, changeReason }: McpCapabilitySave) => {
      const capability: McpCapabilityMutation = {
        gatewayGroupId,
        serverId,
        name,
        content,
        enabled,
        expectedRevision: editing?.revision ?? 0,
        expectedDraftRevision: draftRevision,
        changeReason,
      }
      return editing
        ? gatewayApi.updateMcpCapability(plural, editing.id, capability)
        : gatewayApi.createMcpCapability(plural, capability)
    },
    onSuccess: async () => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey }),
        queryClient.invalidateQueries({ queryKey: ['gateway-draft', gatewayGroupId] }),
        queryClient.invalidateQueries({ queryKey: ['mcp-capability-preview', serverId] }),
      ])
      void message.success('MCP Capability 已保存')
    },
  })
  const remove = useMutation({
    mutationFn: (capability: McpCapabilityDraft) => gatewayApi.deleteMcpCapability(
      plural,
      capability.id,
      {
        gatewayGroupId,
        expectedRevision: capability.revision,
        expectedDraftRevision: draftRevision,
        changeReason: `Delete MCP ${capability.kind} from Admin Web`,
      },
    ),
    onSuccess: async () => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey }),
        queryClient.invalidateQueries({ queryKey: ['gateway-draft', gatewayGroupId] }),
        queryClient.invalidateQueries({ queryKey: ['mcp-capability-preview', serverId] }),
      ])
      void message.success('MCP Capability 已删除')
    },
  })
  return { query, save, remove }
}
