import { useQuery } from '@tanstack/react-query'
import { Alert, Table, Tag, Typography } from 'antd'
import { gatewayApi } from '../../api/gatewayApi'
import type { ProviderInstance } from '../../api/types'
import { LoadingBlock, QueryFailure } from '../../components/QueryState'
import { StatusTag } from '../../components/StatusTag'
import { useScope } from '../../hooks/useScope'

export const ProvidersPage = () => {
  const { scope } = useScope()
  const query = useQuery({
    queryKey: ['providers', scope],
    queryFn: ({ signal }) => gatewayApi.providers(scope, signal),
    refetchInterval: 10_000,
  })
  if (query.isLoading) return <LoadingBlock />
  if (query.error) return <QueryFailure error={query.error} />
  return (
    <section>
      <Typography.Title level={2}>Provider 管理投影</Typography.Title>
      <Alert
        showIcon
        type="info"
        message="这里展示 DDC 注册与 Engine 健康投影，不是静态路由配置。Provider 地址不能写入 Route。"
      />
      <Table<ProviderInstance>
        className="section-row"
        rowKey={(row) => `${row.serviceKey}:${row.instanceId}:${row.leaseId}`}
        dataSource={query.data ?? []}
        scroll={{ x: 1450 }}
        columns={[
          { title: 'Protocol', render: (_, row) => <Tag>{row.protocol}</Tag> },
          { title: 'Service', dataIndex: 'serviceName' },
          { title: 'Group', dataIndex: 'group' },
          { title: 'Version', dataIndex: 'version' },
          { title: 'Instance', dataIndex: 'instanceId' },
          { title: 'Lease', dataIndex: 'leaseId' },
          { title: 'Host:Port', render: (_, row) => `${row.host}:${row.port}` },
          { title: 'Region/Zone', render: (_, row) => `${row.region ?? '-'} / ${row.zone ?? '-'}` },
          { title: 'Weight', dataIndex: 'weight' },
          { title: 'Definition Set', dataIndex: 'definitionSetId' },
          { title: 'Status', render: (_, row) => <StatusTag status={row.status} /> },
          { title: 'Expire At', dataIndex: 'expireAt' },
          { title: 'Observed At', dataIndex: 'observedAt' },
          { title: 'Stale', render: (_, row) => <StatusTag status={row.stale ? 'STALE' : 'FRESH'} /> },
        ]}
      />
    </section>
  )
}
