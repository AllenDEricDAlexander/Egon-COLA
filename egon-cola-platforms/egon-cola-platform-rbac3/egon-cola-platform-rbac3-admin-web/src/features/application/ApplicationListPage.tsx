import {useQuery} from '@tanstack/react-query'
import {Card, Table, Tag} from 'antd'
import {useRbac3Authorization} from '@egon-cola/rbac3-react-sdk'
import {useFeatureApi, useFeatureTenantContext} from '../shared/FeatureApi'
import {PageState} from '@egon-cola/admin-web-shared'
import {applicationApi, type ApplicationView} from './application.api'

export const ApplicationListPage = () => {
    const {status} = useRbac3Authorization()
  const { effectiveTenantId } = useFeatureTenantContext()
  const api = applicationApi(useFeatureApi())
  const query = useQuery({
    queryKey: ['rbac3', 'applications', effectiveTenantId ?? 'none'],
    queryFn: api.applications,
    enabled: status === 'READY',
  })
  return (
    <Card title="应用管理">
      <PageState loading={query.isPending} error={query.error} empty={query.data?.length === 0}>
        <Table<ApplicationView>
          rowKey="applicationId"
          dataSource={query.data ?? []}
          pagination={false}
          columns={[
            { title: '应用编码', dataIndex: 'applicationCode' },
            { title: '应用名称', dataIndex: 'applicationName' },
            { title: '状态', dataIndex: 'status', render: (status: string) => <Tag>{status}</Tag> },
            { title: '版本', dataIndex: 'version' },
          ]}
        />
      </PageState>
    </Card>
  )
}
