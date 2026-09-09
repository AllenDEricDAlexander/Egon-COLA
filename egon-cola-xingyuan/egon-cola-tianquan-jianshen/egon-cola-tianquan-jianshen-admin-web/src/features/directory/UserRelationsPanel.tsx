import {PermissionGuard, useRbac3Authorization} from '@egon-cola/tianquan-jianshen-react-sdk'
import {useMutation, useQuery, useQueryClient} from '@tanstack/react-query'
import {Alert, Button, Card, Checkbox, Form, Input, Popconfirm, Space, Table, Tabs, Tag, Typography} from 'antd'
import type {ReactNode} from 'react'
import {Link} from 'react-router-dom'
import {PageState} from '@egon-cola/xingyuan-admin-web-shared'
import {useFeatureApi, useFeatureTenantContext} from '../shared/FeatureApi'
import {
  directoryApi,
  type OrganizationAssignmentCommand,
  type PositionAssignmentCommand,
  type UserOrganizationAssignmentView,
  type UserPositionAssignmentView,
} from './directory.api'

export interface UserRelationsPanelProps {
  readonly userId: string
  readonly authVersion: number
}

interface OrganizationFormValues {
  readonly orgUnitId: string
  readonly validFrom: string
  readonly validTo?: string
  readonly reason?: string
  readonly ticketNo?: string
}

interface PositionFormValues extends OrganizationFormValues {
  readonly positionId: string
  readonly primaryAssignment?: boolean
}

