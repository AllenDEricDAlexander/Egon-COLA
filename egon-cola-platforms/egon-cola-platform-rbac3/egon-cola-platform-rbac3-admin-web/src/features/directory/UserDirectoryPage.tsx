import {useMutation, useQuery, useQueryClient} from '@tanstack/react-query'
import {PermissionGuard, useRbac3Authorization} from '@egon-cola/rbac3-react-sdk'
import {PageState} from '@egon-cola/admin-web-shared'
import {Button, Card, Descriptions, Form, Input, Modal, Popconfirm, Select, Space, Table, Tag} from 'antd'
import {useState} from 'react'
import {useFeatureApi, useFeatureTenantContext} from '../shared/FeatureApi'
import {
  directoryApi,
  type CreateUserCommand,
  type UserDirectoryFilter,
  type UserDirectoryView,
} from './directory.api'
import {UserRelationsPanel} from './UserRelationsPanel'

export interface UserDirectoryPageProps {
  readonly initialUserId?: string
}

type UserFormValues = CreateUserCommand & {
  readonly expectedAuthVersion?: number
}

const userStatuses = [
  {value: 'INVITED', label: '待邀请'},
  {value: 'ACTIVE', label: '启用'},
  {value: 'LOCKED', label: '锁定'},
  {value: 'DISABLED', label: '停用'},
  {value: 'ARCHIVED', label: '归档'},
]

const emptyFilter: UserDirectoryFilter = {
  query: '',
  status: '',
  orgUnitId: '',
  positionId: '',
  page: 0,
  size: 20,
}

const userIdentity = (user: UserDirectoryView) => user.identitySub || user.username || user.userId

