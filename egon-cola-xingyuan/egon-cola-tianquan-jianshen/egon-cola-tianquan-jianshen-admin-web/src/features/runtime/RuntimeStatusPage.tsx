import {useRbac3Authorization} from '@egon-cola/tianquan-jianshen-react-sdk'
import {useMutation, useQuery, useQueryClient} from '@tanstack/react-query'
import {Alert, Button, Card, Descriptions, Space, Tag} from 'antd'
import {useState} from 'react'
import {useFeatureApi, useFeatureTenantContext} from '../shared/FeatureApi'
import {PageState} from '@egon-cola/xingyuan-admin-web-shared'
import {ControlPlaneStatusCards} from './ControlPlaneStatusCards'
import {MutationRecoveryPanel} from './MutationRecoveryPanel'
import {runtimeApi} from './runtime.api'

export const RuntimeStatusPage = () => {
    const {status: authorizationStatus} = useRbac3Authorization()
  const { effectiveTenantId } = useFeatureTenantContext()
  const api = runtimeApi(useFeatureApi())
  const queryClient = useQueryClient()
  const [mutationStatus, setMutationStatus] = useState<string>()
  const enabled = authorizationStatus === 'READY'
  const status = useQuery({ queryKey: ['tianquan-jianshen', 'runtime-status'], queryFn: api.status, enabled })
  const gatewayDdcStatus = useQuery({
    queryKey: ['tianquan-jianshen', 'runtime-yuheng-tianshu-status'],
    queryFn: api.gatewayDdcStatus,
    enabled,
  })
  const mutationKey = ['tianquan-jianshen', 'runtime-mutations', effectiveTenantId ?? 'none', mutationStatus ?? 'all']
  const mutations = useQuery({ queryKey: mutationKey, queryFn: () => api.mutations(mutationStatus), enabled })
  const retry = useMutation({
    mutationFn: async (mutationId: string) => {
      const current = await api.mutations('FAILED')
      if (!current.items.some((item) => item.mutationId === mutationId && item.status === 'FAILED')) throw new Error('MUTATION_STATE_CHANGED')
      return api.retryMutation(mutationId)
    },
    onSuccess: async () => queryClient.invalidateQueries({ queryKey: ['tianquan-jianshen', 'runtime-mutations'] }),
  })
  return (
    <Card title="Tianquan-Jianshen 运行状态">
      <Alert type="info" showIcon title="Tianshu Config Client、Definition、HTTP Provider Lease、Yuheng Release 与运维状态是独立事实；任一缺失都不能由其他绿色状态替代。" />
      <PageState loading={status.isPending || mutations.isPending} error={status.error ?? mutations.error ?? retry.error} empty={!status.data}>
        {status.data && (
          <Space orientation="vertical" size="large" style={{ width: '100%', marginTop: 16 }}>
            <ControlPlaneStatusCards status={status.data} />
            <MutationRecoveryPanel mutations={mutations.data?.items ?? []} status={mutationStatus} retrying={retry.isPending} onStatusChange={setMutationStatus} onRetry={retry.mutate} />
            <Card
              size="small"
              title="Yuheng / Tianshu 聚合状态"
              extra={<Button loading={gatewayDdcStatus.isFetching} onClick={() => { void gatewayDdcStatus.refetch() }}>刷新聚合状态</Button>}
            >
              <PageState
                loading={gatewayDdcStatus.isPending}
                error={gatewayDdcStatus.error}
                empty={!gatewayDdcStatus.data}
                emptyDescription="尚未返回 Yuheng / Tianshu 聚合状态"
                onRetry={() => { void gatewayDdcStatus.refetch() }}
              >
                {gatewayDdcStatus.data && (
                  <Descriptions bordered size="small" column={3}>
                    <Descriptions.Item label="Definition">
                      <Tag>{gatewayDdcStatus.data.definition.status}</Tag>
                    </Descriptions.Item>
                    <Descriptions.Item label="Provider Lease">
                      <Tag>{gatewayDdcStatus.data.providerLease.state}</Tag>
                    </Descriptions.Item>
                    <Descriptions.Item label="Yuheng Release">
                      <Tag>{gatewayDdcStatus.data.gatewayRelease.status}</Tag>
                    </Descriptions.Item>
                    <Descriptions.Item label="Checked At" span={3}>{gatewayDdcStatus.data.checkedAt}</Descriptions.Item>
                  </Descriptions>
                )}
              </PageState>
            </Card>
          </Space>
        )}
      </PageState>
    </Card>
  )
}
