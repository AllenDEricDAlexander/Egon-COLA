import { Card, Table } from 'antd'
import { useQuery } from '@tanstack/react-query'
import { httpClient } from '../../auth/AuthContext'
import { PageState } from '@egon-cola/admin-web-shared'
import type { SigningKeyView } from '../../api/types'

export const SigningKeyPage = () => {
  const query = useQuery({
    queryKey: ['idp', 'keys'],
    queryFn: () => httpClient.request<SigningKeyView[]>('/api/v1/identity/signing-keys'),
  })

  return (
    <Card title="签名密钥（私钥永不返回浏览器）">
      <PageState loading={query.isPending} error={query.error} empty={query.data?.length === 0} onRetry={() => { void query.refetch() }}>
        <Table<SigningKeyView> rowKey="kid" dataSource={query.data ?? []} columns={[
          { title: 'KID', dataIndex: 'kid' },
          { title: '算法', dataIndex: 'algorithm' },
          { title: '状态', dataIndex: 'status' },
          { title: '当前服务', dataIndex: 'runtimeServing', render: (v: boolean) => v ? '是' : '否' },
          { title: '版本', dataIndex: 'version' },
        ]} />
      </PageState>
    </Card>
  )
}