export const UserDirectoryPage = ({initialUserId = ''}: UserDirectoryPageProps) => {
  const {status} = useRbac3Authorization()
  const {effectiveTenantId} = useFeatureTenantContext()
  const api = directoryApi(useFeatureApi())
  const queryClient = useQueryClient()
  const listMode = initialUserId.trim().length === 0
  const [draftFilter, setDraftFilter] = useState<UserDirectoryFilter>({...emptyFilter})
  const [submittedFilter, setSubmittedFilter] = useState<UserDirectoryFilter>({...emptyFilter})
  const [draftUserId, setDraftUserId] = useState(initialUserId)
  const [userId, setUserId] = useState(initialUserId)
  const [editingUser, setEditingUser] = useState<UserDirectoryView | null>(null)
  const [userModalOpen, setUserModalOpen] = useState(false)
  const [userForm] = Form.useForm<UserFormValues>()
  const tenant = effectiveTenantId ?? 'none'

  const detail = useQuery({
    queryKey: ['rbac3', 'directory-user', tenant, userId.trim()],
    queryFn: () => api.user(userId.trim()),
    enabled: status === 'READY' && userId.trim().length > 0,
  })
  const users = useQuery({
    queryKey: ['rbac3', 'directory-users', tenant, submittedFilter],
    queryFn: () => api.users(submittedFilter),
    enabled: status === 'READY' && listMode,
  })

  const refreshUsers = async () => {
    await Promise.all([
      queryClient.invalidateQueries({queryKey: ['rbac3', 'directory-users']}),
      queryClient.invalidateQueries({queryKey: ['rbac3', 'directory-user']}),
    ])
  }

  const saveUser = useMutation({
    mutationFn: (values: UserFormValues) => editingUser
      ? api.updateUser(editingUser.userId, {
        identitySub: values.identitySub.trim(),
        expectedAuthVersion: editingUser.authVersion,
      })
      : api.createUser({
        identitySub: values.identitySub.trim(),
        status: values.status,
      }),
    onSuccess: async (value) => {
      setUserModalOpen(false)
      setEditingUser(null)
      if (value) {
        setUserId(value.userId)
        setDraftUserId(value.userId)
      }
      await refreshUsers()
    },
  })

  const changeStatus = useMutation({
    mutationFn: (user: UserDirectoryView) => api.changeUserStatus(user.userId, {
      status: user.status === 'ACTIVE' ? 'DISABLED' : 'ACTIVE',
      reason: '管理员在用户目录中变更状态',
      expectedAuthVersion: user.authVersion,
    }),
    onSuccess: refreshUsers,
  })

  const deleteUser = useMutation({
    mutationFn: (user: UserDirectoryView) => api.deleteUser(user.userId, user.authVersion),
    onSuccess: async () => {
      setUserId('')
      setDraftUserId('')
      await Promise.all([
        queryClient.invalidateQueries({queryKey: ['rbac3', 'user-organization-assignments']}),
        queryClient.invalidateQueries({queryKey: ['rbac3', 'user-position-assignments']}),
      ])
      await refreshUsers()
    },
  })

  const openCreate = () => {
    setEditingUser(null)
    userForm.resetFields()
    userForm.setFieldsValue({identitySub: '', status: 'ACTIVE'})
    setUserModalOpen(true)
  }

  const openEdit = (user: UserDirectoryView) => {
    setEditingUser(user)
    userForm.setFieldsValue({identitySub: userIdentity(user), status: user.status})
    setUserModalOpen(true)
  }

  const submitUser = async () => {
    try {
      saveUser.mutate(await userForm.validateFields())
    } catch {
      return
    }
  }

  const displayUser = detail.data
  const mutationError = saveUser.error ?? changeStatus.error ?? deleteUser.error

  return (
    <Space orientation="vertical" size="middle" style={{width: '100%'}}>
      <Card
        title="用户目录"
        extra={(
          <PermissionGuard permission="system:user:manage">
            <Button type="primary" onClick={openCreate}>新增用户</Button>
          </PermissionGuard>
        )}
      >
        <Space.Compact block>
          <Input
            aria-label="用户 ID"
            placeholder="按用户 ID 查看详情"
            value={draftUserId}
            onChange={(event) => setDraftUserId(event.target.value)}
          />
          <Button type="primary" onClick={() => setUserId(draftUserId.trim())}>查询</Button>
        </Space.Compact>
        {userId.trim().length > 0 && (
          <PageState
            loading={detail.isPending}
            error={detail.error}
            empty={!displayUser}
            emptyDescription="输入字符串形式的用户 ID 查询"
            onRetry={() => { void detail.refetch() }}
          >
            {displayUser && (
              <>
                <Descriptions bordered column={2} style={{marginTop: 16}}>
                  <Descriptions.Item label="User ID">{displayUser.userId}</Descriptions.Item>
                  <Descriptions.Item label="身份标识">{displayUser.identitySub ?? displayUser.username ?? '-'}</Descriptions.Item>
                  <Descriptions.Item label="用户名">{displayUser.username ?? '-'}</Descriptions.Item>
                  <Descriptions.Item label="显示名">{displayUser.displayName ?? displayUser.identitySub ?? '-'}</Descriptions.Item>
                  <Descriptions.Item label="状态"><Tag>{displayUser.status}</Tag></Descriptions.Item>
                  <Descriptions.Item label="Auth Version">{displayUser.authVersion}</Descriptions.Item>
                  <Descriptions.Item label="目录快照版本">{displayUser.directorySnapshotVersion ?? '-'}</Descriptions.Item>
                </Descriptions>
                <UserRelationsPanel userId={displayUser.userId} authVersion={displayUser.authVersion} />
              </>
            )}
          </PageState>
        )}
      </Card>

      {listMode && (
        <Card title="租户用户成员">
          <Space wrap style={{marginBottom: 16}}>
            <Input
              aria-label="用户搜索"
              placeholder="身份标识或用户 ID"
              value={draftFilter.query}
              onChange={(event) => setDraftFilter({...draftFilter, query: event.target.value})}
              onPressEnter={() => { setSubmittedFilter({...draftFilter, page: 0}) }}
            />
            <Select
              aria-label="用户状态"
              placeholder="状态"
              allowClear
              style={{width: 140}}
              value={draftFilter.status || undefined}
              options={userStatuses}
              onChange={(value) => setDraftFilter({...draftFilter, status: value ?? ''})}
            />
            <Input
              aria-label="组织 ID"
              placeholder="组织 ID"
              value={draftFilter.orgUnitId}
              onChange={(event) => setDraftFilter({...draftFilter, orgUnitId: event.target.value})}
            />
            <Input
              aria-label="岗位 ID"
              placeholder="岗位 ID"
              value={draftFilter.positionId}
              onChange={(event) => setDraftFilter({...draftFilter, positionId: event.target.value})}
            />
            <Button type="primary" onClick={() => { setSubmittedFilter({...draftFilter, page: 0}) }}>查询</Button>
            <Button onClick={() => { setDraftFilter({...emptyFilter}); setSubmittedFilter({...emptyFilter}) }}>重置</Button>
          </Space>
          {mutationError && (
            <AlertForMutation error={mutationError} />
          )}
          <PageState
            loading={users.isPending}
            error={users.error}
            empty={users.data?.items.length === 0}
            emptyDescription="当前租户暂无用户成员"
            onRetry={() => { void users.refetch() }}
          >
            <Table<UserDirectoryView>
              rowKey="userId"
              dataSource={users.data?.items ?? []}
              scroll={{x: 'max-content'}}
              columns={[
                {title: '用户 ID', dataIndex: 'userId', render: (value: string) => <TypographyCode value={value} />},
                {title: '身份标识', render: (_value: unknown, user: UserDirectoryView) => userIdentity(user)},
                {title: '状态', dataIndex: 'status', render: (value: string) => <Tag>{value}</Tag>},
                {title: 'Auth Version', dataIndex: 'authVersion'},
                {
                  title: '操作',
                  fixed: 'right',
                  render: (_value: unknown, user: UserDirectoryView) => (
                    <Space>
                      <Button size="small" onClick={() => { setUserId(user.userId); setDraftUserId(user.userId) }}>详情</Button>
                      <PermissionGuard permission="system:user:manage">
                        <Button size="small" onClick={() => openEdit(user)}>编辑绑定</Button>
                        <Popconfirm title="确认归档该用户成员？" onConfirm={() => deleteUser.mutate(user)}>
                          <Button size="small" danger loading={deleteUser.isPending && deleteUser.variables?.userId === user.userId}>归档</Button>
                        </Popconfirm>
                      </PermissionGuard>
                      <PermissionGuard permission="system:user-status:manage">
                        <Button size="small" loading={changeStatus.isPending && changeStatus.variables?.userId === user.userId} onClick={() => changeStatus.mutate(user)}>
                          {user.status === 'ACTIVE' ? '停用' : '启用'}
                        </Button>
                      </PermissionGuard>
                    </Space>
                  ),
                },
              ]}
              pagination={{
                current: (users.data?.page ?? submittedFilter.page ?? 0) + 1,
                pageSize: users.data?.size ?? submittedFilter.size ?? 20,
                total: users.data?.total ?? 0,
                showSizeChanger: true,
                onChange: (current, size) => setSubmittedFilter({...submittedFilter, page: current - 1, size}),
              }}
            />
          </PageState>
        </Card>
      )}

      <Modal
        open={userModalOpen}
        title={editingUser ? '编辑用户身份绑定' : '新增用户成员'}
        onCancel={() => { setUserModalOpen(false); setEditingUser(null) }}
        onOk={() => { void submitUser() }}
        okText="保存"
        confirmLoading={saveUser.isPending}
        destroyOnHidden
      >
        <Form form={userForm} layout="vertical">
          <Form.Item name="identitySub" label="身份标识" rules={[{required: true}]}>
            <Input />
          </Form.Item>
          {!editingUser && (
            <Form.Item name="status" label="初始状态" rules={[{required: true}]}>
              <Select options={userStatuses} />
            </Form.Item>
          )}
          {editingUser && (
            <Descriptions size="small" column={1} bordered>
              <Descriptions.Item label="当前版本">{editingUser.authVersion}</Descriptions.Item>
            </Descriptions>
          )}
        </Form>
      </Modal>
    </Space>
  )
}

const AlertForMutation = ({error}: {readonly error: unknown}) => (
  <TypographyError error={error} />
)

const TypographyError = ({error}: {readonly error: unknown}) => (
  <div style={{marginBottom: 16}}>
    <Tag color="error">{error instanceof Error ? error.message : String(error)}</Tag>
  </div>
)

const TypographyCode = ({value}: {readonly value: string}) => <span style={{fontFamily: 'monospace'}}>{value}</span>
