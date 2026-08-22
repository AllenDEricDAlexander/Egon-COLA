import {
    Button,
    Card,
    Descriptions,
    Drawer,
    Form,
    Input,
    message,
    Modal,
    Select,
    Space,
    Table,
    Tag,
    Typography,
} from 'antd'
import {EditOutlined, PlusOutlined, ReloadOutlined, TeamOutlined} from '@ant-design/icons'
import {useState} from 'react'
import {useMutation, useQuery, useQueryClient} from '@tanstack/react-query'
import {httpClient, useAuth} from '../../auth/AuthContext'
import {PageState, usePermission} from '@egon-cola/admin-web-shared'
import type {
    CreateTenantDTO,
    TenantMembershipPageVO,
    TenantMembershipVO,
    TenantPageVO,
    TenantStatus,
    TenantVO,
    UpdateTenantDTO,
    UpsertTenantMembershipDTO,
} from '../../api/types'

const TENANT_STATUS_COLORS: Record<string, string> = {
    INITIALIZING: 'blue',
    ACTIVE: 'green',
    SUSPENDED: 'orange',
    CLOSED: 'default',
}

const MEMBERSHIP_STATUS_COLORS: Record<string, string> = {
    ACTIVE: 'green',
    DISABLED: 'default',
}

const parseSettings = (value: unknown): Record<string, unknown> => {
    if (!value || typeof value !== 'string' || !value.trim()) return {}
    const parsed: unknown = JSON.parse(value)
    if (!parsed || typeof parsed !== 'object' || Array.isArray(parsed)) {
        throw new Error('Settings 必须是 JSON 对象')
    }
    return parsed as Record<string, unknown>
}

