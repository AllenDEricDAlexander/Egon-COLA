import {
    Button,
    Card,
    Descriptions,
    Drawer,
    Form,
    Input,
    InputNumber,
    List,
    message,
    Modal,
    Popconfirm,
    Select,
    Space,
    Table,
    Tag,
    Typography,
} from 'antd'
import {DeleteOutlined, EditOutlined, LinkOutlined, PlusOutlined, ReloadOutlined} from '@ant-design/icons'
import {useState} from 'react'
import {useNavigate} from 'react-router-dom'
import {useMutation, useQuery, useQueryClient} from '@tanstack/react-query'
import {httpClient, useAuth} from '../../auth/AuthContext'
import {PageState, usePermission} from '@egon-cola/admin-web-shared'
import type {CreateOAuthClientDTO, OAuthClientVO, UpdateOAuthClientDTO} from '../../api/types'

const STATUS_COLORS: Record<string, string> = {
    ACTIVE: 'green',
    DISABLED: 'default',
    SUSPENDED: 'orange',
}

export const ClientListPage = () => {
  const auth = useAuth()
  const queryClient = useQueryClient()
    const navigate = useNavigate()
  const { has } = usePermission(auth.bootstrap?.permissions ?? [])
    const [createOpen, setCreateOpen] = useState(false)
    const [detailClient, setDetailClient] = useState<OAuthClientVO | null>(null)
    const [editOpen, setEditOpen] = useState(false)
    const [addUriType, setAddUriType] = useState<'redirect' | 'resource' | null>(null)
    const [createForm] = Form.useForm()
    const [editForm] = Form.useForm()
    const [uriForm] = Form.useForm()
  const [messageApi, contextHolder] = message.useMessage()

  const clientsQuery = useQuery({
    queryKey: ['idp', 'clients'],
      queryFn: () => httpClient.request<OAuthClientVO[]>('/api/v1/identity/clients'),
  })

  const createMutation = useMutation({
      mutationFn: (v: CreateOAuthClientDTO) =>
          httpClient.request<OAuthClientVO>('/api/v1/identity/clients', {
        method: 'POST',
              body: JSON.stringify(v),
      }),
    onSuccess: async () => {
        setCreateOpen(false)
        createForm.resetFields()
        await queryClient.invalidateQueries({queryKey: ['idp', 'clients']})
        messageApi.success('客户端已创建')
    },
      onError: (err) => {
          messageApi.error(err instanceof Error ? err.message : '创建失败')
      },
  })

    const updateMutation = useMutation({
        mutationFn: ({clientId, data}: { clientId: string; data: UpdateOAuthClientDTO }) =>
            httpClient.request<OAuthClientVO>(`/api/v1/identity/clients/${encodeURIComponent(clientId)}`, {
                method: 'PATCH',
                body: JSON.stringify(data),
            }),
        onSuccess: async (result) => {
            setEditOpen(false)
            setDetailClient(result)
            await queryClient.invalidateQueries({queryKey: ['idp', 'clients']})
            messageApi.success('客户端已更新')
        },
        onError: (err) => {
            messageApi.error(err instanceof Error ? err.message : '更新失败')
        },
    })

    const addUriMutation = useMutation({
        mutationFn: ({clientId, type, value}: { clientId: string; type: 'redirect' | 'resource'; value: string }) => {
            const suffix = type === 'redirect' ? 'redirect-uris' : 'resource-uris'
            return httpClient.request<OAuthClientVO>(`/api/v1/identity/clients/${encodeURIComponent(clientId)}/${suffix}`, {
                method: 'PUT',
                body: JSON.stringify({value}),
            })
        },
        onSuccess: async (result) => {
            setAddUriType(null)
            uriForm.resetFields()
            setDetailClient(result)
            await queryClient.invalidateQueries({queryKey: ['idp', 'clients']})
            messageApi.success('地址已添加')
        },
        onError: (err) => {
            messageApi.error(err instanceof Error ? err.message : '添加失败')
        },
    })

    const deleteUriMutation = useMutation({
        mutationFn: ({clientId, type, value}: { clientId: string; type: 'redirect' | 'resource'; value: string }) => {
            const suffix = type === 'redirect' ? 'redirect-uris' : 'resource-uris'
            return httpClient.request<OAuthClientVO>(`/api/v1/identity/clients/${encodeURIComponent(clientId)}/${suffix}`, {
                method: 'DELETE',
                body: JSON.stringify({value}),
            })
        },
        onSuccess: async (result) => {
            setDetailClient(result)
      await queryClient.invalidateQueries({ queryKey: ['idp', 'clients'] })
            messageApi.success('地址已删除')
    },
        onError: (err) => {
            messageApi.error(err instanceof Error ? err.message : '删除失败')
        },
  })

    const openEdit = () => {
        if (!detailClient) return
        editForm.setFieldsValue({
            clientName: detailClient.clientName,
            status: detailClient.status,
            accessTokenTtlSeconds: detailClient.accessTokenTtlSeconds,
            refreshTokenTtlSeconds: detailClient.refreshTokenTtlSeconds,
        })
        setEditOpen(true)
    }

  return (
    <>
      {contextHolder}
      <Card
          title="OAuth 客户端"
          extra={
              <Space>
                  <Button icon={<ReloadOutlined/>} onClick={() => {
                      void clientsQuery.refetch()
                  }}>刷新</Button>
                  {has('idp:oauth-client:create') && (
                      <Button type="primary" icon={<PlusOutlined/>}
                              onClick={() => setCreateOpen(true)}>创建客户端</Button>
                  )}
              </Space>
          }
      >
          <PageState
              loading={clientsQuery.isPending}
              error={clientsQuery.error}
              empty={clientsQuery.data?.length === 0}
              emptyDescription="暂无客户端"
              onRetry={() => {
                  void clientsQuery.refetch()
              }}
          >
              <Table<OAuthClientVO>
                  rowKey="clientId"
                  dataSource={clientsQuery.data ?? []}
                  onRow={(row) => ({onClick: () => setDetailClient(row), style: {cursor: 'pointer'}})}
                  columns={[
                      {title: 'Client ID', dataIndex: 'clientId', ellipsis: true},
                      {title: '名称', dataIndex: 'clientName'},
                      {title: '类型', dataIndex: 'clientType'},
                      {
                          title: '状态',
                          dataIndex: 'status',
                          render: (v: string) => <Tag color={STATUS_COLORS[v] ?? 'default'}>{v}</Tag>
                      },
                      {title: 'PKCE', dataIndex: 'pkceRequired', width: 70, render: (v: boolean) => v ? 'S256' : '—'},
                      {title: '回调地址', dataIndex: 'redirectUris', render: (v: string[]) => `${v.length} 个`},
                      {title: 'Resource URI', dataIndex: 'resourceUris', render: (v: string[]) => `${v.length} 个`},
                  ]}
              />
        </PageState>
      </Card>

        {/* Detail Drawer */}
        <Drawer
            title={detailClient ? `客户端：${detailClient.clientId}` : ''}
            open={!!detailClient}
            onClose={() => {
                setDetailClient(null);
                setEditOpen(false);
                setAddUriType(null)
            }}
            width={640}
            extra={
                detailClient && (
                    <Space>
                        {has('idp:resource-server:grant') && (
                            <Button
                                onClick={() => navigate(`/clients/${encodeURIComponent(detailClient.clientId)}/resource-grants`)}>
                                <LinkOutlined/> Resource Grant
                            </Button>
                        )}
                        {has('idp:oauth-client:update') && (
                            <Button type="primary" icon={<EditOutlined/>} onClick={openEdit}>编辑</Button>
                        )}
                    </Space>
                )
            }
        >
            {detailClient && (
                <>
                    <Descriptions column={1} bordered size="small" style={{marginBottom: 24}}>
                        <Descriptions.Item label="Client ID">{detailClient.clientId}</Descriptions.Item>
                        <Descriptions.Item label="名称">{detailClient.clientName}</Descriptions.Item>
                        <Descriptions.Item label="类型">{detailClient.clientType}</Descriptions.Item>
                        <Descriptions.Item label="状态">
                            <Tag color={STATUS_COLORS[detailClient.status]}>{detailClient.status}</Tag>
                        </Descriptions.Item>
                        <Descriptions.Item
                            label="PKCE">{detailClient.pkceRequired ? 'S256 (必需)' : '不要求'}</Descriptions.Item>
                        <Descriptions.Item
                            label="Access Token TTL">{detailClient.accessTokenTtlSeconds}s</Descriptions.Item>
                        <Descriptions.Item
                            label="Refresh Token TTL">{detailClient.refreshTokenTtlSeconds}s</Descriptions.Item>
                        <Descriptions.Item label="版本">{detailClient.version}</Descriptions.Item>
                        <Descriptions.Item label="创建时间">{detailClient.createdAt}</Descriptions.Item>
                        <Descriptions.Item label="更新时间">{detailClient.updatedAt}</Descriptions.Item>
                    </Descriptions>

                    {/* Redirect URIs */}
                    <Card
                        size="small"
                        title="回调地址 (redirect_uri)"
                        extra={has('idp:oauth-client:update') && (
                            <Button size="small" onClick={() => {
                                setAddUriType('redirect');
                                uriForm.resetFields()
                            }}>添加</Button>
                        )}
                        style={{marginBottom: 16}}
                    >
                        {detailClient.redirectUris.length === 0
                            ? <Typography.Text type="secondary">暂无</Typography.Text>
                            : (
                                <List
                                    size="small"
                                    dataSource={[...detailClient.redirectUris]}
                                    renderItem={(uri: string) => (
                                        <List.Item extra={
                                            has('idp:oauth-client:update') && (
                                                <Popconfirm title="确认删除该回调地址？"
                                                            onConfirm={() => deleteUriMutation.mutate({
                                                                clientId: detailClient.clientId,
                                                                type: 'redirect',
                                                                value: uri
                                                            })}>
                                                    <Button size="small" danger icon={<DeleteOutlined/>}/>
                                                </Popconfirm>
                                            )
                                        }>
                                            <Typography.Text code>{uri}</Typography.Text>
                                        </List.Item>
                                    )}
                                />
                            )}
                    </Card>

                    {/* Resource URIs */}
                    <Card
                        size="small"
                        title="Resource URI"
                        extra={has('idp:oauth-client:update') && (
                            <Button size="small" onClick={() => {
                                setAddUriType('resource');
                                uriForm.resetFields()
                            }}>添加</Button>
                        )}
                    >
                        {detailClient.resourceUris.length === 0
                            ? <Typography.Text type="secondary">暂无</Typography.Text>
                            : (
                                <List
                                    size="small"
                                    dataSource={[...detailClient.resourceUris]}
                                    renderItem={(uri: string) => (
                                        <List.Item extra={
                                            has('idp:oauth-client:update') && (
                                                <Popconfirm title="确认删除该 Resource URI？"
                                                            onConfirm={() => deleteUriMutation.mutate({
                                                                clientId: detailClient.clientId,
                                                                type: 'resource',
                                                                value: uri
                                                            })}>
                                                    <Button size="small" danger icon={<DeleteOutlined/>}/>
                                                </Popconfirm>
                                            )
                                        }>
                                            <Typography.Text code>{uri}</Typography.Text>
                                        </List.Item>
                                    )}
                                />
                            )}
                    </Card>
                </>
            )}
        </Drawer>

        {/* Create Modal */}
        <Modal
            title="创建 OAuth 客户端"
            open={createOpen}
            width={560}
            confirmLoading={createMutation.isPending}
            onCancel={() => {
                setCreateOpen(false);
                createForm.resetFields()
            }}
            onOk={() => {
                void createForm.validateFields().then((v: Record<string, unknown>) => {
                    createMutation.mutate({
                        clientId: v.clientId as string,
                        clientName: v.clientName as string,
                        clientType: (v.clientType as string) || 'PUBLIC',
                        accessTokenTtlSeconds: (v.accessTokenTtlSeconds as number) || 900,
                        refreshTokenTtlSeconds: (v.refreshTokenTtlSeconds as number) || 604800,
                        redirectUris: v.redirectUri ? [v.redirectUri as string] : [],
                        resourceUris: v.resourceUri ? [v.resourceUri as string] : [],
                    })
                })
            }}
            destroyOnClose
        >
            <Form form={createForm} layout="vertical" preserve={false}>
                <Form.Item name="clientId" label="Client ID" rules={[{required: true}]}>
                    <Input placeholder="唯一标识，如 my-app"/>
                </Form.Item>
                <Form.Item name="clientName" label="名称" rules={[{required: true}]}>
                    <Input placeholder="展示名称"/>
                </Form.Item>
                <Form.Item name="clientType" label="类型" initialValue="PUBLIC">
                    <Select options={[
                        {label: 'PUBLIC (浏览器客户端)', value: 'PUBLIC'},
                        {label: 'CONFIDENTIAL (机器客户端)', value: 'CONFIDENTIAL'},
                    ]}/>
                </Form.Item>
                <Form.Item name="accessTokenTtlSeconds" label="Access Token TTL (秒)" initialValue={900}>
                    <InputNumber min={1} style={{width: '100%'}}/>
                </Form.Item>
                <Form.Item name="refreshTokenTtlSeconds" label="Refresh Token TTL (秒)" initialValue={604800}>
                    <InputNumber min={1} style={{width: '100%'}}/>
                </Form.Item>
                <Form.Item name="redirectUri" label="回调地址 (redirect_uri)">
                    <Input placeholder="https://app.example.com/callback"/>
                </Form.Item>
                <Form.Item name="resourceUri" label="Resource URI">
                    <Input placeholder="https://api.example.com"/>
                </Form.Item>
            </Form>
        </Modal>

        {/* Edit Modal */}
      <Modal
          title={`编辑：${detailClient?.clientId ?? ''}`}
          open={editOpen}
          confirmLoading={updateMutation.isPending}
          onCancel={() => setEditOpen(false)}
          onOk={() => {
              void editForm.validateFields().then((v) => {
                  if (detailClient) {
                      updateMutation.mutate({
                          clientId: detailClient.clientId,
                          data: {
                              clientName: v.clientName,
                              status: v.status,
                              accessTokenTtlSeconds: v.accessTokenTtlSeconds,
                              refreshTokenTtlSeconds: v.refreshTokenTtlSeconds,
                              expectedVersion: detailClient.version,
                          },
                      })
                  }
              })
          }}
          destroyOnClose
      >
          <Form form={editForm} layout="vertical" preserve={false}>
              <Form.Item name="clientName" label="名称" rules={[{required: true}]}>
                  <Input/>
              </Form.Item>
              <Form.Item name="status" label="状态" rules={[{required: true}]}>
                  <Select options={[
                      {label: 'ACTIVE', value: 'ACTIVE'},
                      {label: 'DISABLED', value: 'DISABLED'},
                      {label: 'SUSPENDED', value: 'SUSPENDED'},
                  ]}/>
              </Form.Item>
              <Form.Item name="accessTokenTtlSeconds" label="Access Token TTL (秒)" rules={[{required: true}]}>
                  <InputNumber min={1} style={{width: '100%'}}/>
              </Form.Item>
              <Form.Item name="refreshTokenTtlSeconds" label="Refresh Token TTL (秒)" rules={[{required: true}]}>
                  <InputNumber min={1} style={{width: '100%'}}/>
              </Form.Item>
          </Form>
      </Modal>

        {/* Add URI Modal */}
        <Modal
            title={`添加${addUriType === 'redirect' ? '回调地址' : 'Resource URI'}`}
            open={!!addUriType}
            confirmLoading={addUriMutation.isPending}
            onCancel={() => {
                setAddUriType(null);
                uriForm.resetFields()
            }}
            onOk={() => {
                void uriForm.validateFields().then((v) => {
                    if (detailClient && addUriType) {
                        addUriMutation.mutate({clientId: detailClient.clientId, type: addUriType, value: v.value})
                    }
                })
            }}
            destroyOnClose
        >
            <Form form={uriForm} layout="vertical" preserve={false}>
                <Form.Item name="value" label={addUriType === 'redirect' ? '回调地址' : 'Resource URI'}
                           rules={[{required: true, type: 'url', message: '请输入有效 URL'}]}>
                    <Input placeholder="https://..."/>
                </Form.Item>
        </Form>
      </Modal>
    </>
  )
}
