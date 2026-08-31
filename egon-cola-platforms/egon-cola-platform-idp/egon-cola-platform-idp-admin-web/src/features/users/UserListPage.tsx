import {Button, Card, Form, Input, message, Modal, Select, Space, Table, Tag, Typography} from 'antd'
import {EditOutlined, LockOutlined, PlusOutlined, ReloadOutlined, StopOutlined} from '@ant-design/icons'
import {useState} from 'react'
import {useMutation, useQuery, useQueryClient} from '@tanstack/react-query'
import {useSearchParams} from 'react-router-dom'
import {httpClient, useAuth} from '../../auth/AuthContext'
import {PageState, usePermission} from '@egon-cola/admin-web-shared'
import {normalizePage} from '../../api/page'
import type {
  CreateIdentityUserDTO,
  CreatedIdentityUserVO,
  IdentityListFilter,
  IdentityUserPageVO,
  IdentityUserVO,
  ResetPasswordVO,
} from '../../api/types'

const STATUS_COLORS: Record<string, string> = {
    ACTIVE: 'green',
    LOCKED: 'red',
    DISABLED: 'default',
    PASSWORD_EXPIRED: 'orange',
}

const PAGE_SIZE = 20

type UserFilterForm = Pick<IdentityListFilter, 'query' | 'status'>

const readUserFilter = (searchParams: URLSearchParams): IdentityListFilter => {
  const pageValue = Number.parseInt(searchParams.get('page') ?? '0', 10)
  const sizeValue = Number.parseInt(searchParams.get('size') ?? String(PAGE_SIZE), 10)

  return {
    page: Number.isInteger(pageValue) && pageValue >= 0 ? pageValue : 0,
    size: Number.isInteger(sizeValue) && sizeValue > 0 ? sizeValue : PAGE_SIZE,
    query: searchParams.get('query')?.trim() || undefined,
    status: searchParams.get('status') || undefined,
  }
}

const buildUserQuery = (filter: IdentityListFilter, includePaging: boolean): string => {
  const query = new URLSearchParams()
  if (includePaging) {
    query.set('page', String(filter.page))
    query.set('size', String(filter.size))
  }
  if (filter.query) query.set('query', filter.query)
  if (filter.status) query.set('status', filter.status)
  return query.toString()
}

const toUserSearchParams = ({page, size, query, status}: IdentityListFilter): URLSearchParams => {
  const searchParams = new URLSearchParams({page: String(page), size: String(size)})
  if (query?.trim()) searchParams.set('query', query.trim())
  if (status) searchParams.set('status', status)
  return searchParams
}

