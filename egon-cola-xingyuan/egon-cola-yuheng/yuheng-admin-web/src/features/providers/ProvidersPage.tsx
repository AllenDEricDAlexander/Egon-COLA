import { useQuery } from '@tanstack/react-query'
import { Alert, Table, Tag, Typography } from 'antd'
import { useSearchParams } from 'react-router-dom'
import { gatewayApi } from '../../api/gatewayApi'
import type { ProviderInstance, Scope } from '../../api/types'
import { EmptyBlock, LoadingBlock, QueryFailure } from '../../components/QueryState'
import { StatusTag } from '../../components/StatusTag'
import { GatewayScopeFilter } from '../../components/GatewayScopeFilter'
import { hasRequiredScopeFields, readScopeSearchParams, writeScopeSearchParams } from '../../hooks/scopeSearchParams'

export const ProvidersPage = () => {
  const [searchParams, setSearchParams] = useSearchParams()
  const scope = readScopeSearchParams(searchParams, ['bizCode', 'namespace', 'env', 'appCode'])
  const requestScope = hasRequiredScopeFields(scope, ['bizCode', 'namespace', 'env', 'appCode'])
    ? scope as Scope
    : undefined
  const query = useQuery({
    queryKey: ['providers', requestScope],
    queryFn: ({ signal }) => gatewayApi.providers(requestScope!, signal),
    enabled: Boolean(requestScope),
    refetchInterval: requestScope ? 10_000 : false,
  })
  if (query.isLoading) return <LoadingBlock />
  if (!requestScope) return (
    <section>
      <Typography.Title level={2}>Provider 管理投影</Typography.Title>
      <GatewayScopeFilter
        fields={['bizCode', 'namespace', 'env', 'appCode']}
        value={scope}
        required
        onChange={(value) => setSearchParams(
          writeScopeSearchParams(searchParams, value, ['bizCode', 'namespace', 'env', 'appCode']),
        )}
      />
      <EmptyBlock description="请选择完整的 Biz / Namespace / Env / App 查询范围" />
    </section>
  )
  if (query.error) return <QueryFailure error={query.error} />
  return (
    <section>
      <Typography.Title level={2}>Provider 管理投影</Typography.Title>
      <GatewayScopeFilter
        fields={['bizCode', 'namespace', 'env', 'appCode']}
        value={scope}
        required
        onChange={(value) => setSearchParams(
          writeScopeSearchParams(searchParams, value, ['bizCode', 'namespace', 'env', 'appCode']),
        )}
      />
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