export const TenantListPage = () => {
    const auth = useAuth()
    const queryClient = useQueryClient()
    const {has} = usePermission(auth.bootstrap?.permissions ?? [])
    const [createOpen, setCreateOpen] = useState(false)
    const [detailTenant, setDetailTenant] = useState<TenantVO | null>(null)
    const [editOpen, setEditOpen] = useState(false)
    const [memberOpen, setMemberOpen] = useState(false)
    const [memberFormOpen, setMemberFormOpen] = useState(false)
    const [memberEdit, setMemberEdit] = useState<TenantMembershipVO | null>(null)
    const [createForm] = Form.useForm()
    const [editForm] = Form.useForm()
    const [memberForm] = Form.useForm()
    const [messageApi, contextHolder] = message.useMessage()

    const tenantsQuery = useQuery({
        queryKey: ['idp', 'tenants', 0, 20],
        queryFn: () => httpClient.request<TenantPageVO>('/api/v1/identity/tenants?page=0&size=20'),
    })

    const createMutation = useMutation({
        mutationFn: (data: CreateTenantDTO) =>
            httpClient.request<TenantVO>('/api/v1/identity/tenants', {
                method: 'POST',
                body: JSON.stringify(data),
            }),
        onSuccess: async () => {
            setCreateOpen(false)
            createForm.resetFields()
            await queryClient.invalidateQueries({queryKey: ['idp', 'tenants']})
            messageApi.success('租户已创建')
        },
        onError: (err) => messageApi.error(err instanceof Error ? err.message : '创建失败'),
    })

    const updateMutation = useMutation({
        mutationFn: ({tenantId, data}: { tenantId: string; data: UpdateTenantDTO }) =>
            httpClient.request<TenantVO>(`/api/v1/identity/tenants/${encodeURIComponent(tenantId)}`, {
                method: 'PATCH',
                body: JSON.stringify(data),
            }),
        onSuccess: async (result) => {
            setEditOpen(false)
            setDetailTenant(result)
            editForm.resetFields()
            await queryClient.invalidateQueries({queryKey: ['idp', 'tenants']})
            messageApi.success('租户已更新')
        },
        onError: (err) => messageApi.error(err instanceof Error ? err.message : '更新失败'),
    })

    const membersQuery = useQuery({
        queryKey: ['idp', 'tenant-members', detailTenant?.tenantId, 0, 20],
        enabled: memberOpen && !!detailTenant,
        queryFn: () => httpClient.request<TenantMembershipPageVO>(
            `/api/v1/identity/tenants/${encodeURIComponent(detailTenant!.tenantId)}/members?page=0&size=20`,
        ),
    })

    const memberMutation = useMutation({
        mutationFn: ({tenantId, identitySub, data}: {
            tenantId: string
            identitySub: string
            data: UpsertTenantMembershipDTO
        }) => httpClient.request<TenantMembershipVO>(
            `/api/v1/identity/tenants/${encodeURIComponent(tenantId)}/members/${encodeURIComponent(identitySub)}`,
            {
                method: 'PUT',
                body: JSON.stringify(data),
            },
        ),
        onSuccess: async () => {
            setMemberEdit(null)
            setMemberFormOpen(false)
            memberForm.resetFields()
            await queryClient.invalidateQueries({queryKey: ['idp', 'tenant-members', detailTenant?.tenantId]})
            messageApi.success('成员已保存')
        },
        onError: (err) => messageApi.error(err instanceof Error ? err.message : '成员保存失败'),
    })

    const openEdit = () => {
        if (!detailTenant) return
        editForm.setFieldsValue({
            tenantName: detailTenant.tenantName,
            status: detailTenant.status,
            settings: JSON.stringify(detailTenant.settings ?? {}, null, 2),
        })
        setEditOpen(true)
    }

    const openMembers = () => {
        if (!detailTenant) return
        setMemberOpen(true)
    }

    const openMemberEdit = (member?: TenantMembershipVO) => {
        setMemberEdit(member ?? null)
        setMemberFormOpen(true)
        memberForm.setFieldsValue({
            identitySub: member?.identitySub ?? '',
            status: member?.status ?? 'ACTIVE',
            expectedVersion: member?.version,
        })
    }

    const submitCreate = async () => {
        try {
            const value = await createForm.validateFields()
            createMutation.mutate({
                tenantCode: value.tenantCode,
                tenantName: value.tenantName,
                settings: parseSettings(value.settings),
            })
        } catch (err) {
            if (err instanceof SyntaxError || err instanceof Error && err.message.startsWith('Settings')) {
                messageApi.error(err instanceof Error ? err.message : 'Settings JSON 无效')
            }
        }
    }

    const submitUpdate = async () => {
        if (!detailTenant) return
        try {
            const value = await editForm.validateFields()
            updateMutation.mutate({
                tenantId: detailTenant.tenantId,
                data: {
                    tenantName: value.tenantName,
                    status: value.status,
                    settings: parseSettings(value.settings),
                    expectedVersion: detailTenant.version,
                },
            })
        } catch (err) {
            if (err instanceof SyntaxError || err instanceof Error && err.message.startsWith('Settings')) {
                messageApi.error(err instanceof Error ? err.message : 'Settings JSON 无效')
            }
        }
    }

    const submitMember = async () => {
        if (!detailTenant) return
        try {
            const value = await memberForm.validateFields()
            const data: UpsertTenantMembershipDTO = {status: value.status}
            if (value.expectedVersion !== undefined && value.expectedVersion !== null && value.expectedVersion !== '') {
                data.expectedVersion = Number(value.expectedVersion)
            }
            memberMutation.mutate({
                tenantId: detailTenant.tenantId,
                identitySub: value.identitySub,
                data,
            })
        } catch {
            // Ant Design renders field-level validation; preserve the entered values.
        }
    }

    return (
        <>
            {contextHolder}
            <Card
                title="IdP 租户目录"
                extra={
                    <Space>
                        <Button icon={<ReloadOutlined/>} onClick={() => {void tenantsQuery.refetch()}}>刷新</Button>
                        {has('idp:tenant:manage') && (
                            <Button type="primary" icon={<PlusOutlined/>} onClick={() => {
                                createForm.resetFields()
                                setCreateOpen(true)
                            }}>创建租户</Button>
                        )}
                    </Space>
                }
            >
                <PageState
                    loading={tenantsQuery.isPending}
                    error={tenantsQuery.error}
                    empty={tenantsQuery.data?.content.length === 0}
                    emptyDescription="暂无租户"
                    onRetry={() => {void tenantsQuery.refetch()}}
                >
                    <Table<TenantVO>
                        rowKey="tenantId"
                        dataSource={tenantsQuery.data?.content ?? []}
                        onRow={(row) => ({onClick: () => setDetailTenant(row), style: {cursor: 'pointer'}})}
                        columns={[
                            {title: '编码', dataIndex: 'tenantCode'},
                            {title: '名称', dataIndex: 'tenantName'},
                            {
                                title: '状态',
                                dataIndex: 'status',
                                render: (value: TenantStatus) => (
                                    <Tag color={TENANT_STATUS_COLORS[value] ?? 'default'}>{value}</Tag>
                                ),
                            },
                            {title: '版本', dataIndex: 'version', width: 80},
                            {title: '更新时间', dataIndex: 'updatedAt'},
                        ]}
                    />
                </PageState>
            </Card>

            <Drawer
                title={detailTenant ? `租户：${detailTenant.tenantName}` : ''}
                open={!!detailTenant}
                width={640}
                onClose={() => {
                    setDetailTenant(null)
                    setEditOpen(false)
                    setMemberOpen(false)
                    setMemberFormOpen(false)
                }}
                extra={detailTenant && (
                    <Space>
                        <Button icon={<TeamOutlined/>} onClick={openMembers}>成员</Button>
                        {has('idp:tenant:manage') && detailTenant.status !== 'CLOSED' && (
                            <Button type="primary" icon={<EditOutlined/>} onClick={openEdit}>编辑</Button>
                        )}
                    </Space>
                )}
            >
                {detailTenant && (
                    <Descriptions column={1} bordered size="small">
                        <Descriptions.Item label="Tenant ID">{detailTenant.tenantId}</Descriptions.Item>
                        <Descriptions.Item label="编码">{detailTenant.tenantCode}</Descriptions.Item>
                        <Descriptions.Item label="名称">{detailTenant.tenantName}</Descriptions.Item>
                        <Descriptions.Item label="状态">
                            <Tag color={TENANT_STATUS_COLORS[detailTenant.status] ?? 'default'}>{detailTenant.status}</Tag>
                        </Descriptions.Item>
                        <Descriptions.Item label="Settings">
                            <Typography.Text code>{JSON.stringify(detailTenant.settings ?? {})}</Typography.Text>
                        </Descriptions.Item>
                        <Descriptions.Item label="版本">{detailTenant.version}</Descriptions.Item>
                        <Descriptions.Item label="创建时间">{detailTenant.createdAt}</Descriptions.Item>
                        <Descriptions.Item label="更新时间">{detailTenant.updatedAt}</Descriptions.Item>
                    </Descriptions>
                )}
            </Drawer>

            <Drawer
                title={detailTenant ? `成员：${detailTenant.tenantName}` : '成员'}
                open={memberOpen}
                width={640}
                onClose={() => setMemberOpen(false)}
                extra={has('idp:tenant:manage') && detailTenant?.status !== 'CLOSED' && (
                    <Button icon={<PlusOutlined/>} onClick={() => openMemberEdit()}>新增/更新成员</Button>
                )}
            >
                <PageState
                    loading={membersQuery.isPending}
                    error={membersQuery.error}
                    empty={membersQuery.data?.content.length === 0}
                    emptyDescription="暂无成员"
                    onRetry={() => {void membersQuery.refetch()}}
                >
                    <Table<TenantMembershipVO>
                        rowKey="identitySub"
                        dataSource={membersQuery.data?.content ?? []}
                        columns={[
                            {title: 'Identity Sub', dataIndex: 'identitySub'},
                            {title: '显示名', dataIndex: 'displayName'},
                            {
                                title: '状态',
                                dataIndex: 'status',
                                render: (value: string) => (
                                    <Tag color={MEMBERSHIP_STATUS_COLORS[value] ?? 'default'}>{value}</Tag>
                                ),
                            },
                            {title: '版本', dataIndex: 'version'},
                            {title: '更新时间', dataIndex: 'updatedAt'},
                            ...(has('idp:tenant:manage') && detailTenant?.status !== 'CLOSED' ? [{
                                title: '操作',
                                render: (_value: unknown, row: TenantMembershipVO) => (
                                    <Button size="small" onClick={() => openMemberEdit(row)}>编辑</Button>
                                ),
                            }] : []),
                        ]}
                    />
                </PageState>
            </Drawer>

            <Modal
                title="创建 IdP 租户"
                open={createOpen}
                confirmLoading={createMutation.isPending}
                onCancel={() => setCreateOpen(false)}
                onOk={() => {void submitCreate()}}
                destroyOnClose
            >
                <Form form={createForm} layout="vertical" preserve={false}>
                    <Form.Item name="tenantCode" label="租户编码" rules={[{required: true, pattern: /^[a-z][a-z0-9-]{2,63}$/}]}>
                        <Input placeholder="acme" autoComplete="off"/>
                    </Form.Item>
                    <Form.Item name="tenantName" label="租户名称" rules={[{required: true, max: 200}]}>
                        <Input placeholder="Acme"/>
                    </Form.Item>
                    <Form.Item name="settings" label="Settings JSON" initialValue="{}">
                        <Input.TextArea rows={4} placeholder='{"region":"cn"}'/>
                    </Form.Item>
                </Form>
            </Modal>

            <Modal
                title={`编辑租户：${detailTenant?.tenantName ?? ''}`}
                open={editOpen}
                confirmLoading={updateMutation.isPending}
                onCancel={() => setEditOpen(false)}
                onOk={() => {void submitUpdate()}}
                destroyOnClose
            >
                <Form form={editForm} layout="vertical" preserve={false}>
                    <Form.Item name="tenantName" label="租户名称" rules={[{required: true, max: 200}]}>
                        <Input/>
                    </Form.Item>
                    <Form.Item name="status" label="状态" rules={[{required: true}]}>
                        <Select options={[
                            {label: 'INITIALIZING', value: 'INITIALIZING'},
                            {label: 'ACTIVE', value: 'ACTIVE'},
                            {label: 'SUSPENDED', value: 'SUSPENDED'},
                            {label: 'CLOSED', value: 'CLOSED'},
                        ]}/>
                    </Form.Item>
                    <Form.Item name="settings" label="Settings JSON">
                        <Input.TextArea rows={4}/>
                    </Form.Item>
                </Form>
            </Modal>

            <Modal
                title={`${memberEdit ? '编辑' : '新增'}租户成员`}
                open={memberFormOpen}
                confirmLoading={memberMutation.isPending}
                onCancel={() => {
                    setMemberEdit(null)
                    setMemberFormOpen(false)
                    memberForm.resetFields()
                }}
                onOk={() => {void submitMember()}}
                destroyOnClose
            >
                <Form form={memberForm} layout="vertical" preserve={false}>
                    <Form.Item name="identitySub" label="Identity Sub" rules={[{required: true}]}>
                        <Input disabled={!!memberEdit} autoComplete="off"/>
                    </Form.Item>
                    <Form.Item name="status" label="状态" rules={[{required: true}]}>
                        <Select options={[
                            {label: 'ACTIVE', value: 'ACTIVE'},
                            {label: 'DISABLED', value: 'DISABLED'},
                        ]}/>
                    </Form.Item>
                    <Form.Item name="expectedVersion" label="成员版本（新建留空）">
                        <Input type="number" disabled={!memberEdit}/>
                    </Form.Item>
                </Form>
            </Modal>
        </>
    )
}
