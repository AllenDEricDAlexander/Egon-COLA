import { useQuery } from '@tanstack/react-query'
import { Button, Table, Typography } from 'antd'
import { useNavigate } from 'react-router-dom'
import { gatewayApi } from '../../api/gatewayApi'
import { LoadingBlock, QueryFailure } from '../../components/QueryState'
import { StatusTag } from '../../components/StatusTag'
import { useScope } from '../../hooks/useScope'

export const GatewayGroupsPage = () => {
  const { scope } = useScope()
  const navigate = useNavigate()
  const query = useQuery({
    queryKey: ['gateway-groups', scope],
    queryFn: ({ signal }) => gatewayApi.groups(scope, signal),
  })
  if (query.isLoading) return <LoadingBlock />
  if (query.error) return <QueryFailure error={query.error} retry={() => void query.refetch()} />
  return (
    <section>
      <Typography.Title level={2}>Gateway Group</Typography.Title>
      <Table
        rowKey="id"
        dataSource={query.data ?? []}
        scroll={{ x: 900 }}
        columns={[
          { title: 'Code', dataIndex: 'gatewayGroupCode' },
          { title: '名称', dataIndex: 'displayName' },
          { title: 'Env', dataIndex: 'env' },
          { title: 'Namespace', dataIndex: 'namespace' },
          {
            title: '状态',
            render: (_, record) => <StatusTag status={record.enabled ? 'ACTIVE' : 'DISABLED'} />,
          },
          { title: 'Revision', dataIndex: 'revision' },
          {
            title: '操作',
            render: (_, record) => (
              <Button type="link" onClick={() => navigate(`/gateway-groups/${record.id}/overview`)}>
                查看
              </Button>
            ),
          },
        ]}
      />
    </section>
  )
}