export const UserRelationsPanel = ({userId, authVersion}: UserRelationsPanelProps) => {
  const {status} = useRbac3Authorization()
  const {effectiveTenantId} = useFeatureTenantContext()
  const api = directoryApi(useFeatureApi())
  const queryClient = useQueryClient()
  const normalizedUserId = userId.trim()
  const tenant = effectiveTenantId ?? 'none'
  const organizationKey = ['rbac3', 'user-organization-assignments', tenant, normalizedUserId]
  const positionKey = ['rbac3', 'user-position-assignments', tenant, normalizedUserId]
  const [organizationForm] = Form.useForm<OrganizationFormValues>()
  const [positionForm] = Form.useForm<PositionFormValues>()

  const organizations = useQuery({
    queryKey: organizationKey,
    queryFn: () => api.organizationAssignments(normalizedUserId),
    enabled: status === 'READY' && normalizedUserId.length > 0,
  })
  const positions = useQuery({
    queryKey: positionKey,
    queryFn: () => api.positionAssignments(normalizedUserId),
    enabled: status === 'READY' && normalizedUserId.length > 0,
  })
  const assignOrganization = useMutation({
    mutationFn: (values: OrganizationFormValues) => {
      const command: OrganizationAssignmentCommand = {
        orgUnitId: values.orgUnitId,
        validFrom: values.validFrom,
        validTo: values.validTo || null,
        reason: values.reason ?? '',
        ticketNo: values.ticketNo ?? '',
      }
      return api.assignOrganization(normalizedUserId, command)
    },
    onSuccess: async () => {
      organizationForm.resetFields()
      await queryClient.invalidateQueries({queryKey: organizationKey})
    },
  })
  const revokeOrganization = useMutation({
    mutationFn: (assignment: UserOrganizationAssignmentView) => api.revokeOrganization(
      normalizedUserId,
      assignment.assignmentId,
      assignment.version,
    ),
    onSuccess: async () => queryClient.invalidateQueries({queryKey: organizationKey}),
  })
  const assignPosition = useMutation({
    mutationFn: (values: PositionFormValues) => {
      const command: PositionAssignmentCommand = {
        orgUnitId: values.orgUnitId,
        positionId: values.positionId,
        primaryAssignment: values.primaryAssignment ?? false,
        validFrom: values.validFrom,
        validTo: values.validTo || null,
        reason: values.reason ?? '',
        ticketNo: values.ticketNo ?? '',
      }
      return api.assignPosition(normalizedUserId, command)
    },
    onSuccess: async () => {
      positionForm.resetFields()
      await queryClient.invalidateQueries({queryKey: positionKey})
    },
  })
  const revokePosition = useMutation({
    mutationFn: (assignment: UserPositionAssignmentView) => api.revokePosition(
      normalizedUserId,
      assignment.assignmentId,
      assignment.version,
    ),
    onSuccess: async () => queryClient.invalidateQueries({queryKey: positionKey}),
  })

  const organizationError = assignOrganization.error ?? revokeOrganization.error
  const positionError = assignPosition.error ?? revokePosition.error

  return (
    <Card
      title="用户关系"
      style={{marginTop: 16}}
      extra={<Typography.Text type="secondary">Auth Version {authVersion}</Typography.Text>}
    >
      <Tabs
        animated={false}
        items={[
          {
            key: 'organizations',
            label: '组织关系',
            children: (
              <RelationSection
                error={organizationError}
                form={(
                  <Form
                    form={organizationForm}
                    layout="vertical"
                    onFinish={(values) => assignOrganization.mutate(values)}
                    initialValues={{validTo: ''}}
                  >
                    <Space wrap align="start">
                      <Form.Item name="orgUnitId" label="组织 ID" rules={[{required: true, whitespace: true}]}>
                        <Input placeholder="例如 1001" />
                      </Form.Item>
                      <Form.Item name="validFrom" label="生效时间" rules={[{required: true, whitespace: true}]}>
                        <Input placeholder="2026-08-29T00:00:00Z" />
                      </Form.Item>
                      <Form.Item name="validTo" label="失效时间">
                        <Input placeholder="留空表示长期有效" />
                      </Form.Item>
                      <Form.Item name="reason" label="原因">
                        <Input />
                      </Form.Item>
                      <Form.Item name="ticketNo" label="外部工单号">
                        <Input />
                      </Form.Item>
                    </Space>
                    <PermissionGuard permission="system:user-organization:manage">
                      <Button type="primary" htmlType="submit" loading={assignOrganization.isPending}>
                        保存组织关系
                      </Button>
                    </PermissionGuard>
                  </Form>
                )}
                query={organizations}
                emptyDescription="该用户暂无组织关系"
              >
                <Table<UserOrganizationAssignmentView>
                  rowKey="assignmentId"
                  dataSource={organizations.data ?? []}
                  pagination={false}
                  scroll={{x: 'max-content'}}
                  columns={[
                    {title: '关系 ID', dataIndex: 'assignmentId'},
                    {title: '组织 ID', dataIndex: 'orgUnitId'},
                    {title: '状态', dataIndex: 'status', render: (value: string) => <Tag>{value}</Tag>},
                    {title: '来源', dataIndex: 'sourceType'},
                    {title: '版本', dataIndex: 'version'},
                    {
                      title: '操作',
                      render: (_value: unknown, assignment: UserOrganizationAssignmentView) => (
                        <OrganizationRevokeAction
                          assignment={assignment}
                          loading={revokeOrganization.isPending && revokeOrganization.variables?.assignmentId === assignment.assignmentId}
                          onRevoke={() => revokeOrganization.mutate(assignment)}
                        />
                      ),
                    },
                  ]}
                />
              </RelationSection>
            ),
          },
          {
            key: 'positions',
            label: '岗位关系',
            children: (
              <RelationSection
                error={positionError}
                form={(
                  <Form
                    form={positionForm}
                    layout="vertical"
                    onFinish={(values) => assignPosition.mutate(values)}
                    initialValues={{primaryAssignment: false, validTo: ''}}
                  >
                    <Space wrap align="start">
                      <Form.Item name="orgUnitId" label="组织 ID（岗位）" rules={[{required: true, whitespace: true}]}>
                        <Input placeholder="例如 1001" />
                      </Form.Item>
                      <Form.Item name="positionId" label="岗位 ID" rules={[{required: true, whitespace: true}]}>
                        <Input placeholder="例如 2001" />
                      </Form.Item>
                      <Form.Item name="validFrom" label="生效时间" rules={[{required: true, whitespace: true}]}>
                        <Input placeholder="2026-08-29T00:00:00Z" />
                      </Form.Item>
                      <Form.Item name="validTo" label="失效时间">
                        <Input placeholder="留空表示长期有效" />
                      </Form.Item>
                      <Form.Item name="reason" label="原因">
                        <Input />
                      </Form.Item>
                      <Form.Item name="ticketNo" label="外部工单号">
                        <Input />
                      </Form.Item>
                      <Form.Item name="primaryAssignment" valuePropName="checked" label="主岗位">
                        <Checkbox>设为主岗位</Checkbox>
                      </Form.Item>
                    </Space>
                    <PermissionGuard permission="system:user-position:manage">
                      <Button type="primary" htmlType="submit" loading={assignPosition.isPending}>
                        保存岗位关系
                      </Button>
                    </PermissionGuard>
                  </Form>
                )}
                query={positions}
                emptyDescription="该用户暂无岗位关系"
              >
                <Table<UserPositionAssignmentView>
                  rowKey="assignmentId"
                  dataSource={positions.data ?? []}
                  pagination={false}
                  scroll={{x: 'max-content'}}
                  columns={[
                    {title: '关系 ID', dataIndex: 'assignmentId'},
                    {title: '岗位 ID', dataIndex: 'positionId'},
                    {title: '组织 ID', dataIndex: 'orgUnitId'},
                    {title: '主岗位', dataIndex: 'primaryAssignment', render: (value: boolean) => value ? '是' : '否'},
                    {title: '状态', dataIndex: 'status', render: (value: string) => <Tag>{value}</Tag>},
                    {title: '来源', dataIndex: 'sourceType'},
                    {title: '版本', dataIndex: 'version'},
                    {
                      title: '操作',
                      render: (_value: unknown, assignment: UserPositionAssignmentView) => (
                        <PositionRevokeAction
                          assignment={assignment}
                          loading={revokePosition.isPending && revokePosition.variables?.assignmentId === assignment.assignmentId}
                          onRevoke={() => revokePosition.mutate(assignment)}
                        />
                      ),
                    },
                  ]}
                />
              </RelationSection>
            ),
          },
          {
            key: 'roles',
            label: '角色任职',
            children: (
              <PermissionGuard
                permission="system:role-assignment:read"
                fallback={<Alert type="warning" showIcon title="当前账号没有角色任职读取权限" />}
              >
                <Space orientation="vertical" size="middle">
                  <Typography.Paragraph type="secondary">
                    角色任职使用独立工作台维护资格、状态和生效窗口，用户关系页只提供稳定入口。
                  </Typography.Paragraph>
                  <Link to={`/iam/users/${encodeURIComponent(normalizedUserId)}/role-assignments`}>
                    <Button type="primary">打开角色任职工作台</Button>
                  </Link>
                </Space>
              </PermissionGuard>
            ),
          },
        ]}
      />
    </Card>
  )
}

