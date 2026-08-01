import { useRbac3Session } from '@egon-cola/rbac3-react-sdk'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Alert, Card, Space } from 'antd'
import { useState } from 'react'
import { useFeatureApi, useFeatureTenantContext } from '../shared/FeatureApi'
import { PageState } from '../shared/PageState'
import { ControlPlaneStatusCards } from './ControlPlaneStatusCards'
import { MutationRecoveryPanel } from './MutationRecoveryPanel'
import { runtimeApi } from './runtime.api'

export const RuntimeStatusPage = () => {
  const { status: sessionStatus } = useRbac3Session()
  const { effectiveTenantId } = useFeatureTenantContext()
  const api = runtimeApi(useFeatureApi())
  const queryClient = useQueryClient()
  const [mutationStatus, setMutationStatus] = useState<string>()
  const enabled = sessionStatus === 'READY'
  const status = useQuery({ queryKey: ['rbac3', 'runtime-status'], queryFn: api.status, enabled })
  const mutationKey = ['rbac3', 'runtime-mutations', effectiveTenantId ?? 'none', mutationStatus ?? 'all']
  const mutations = useQuery({ queryKey: mutationKey, queryFn: () => api.mutations(mutationStatus), enabled })
  const retry = useMutation({
    mutationFn: async (mutationId: string) => {
      const current = await api.mutations('FAILED')
      if (!current.items.some((item) => item.mutationId === mutationId && item.status === 'FAILED')) throw new Error('MUTATION_STATE_CHANGED')
      return api.retryMutation(mutationId)
    },
    onSuccess: async () => queryClient.invalidateQueries({ queryKey: ['rbac3', 'runtime-mutations'] }),
  })
  return (
    <Card title="RBAC3 运行状态">
      <Alert type="info" showIcon message="Definition、DDC Lease 与 Gateway Release 是独立事实；任一缺失都不能由其他绿色状态替代。" />
      <PageState loading={status.isPending || mutations.isPending} error={status.error ?? mutations.error ?? retry.error} empty={!status.data}>
        {status.data && (
          <Space direction="vertical" size="large" style={{ width: '100%', marginTop: 16 }}>
            <ControlPlaneStatusCards status={status.data} />
            <MutationRecoveryPanel mutations={mutations.data?.items ?? []} status={mutationStatus} retrying={retry.isPending} onStatusChange={setMutationStatus} onRetry={retry.mutate} />
          </Space>
        )}
      </PageState>
    </Card>
  )
}
