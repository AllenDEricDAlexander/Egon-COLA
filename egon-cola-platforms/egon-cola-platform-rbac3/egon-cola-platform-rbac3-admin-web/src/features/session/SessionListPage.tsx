import { PermissionGuard, useRbac3Session } from '@egon-cola/rbac3-react-sdk'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Button, Card, Popconfirm, Table, Tag, Typography } from 'antd'
import { useFeatureApi, useFeatureTenantContext } from '../shared/FeatureApi'
import { PageState } from '../shared/PageState'
import { sessionApi, type SessionView } from './session.api'

export const SessionListPage = () => {
  const { status, bootstrap } = useRbac3Session()
  const { effectiveTenantId } = useFeatureTenantContext()
  const api = sessionApi(useFeatureApi())
  const queryClient = useQueryClient()
  const queryKey = ['rbac3', 'sessions', effectiveTenantId ?? 'none', bootstrap?.user.id ?? 'none']
  const query = useQuery({ queryKey, queryFn: api.mine, enabled: status === 'READY' })
  const revoke = useMutation({
    mutationFn: api.revoke,
    onSuccess: async () => queryClient.invalidateQueries({ queryKey }),
  })
  return (
    <Card title="我的会话">
      <Typography.Paragraph type="secondary">
        仅展示会话状态与版本元数据；凭据、令牌派生值及完整授权快照不会进入页面。
      </Typography.Paragraph>
      <PageState loading={query.isPending} error={query.error ?? revoke.error} empty={query.data?.length === 0}>
        <Table<SessionView>
          rowKey="sessionId"
          dataSource={query.data ?? []}
          pagination={false}
          columns={[
            { title: 'Session ID', dataIndex: 'sessionId' },
            { title: '状态', dataIndex: 'status', render: (value: string) => <Tag>{value}</Tag> },
            { title: 'Session Version', dataIndex: 'sessionVersion' },
            { title: '认证时间', dataIndex: 'authenticatedAt' },
            { title: '最近访问', dataIndex: 'lastSeenAt' },
            { title: '绝对过期时间', dataIndex: 'absoluteExpiresAt' },
            {
              title: '操作', render: (_value, session) => session.status === 'ACTIVE' && (
                <PermissionGuard permission="system:session:revoke">
                  <Popconfirm title="确认撤销此会话及其刷新令牌族？" onConfirm={() => revoke.mutate(session.sessionId)}>
                    <Button danger size="small">撤销会话</Button>
                  </Popconfirm>
                </PermissionGuard>
              ),
            },
          ]}
        />
      </PageState>
    </Card>
  )
}