interface RelationSectionProps {
  readonly error: unknown
  readonly form: ReactNode
  readonly query: {
    readonly isPending: boolean
    readonly error: unknown
    readonly data?: readonly unknown[]
    readonly refetch: () => unknown
  }
  readonly emptyDescription: string
  readonly children: ReactNode
}

const RelationSection = ({error, form, query, emptyDescription, children}: RelationSectionProps) => (
  <Space orientation="vertical" size="middle" style={{width: '100%'}}>
    {form}
    {error ? (
      <Alert
        type="error"
        showIcon
        title={error instanceof Error ? error.message : String(error)}
        action={<Button size="small" onClick={() => {void query.refetch()}}>刷新</Button>}
      />
    ) : null}
    <PageState
      loading={query.isPending}
      error={query.error}
      empty={query.data?.length === 0}
      emptyDescription={emptyDescription}
      onRetry={() => { void query.refetch() }}
      showPartial
    >
      {children}
    </PageState>
  </Space>
)

interface OrganizationRevokeActionProps {
  readonly assignment: UserOrganizationAssignmentView
  readonly loading: boolean
  readonly onRevoke: () => void
}

const OrganizationRevokeAction = ({assignment, loading, onRevoke}: OrganizationRevokeActionProps) => {
  if (assignment.sourceType !== 'MANUAL' || !['ACTIVE', 'SUSPENDED'].includes(assignment.status)) {
    return <Tag>只读</Tag>
  }
  return (
    <PermissionGuard permission="system:user-organization:manage">
      <Popconfirm title="确认撤销组织关系？" okText="确认撤销组织关系" cancelText="取消" onConfirm={onRevoke}>
        <Button size="small" danger loading={loading}>撤销组织关系</Button>
      </Popconfirm>
    </PermissionGuard>
  )
}

interface PositionRevokeActionProps {
  readonly assignment: UserPositionAssignmentView
  readonly loading: boolean
  readonly onRevoke: () => void
}

const PositionRevokeAction = ({assignment, loading, onRevoke}: PositionRevokeActionProps) => {
  if (assignment.sourceType !== 'MANUAL' || !['ACTIVE', 'SUSPENDED'].includes(assignment.status)) {
    return <Tag>只读</Tag>
  }
  return (
    <PermissionGuard permission="system:user-position:manage">
      <Popconfirm title="确认撤销岗位关系？" okText="确认撤销岗位关系" cancelText="取消" onConfirm={onRevoke}>
        <Button size="small" danger loading={loading}>撤销岗位关系</Button>
      </Popconfirm>
    </PermissionGuard>
  )
}