export const UserListPage = () => {
  const auth = useAuth()
  const queryClient = useQueryClient()
  const { has } = usePermission(auth.bootstrap?.permissions ?? [])
  const [searchParams, setSearchParams] = useSearchParams()
    const [createOpen, setCreateOpen] = useState(false)
    const [editUser, setEditUser] = useState<IdentityUserVO | null>(null)
    const [filterForm] = Form.useForm<UserFilterForm>()
    const [createForm] = Form.useForm()
    const [editForm] = Form.useForm()
  const [messageApi, contextHolder] = message.useMessage()
  const submitted = readUserFilter(searchParams)
  const requestQuery = buildUserQuery(submitted, searchParams.has('page') || searchParams.has('size'))

  const usersQuery = useQuery({
    queryKey: ['idp', 'users', requestQuery],
      queryFn: () => httpClient
        .request<IdentityUserVO[] | IdentityUserPageVO>(`/api/v1/identity/users${requestQuery ? `?${requestQuery}` : ''}`)
        .then(normalizePage),
  })

  const createMutation = useMutation({
      mutationFn: (v: CreateIdentityUserDTO) =>
          httpClient.request<CreatedIdentityUserVO>('/api/v1/identity/users', {
        method: 'POST',
              body: JSON.stringify(v),
      }),
    onSuccess: async (result) => {
        setCreateOpen(false)
        createForm.resetFields()
        await queryClient.invalidateQueries({queryKey: ['idp', 'users']})
        Modal.success({
            title: '用户已创建',
            content: (
                <div>
                    <p>用户：<Typography.Text code>{result.subject}</Typography.Text></p>
                    <p>一次性密码：<Typography.Text code copyable>{result.oneTimePassword}</Typography.Text></p>
                    <Typography.Text type="danger">关闭此窗口后密码不再显示</Typography.Text>
                </div>
            ),
        })
    },
      onError: (err) => {
          messageApi.error(err instanceof Error ? err.message : '创建失败')
      },
  })

    const editMutation = useMutation({
        mutationFn: ({subject, data}: {
            subject: string;
            data: { displayName: string; status: string; expectedVersion: number }
        }) =>
            httpClient.request<IdentityUserVO>(`/api/v1/identity/users/${encodeURIComponent(subject)}`, {
                method: 'PATCH',
                body: JSON.stringify(data),
            }),
        onSuccess: async () => {
            setEditUser(null)
            editForm.resetFields()
      await queryClient.invalidateQueries({ queryKey: ['idp', 'users'] })
            messageApi.success('用户已更新')
    },
        onError: (err) => {
            messageApi.error(err instanceof Error ? err.message : '更新失败')
        },
  })

  const resetPasswordMutation = useMutation({
    mutationFn: (subject: string) =>
        httpClient.request<ResetPasswordVO>(`/api/v1/identity/users/${encodeURIComponent(subject)}/password-reset`, {method: 'POST'}),
      onSuccess: (result) => {
          Modal.success({
              title: '密码已重置',
              content: <Typography.Text code copyable>{result.oneTimePassword}</Typography.Text>,
          })
      },
      onError: (err) => {
          messageApi.error(err instanceof Error ? err.message : '重置失败')
      },
  })

  const revokeMutation = useMutation({
    mutationFn: (subject: string) =>
      httpClient.request(`/api/v1/identity/users/${encodeURIComponent(subject)}/revoke-all`, { method: 'POST' }),
    onSuccess: async () => {
        messageApi.success('该用户全部会话已撤销')
      await queryClient.invalidateQueries({ queryKey: ['idp', 'users'] })
    },
      onError: (err) => {
          messageApi.error(err instanceof Error ? err.message : '撤销失败')
      },
  })

    const openEdit = (user: IdentityUserVO) => {
        setEditUser(user)
        editForm.setFieldsValue({displayName: user.displayName, status: user.status})
    }

  return (
    <>
      {contextHolder}
      <Card
        title="全局身份用户"
        extra={
            <Space>
                <Button icon={<ReloadOutlined/>} onClick={() => {
                    void usersQuery.refetch()
                }}>刷新</Button>
                {has('idp:identity-user:create') && (
                    <Button type="primary" icon={<PlusOutlined/>} onClick={() => setCreateOpen(true)}>创建用户</Button>
                )}
            </Space>
        }
      >
          <Form<UserFilterForm>
              form={filterForm}
              layout="inline"
              initialValues={{query: submitted.query, status: submitted.status}}
              onFinish={(values) => {
                  setSearchParams(toUserSearchParams({
                      page: 0,
                      size: PAGE_SIZE,
                      query: values.query,
                      status: values.status,
                  }))
              }}
              style={{marginBottom: 16}}
          >
              <Form.Item name="query" label="用户">
                  <Input allowClear placeholder="用户名/显示名"/>
              </Form.Item>
              <Form.Item name="status" label="状态">
                  <Select
                      allowClear
                      placeholder="全部"
                      options={Object.keys(STATUS_COLORS).map((status) => ({label: status, value: status}))}
                      style={{width: 160}}
                  />
              </Form.Item>
              <Form.Item>
                  <Space>
                      <Button type="primary" htmlType="submit">查询</Button>
                      <Button onClick={() => {
                          filterForm.resetFields()
                          setSearchParams(toUserSearchParams({page: 0, size: PAGE_SIZE}))
                      }}>重置</Button>
                  </Space>
              </Form.Item>
          </Form>
          <PageState
              loading={usersQuery.isPending}
              error={usersQuery.error}
              empty={usersQuery.data?.content.length === 0}
              emptyDescription="暂无用户"
              onRetry={() => {
                  void usersQuery.refetch()
              }}
          >
              <Table<IdentityUserVO>
            rowKey="subject"
            dataSource={usersQuery.data?.content ?? []}
            pagination={{
                current: (usersQuery.data?.page ?? submitted.page) + 1,
                pageSize: usersQuery.data?.size || submitted.size,
                total: usersQuery.data?.totalElements ?? 0,
                showTotal: (total) => `共 ${total} 条`,
                onChange: (page, size) => {
                    setSearchParams(toUserSearchParams({...submitted, page: page - 1, size}))
                },
            }}
            columns={[
              { title: '用户名', dataIndex: 'username' },
              { title: '显示名', dataIndex: 'displayName' },
              {
                  title: '状态', dataIndex: 'status',
                  render: (v: string) => <Tag color={STATUS_COLORS[v] ?? 'default'}>{v}</Tag>,
              },
                {title: '登录失败', dataIndex: 'failedLoginCount', width: 90},
                {title: '最后登录', dataIndex: 'lastLoginAt', render: (v?: string) => v ?? '-'},
                {
                    title: '操作', width: 260,
                    render: (_: unknown, row: IdentityUserVO) => (
                        <Space size="small">
                            {has('idp:identity-user:update') && (
                                <Button size="small" icon={<EditOutlined/>} onClick={() => openEdit(row)}>编辑</Button>
                            )}
                            {has('idp:identity-user:password-reset') && (
                                <Button size="small" icon={<LockOutlined/>}
                                        onClick={() => resetPasswordMutation.mutate(row.subject)}>重置密码</Button>
                            )}
                            {has('idp:identity-user:revoke-all') && (
                                <Button size="small" danger icon={<StopOutlined/>}
                                        onClick={() => revokeMutation.mutate(row.subject)}>撤销会话</Button>
                            )}
                  </Space>
                ),
              },
            ]}
          />
        </PageState>
      </Card>

        {/* Create Modal */}
      <Modal
        title="创建全局身份用户"
        open={createOpen}
        confirmLoading={createMutation.isPending}
        onCancel={() => {
            setCreateOpen(false);
            createForm.resetFields()
        }}
        onOk={() => {
            void createForm.validateFields().then((v) => createMutation.mutate(v))
        }}
        destroyOnHidden
      >
          <Form form={createForm} layout="vertical" preserve={false}>
              <Form.Item name="username" label="用户名" rules={[{required: true, message: '请输入用户名'}]}>
                  <Input autoComplete="off"/>
              </Form.Item>
              <Form.Item name="displayName" label="显示名" rules={[{required: true, message: '请输入显示名'}]}>
                  <Input/>
              </Form.Item>
          </Form>
      </Modal>

        {/* Edit Modal */}
        <Modal
            title={`编辑用户：${editUser?.username ?? ''}`}
            open={!!editUser}
            confirmLoading={editMutation.isPending}
            onCancel={() => {
                setEditUser(null);
                editForm.resetFields()
            }}
            onOk={() => {
                void editForm.validateFields().then((v) => {
                    if (editUser) {
                        editMutation.mutate({
                            subject: editUser.subject,
                            data: {displayName: v.displayName, status: v.status, expectedVersion: editUser.version},
                        })
                    }
                })
            }}
            destroyOnHidden
        >
            <Form form={editForm} layout="vertical" preserve={false}>
                <Form.Item name="displayName" label="显示名" rules={[{required: true, message: '请输入显示名'}]}>
                    <Input/>
                </Form.Item>
                <Form.Item name="status" label="状态" rules={[{required: true}]}>
                    <Select options={[
                        {label: 'ACTIVE', value: 'ACTIVE'},
                        {label: 'DISABLED', value: 'DISABLED'},
                        {label: 'LOCKED', value: 'LOCKED'},
                    ]}/>
                </Form.Item>
        </Form>
      </Modal>
    </>
  )
}
