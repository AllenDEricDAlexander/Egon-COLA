import {PermissionGuard, useRbac3Authorization} from '@egon-cola/rbac3-react-sdk'
import {useMutation, useQuery, useQueryClient} from '@tanstack/react-query'
import {Alert, Button, Card, Popconfirm, Space, Table, Tag, Typography} from 'antd'
import {useState} from 'react'
import {useFeatureApi, useFeatureTenantContext} from '../shared/FeatureApi'
import {PageState} from '@egon-cola/admin-web-shared'
import {AssignmentEditor} from './AssignmentEditor'
import {
    assignmentApi,
    type AssignmentOperation,
    type AssignmentView,
    type CreateAssignmentCommand
} from './assignment.api'

export interface AssignmentListPageProps {
  readonly userId: string
}

export const AssignmentListPage = ({ userId }: AssignmentListPageProps) => {
    const {status, bootstrap} = useRbac3Authorization()
  const { effectiveTenantId } = useFeatureTenantContext()
  const api = assignmentApi(useFeatureApi())
  const queryClient = useQueryClient()
  const [editorOpen, setEditorOpen] = useState(false)
  const queryKey = ['rbac3', 'assignments', effectiveTenantId ?? 'none', userId]
  const query = useQuery({ queryKey, queryFn: () => api.list(userId), enabled: status === 'READY' })
  const create = useMutation({
    mutationFn: (command: CreateAssignmentCommand) => api.create(userId, command, crypto.randomUUID()),
    onSuccess: async () => { setEditorOpen(false); await queryClient.invalidateQueries({ queryKey }) },
  })
  const change = useMutation({
    mutationFn: ({ assignment, operation }: { assignment: AssignmentView; operation: AssignmentOperation }) => api.change(
      userId,
      assignment.assignmentId,
      operation,
      {
        reason: 'console state change',
        ticketNo: null,
        expectedAssignmentVersion: assignment.version,
        expectedUserAuthVersion: bootstrap?.authVersion ?? 0,
      },
      crypto.randomUUID(),
    ),
    onSuccess: async () => queryClient.invalidateQueries({ queryKey }),
  })
  return (
    <Card
      title={`用户 ${userId} 的角色任职`}
      extra={(
        <PermissionGuard permission="system:role-assignment:manage">
          <Button type="primary" onClick={() => setEditorOpen(true)}>新增任职资格</Button>
        </PermissionGuard>
      )}
    >
      <Alert type="info" showIcon message="任职只定义授权资格与生效窗口；业务系统根据自身语义调用激活角色接口。" />
      <Typography.Paragraph type="secondary" style={{ marginTop: 12 }}>
        列表保留暂停、恢复、撤销后的历史状态；所有写操作使用幂等键和版本前置条件。
      </Typography.Paragraph>
      <PageState loading={query.isPending} error={query.error ?? create.error ?? change.error} empty={query.data?.length === 0}>
        <Table<AssignmentView>
          rowKey="assignmentId"
          dataSource={query.data ?? []}
          pagination={false}
          columns={[
            { title: 'Assignment ID', dataIndex: 'assignmentId' },
            { title: 'Role ID', dataIndex: 'roleId' },
            { title: '类型', dataIndex: 'assignmentType' },
            { title: '状态', dataIndex: 'status', render: (value: string) => <Tag>{value}</Tag> },
            { title: '生效时间', dataIndex: 'validFrom' },
            { title: '失效时间', dataIndex: 'validTo', render: (value: string | null) => value ?? '长期' },
            {
              title: '操作',
              render: (_value, assignment) => (
                <PermissionGuard permission="system:role-assignment:manage">
                  <Space>
                    {assignment.status === 'ACTIVE' && <ChangeButton label="暂停" operation="suspend" assignment={assignment} onChange={change.mutate} />}
                    {assignment.status === 'SUSPENDED' && <ChangeButton label="恢复" operation="resume" assignment={assignment} onChange={change.mutate} />}
                    {!['REVOKED', 'EXPIRED'].includes(assignment.status) && <ChangeButton label="撤销" operation="revoke" assignment={assignment} onChange={change.mutate} danger />}
                  </Space>
                </PermissionGuard>
              ),
            },
          ]}
        />
      </PageState>
      <AssignmentEditor open={editorOpen} saving={create.isPending} onCancel={() => setEditorOpen(false)} onSave={create.mutate} />
    </Card>
  )
}

interface ChangeButtonProps {
  readonly label: string
  readonly operation: AssignmentOperation
  readonly assignment: AssignmentView
  readonly danger?: boolean
  readonly onChange: (value: { assignment: AssignmentView; operation: AssignmentOperation }) => void
}

const ChangeButton = ({ label, operation, assignment, danger, onChange }: ChangeButtonProps) => (
  <Popconfirm title={`确认${label}此任职资格？`} onConfirm={() => onChange({ assignment, operation })}>
    <Button size="small" danger={danger}>{label}</Button>
  </Popconfirm>
)
