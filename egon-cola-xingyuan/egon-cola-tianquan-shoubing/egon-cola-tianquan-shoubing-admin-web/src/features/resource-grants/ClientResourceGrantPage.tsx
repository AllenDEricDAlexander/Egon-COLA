import {
    Alert,
    Button,
    Card,
    Descriptions,
    Form,
    Input,
    message,
    Modal,
    Popconfirm,
    Result,
    Select,
    Space,
    Table,
    Tag,
    Typography,
} from 'antd'
import {ArrowLeftOutlined, DeleteOutlined, LinkOutlined, ReloadOutlined} from '@ant-design/icons'
import {useMemo, useState} from 'react'
import {useNavigate, useParams} from 'react-router-dom'
import {useMutation, useQuery, useQueryClient} from '@tanstack/react-query'
import {httpClient, useAuth} from '../../auth/AuthContext'
import {PageState, usePermission} from '@egon-cola/xingyuan-admin-web-shared'
import {normalizePage, type IdentityPage} from '../../api/page'
import type {
    BatchClientResourceGrantDTO,
    ClientResourceGrantVO,
    DeleteClientResourceGrantDTO,
    ResourceGrantType,
    ResourceServerPageVO,
    ResourceServerVO,
    ServiceTokenContext,
    UpsertClientResourceGrantDTO,
} from '../../api/types'

const GRANT_TYPE_LABELS: Record<string, string> = {
    USER_DELEGATION: '用户委托 (USER_DELEGATION)',
    CLIENT_CREDENTIALS: '服务访问 (CLIENT_CREDENTIALS)',
}

type GrantFormValues = {
    grantType: ResourceGrantType
    scopeContext?: ServiceTokenContext
    tenantId?: string
    allowedScopes?: string
    expectedGrantVersion?: number | string
}

type GrantReadResponse = ClientResourceGrantVO[] | IdentityPage<ClientResourceGrantVO>

type GrantRow = {
    resourceServer: ResourceServerVO
    grant: ClientResourceGrantVO
}

const EMPTY_RESOURCE_SERVERS: readonly ResourceServerVO[] = []
const EMPTY_GRANTS: readonly ClientResourceGrantVO[] = []

const statusOf = (error: unknown): number | undefined => {
    if (typeof error !== 'object' || error === null) return undefined
    const status = (error as {status?: unknown}).status
    return typeof status === 'number' ? status : undefined
}

const grantContext = (grant: ClientResourceGrantVO): string => {
    if (grant.grantType === 'USER_DELEGATION') return '用户委托'
    return grant.tenantId ? `TENANT：${grant.tenantId}` : 'PLATFORM'
}

export const ClientResourceGrantPage = () => {
    const {clientId} = useParams<{ clientId: string }>()
    const auth = useAuth()
    const navigate = useNavigate()
    const queryClient = useQueryClient()
    const {has} = usePermission(auth.bootstrap?.permissions ?? [])
    const [upsertOpen, setUpsertOpen] = useState(false)
    const [selectedRs, setSelectedRs] = useState<ResourceServerVO | null>(null)
    const [selectedResourceServerIds, setSelectedResourceServerIds] = useState<string[]>([])
    const [form] = Form.useForm<GrantFormValues>()
    const [messageApi, contextHolder] = message.useMessage()

    const encodedClientId = clientId ? encodeURIComponent(clientId) : ''
    const rsQuery = useQuery({
        queryKey: ['idp', 'resource-servers'],
        queryFn: () => httpClient
            .request<ResourceServerVO[] | ResourceServerPageVO>('/api/v1/identity/resource-servers')
            .then(normalizePage),
    })
    const grantQuery = useQuery({
        queryKey: ['idp', 'grants', clientId],
        enabled: Boolean(clientId),
        retry: false,
        queryFn: () => httpClient
            .request<GrantReadResponse>(`/api/v1/identity/clients/${encodedClientId}/resources`)
            .then(normalizePage),
    })

    const resourceServers = rsQuery.data?.content ?? EMPTY_RESOURCE_SERVERS
    const grants = grantQuery.data?.content ?? EMPTY_GRANTS
    const grantsByResourceServerId = useMemo(
        () => new Map(grants.map((grant) => [grant.resourceServerId, grant])),
        [grants],
    )
    const selectedGrantRows = useMemo<GrantRow[]>(
        () => resourceServers
            .filter((resourceServer) => selectedResourceServerIds.includes(resourceServer.resourceServerId))
            .map((resourceServer) => ({
                resourceServer,
                grant: grantsByResourceServerId.get(resourceServer.resourceServerId),
            }))
            .filter((row): row is GrantRow => Boolean(row.grant)),
        [grantsByResourceServerId, resourceServers, selectedResourceServerIds],
    )
    const firstSelectedGrantRow = selectedGrantRows[0]
    const batchSelectionCompatible = Boolean(
        firstSelectedGrantRow
        && selectedGrantRows.length === selectedResourceServerIds.length
        && selectedGrantRows.every(({resourceServer, grant}) =>
            resourceServer.bizCode === firstSelectedGrantRow.resourceServer.bizCode
            && resourceServer.environment === firstSelectedGrantRow.resourceServer.environment
            && grant.grantType === firstSelectedGrantRow.grant.grantType
            && grant.tenantId === firstSelectedGrantRow.grant.tenantId),
    )
    const canManageGrant = has('idp:resource-server:grant')
    const grantReadUnavailable = statusOf(grantQuery.error) === 404
    const grantReadFailed = Boolean(grantQuery.error) && !grantReadUnavailable

    const invalidateGrantQueries = async () => {
        await Promise.all([
            queryClient.invalidateQueries({queryKey: ['idp', 'resource-servers']}),
            queryClient.invalidateQueries({queryKey: ['idp', 'grants', clientId]}),
        ])
    }

    const upsertMutation = useMutation({
        mutationFn: ({rsId, data}: {rsId: string; data: UpsertClientResourceGrantDTO}) =>
            httpClient.request<ClientResourceGrantVO>(
                `/api/v1/identity/clients/${encodedClientId}/resources/${encodeURIComponent(rsId)}`,
                {method: 'PUT', body: JSON.stringify(data)},
            ),
        onSuccess: async () => {
            setUpsertOpen(false)
            form.resetFields()
            await invalidateGrantQueries()
            messageApi.success('Grant 已保存')
        },
        onError: (err) => {
            messageApi.error(err instanceof Error ? err.message : '保存失败')
        },
    })

    const deleteMutation = useMutation({
        mutationFn: ({resourceServer, grant}: GrantRow) => {
            const data: DeleteClientResourceGrantDTO = {
                grantType: grant.grantType,
                tenantId: grant.tenantId,
                expectedResourceVersion: resourceServer.version,
                expectedGrantVersion: grant.version,
            }
            return httpClient.request<void>(
                `/api/v1/identity/clients/${encodedClientId}/resources/${encodeURIComponent(resourceServer.resourceServerId)}`,
                {method: 'DELETE', body: JSON.stringify(data)},
            )
        },
        onSuccess: async (_result, input) => {
            setSelectedResourceServerIds((current) => current.filter(
                (resourceServerId) => resourceServerId !== input.resourceServer.resourceServerId,
            ))
            await invalidateGrantQueries()
            messageApi.success('Grant 已删除')
        },
        onError: (err) => {
            messageApi.error(err instanceof Error ? err.message : '删除失败')
        },
    })

    const batchDeleteMutation = useMutation({
        mutationFn: (rows: GrantRow[]) => {
            const first = rows[0]
            if (!first) throw new Error('请选择可删除的 Grant')
            const data: BatchClientResourceGrantDTO = {
                bizCode: first.resourceServer.bizCode,
                environment: first.resourceServer.environment,
                appCodes: rows.map(({resourceServer}) => resourceServer.appCode),
                action: 'DELETE',
                grantType: first.grant.grantType,
                tenantId: first.grant.tenantId,
                allowedScopes: [],
                expectedResourceVersions: Object.fromEntries(rows.map(({resourceServer}) => [
                    resourceServer.appCode,
                    resourceServer.version,
                ])),
                expectedGrantVersions: Object.fromEntries(rows.map(({resourceServer, grant}) => [
                    resourceServer.appCode,
                    grant.version,
                ])),
            }
            return httpClient.request<ClientResourceGrantVO[]>(
                `/api/v1/identity/clients/${encodedClientId}/resource-grants/actions/batch`,
                {method: 'POST', body: JSON.stringify(data)},
            )
        },
        onSuccess: async () => {
            setSelectedResourceServerIds([])
            await invalidateGrantQueries()
            messageApi.success('Grant 已批量删除')
        },
        onError: (err) => {
            messageApi.error(err instanceof Error ? err.message : '批量删除失败')
        },
    })

    if (!clientId) {
        return <Result status="error" title="缺少 clientId 参数"
                       extra={<Button onClick={() => navigate('/clients')}>返回客户端列表</Button>}/>
    }

    const openUpsert = (resourceServer: ResourceServerVO) => {
        const existingGrant = grantsByResourceServerId.get(resourceServer.resourceServerId)
        setSelectedRs(resourceServer)
        form.setFieldsValue({
            grantType: existingGrant?.grantType ?? 'USER_DELEGATION',
            scopeContext: existingGrant?.tenantId ? 'TENANT' : 'PLATFORM',
            allowedScopes: existingGrant?.allowedScopes.join(' ') ?? '',
            tenantId: existingGrant?.tenantId ?? '',
            expectedGrantVersion: existingGrant?.version,
        })
        setUpsertOpen(true)
    }

    const confirmDelete = (row: GrantRow) => {
        deleteMutation.mutate(row)
    }

    return (
        <>
            {contextHolder}
            <Card
                title={
                    <Space>
                        <Button icon={<ArrowLeftOutlined/>} onClick={() => navigate('/clients')} type="text"/>
                        <span>Client：<Typography.Text code>{clientId}</Typography.Text> 的 Resource Grant</span>
                    </Space>
                }
                extra={
                    <Button icon={<ReloadOutlined/>} onClick={() => {
                        void Promise.all([rsQuery.refetch(), grantQuery.refetch()])
                    }}>刷新</Button>
                }
            >
                <Typography.Paragraph type="secondary" style={{marginBottom: 16}}>
                    为当前 Client 向 Resource Server 建立授权委托（Grant）。SERVICE Grant 的 TENANT/PLATFORM 通过 tenantId 是否有值表达。
                </Typography.Paragraph>
                {grantReadUnavailable && (
                    <Alert
                        type="warning"
                        showIcon
                        title="Grant 读取接口待补齐"
                        description="当前后端已提供 Grant 保存、删除与批量变更接口；读取接口返回 404 时不将其误判为空授权。"
                        style={{marginBottom: 16}}
                    />
                )}
                {grantReadFailed && (
                    <Alert
                        type="error"
                        showIcon
                        title="Grant 读取失败"
                        description={grantQuery.error instanceof Error ? grantQuery.error.message : '请稍后重试'}
                        style={{marginBottom: 16}}
                    />
                )}
                {grantQuery.isPending && (
                    <Alert
                        type="info"
                        showIcon
                        title="正在读取当前 Grant"
                        style={{marginBottom: 16}}
                    />
                )}
                {grantQuery.data && selectedResourceServerIds.length > 0 && (
                    <Space wrap style={{marginBottom: 16}}>
                        <Typography.Text type="secondary">已选 {selectedResourceServerIds.length} 个 Resource Server</Typography.Text>
                        <Popconfirm
                            title="确认批量删除 Grant？"
                            description="仅删除已读取且属于同一业务域、环境和授权上下文的 Grant。"
                            okText="确认批量删除"
                            cancelText="取消"
                            disabled={!batchSelectionCompatible}
                            onConfirm={() => {
                                if (batchSelectionCompatible) batchDeleteMutation.mutate(selectedGrantRows)
                            }}
                        >
                            <Button
                                danger
                                disabled={!canManageGrant || !batchSelectionCompatible}
                                loading={batchDeleteMutation.isPending}
                            >
                                批量删除 Grant
                            </Button>
                        </Popconfirm>
                        {!batchSelectionCompatible && (
                            <Typography.Text type="warning">请选择同一业务域、环境和授权上下文且已登记 Grant 的资源。</Typography.Text>
                        )}
                    </Space>
                )}
                <PageState
                    loading={rsQuery.isPending}
                    error={rsQuery.error}
                    empty={resourceServers.length === 0}
                    emptyDescription="暂无 Resource Server，请先创建"
                    onRetry={() => {
                        void rsQuery.refetch()
                    }}
                >
                    <Table<ResourceServerVO>
                        rowKey="resourceServerId"
                        dataSource={resourceServers}
                        rowSelection={grantQuery.data ? {
                            selectedRowKeys: selectedResourceServerIds,
                            onChange: (keys) => setSelectedResourceServerIds(keys.map(String)),
                        } : undefined}
                        columns={[
                            {title: 'Resource Server', dataIndex: 'displayName'},
                            {title: 'ID', dataIndex: 'resourceServerId', ellipsis: true},
                            {title: '业务域', dataIndex: 'bizCode'},
                            {title: '应用', dataIndex: 'appCode'},
                            {title: '环境', dataIndex: 'environment'},
                            {
                                title: '状态',
                                dataIndex: 'status',
                                render: (v: string) => <Tag color={v === 'ACTIVE' ? 'green' : 'default'}>{v}</Tag>,
                            },
                            {
                                title: '当前 Grant',
                                render: (_value: unknown, resourceServer: ResourceServerVO) => {
                                    const grant = grantsByResourceServerId.get(resourceServer.resourceServerId)
                                    if (grantReadUnavailable || grantReadFailed) return <Tag>读取不可用</Tag>
                                    if (grantQuery.isPending) return <Tag>读取中</Tag>
                                    if (!grant) return <Typography.Text type="secondary">未登记</Typography.Text>
                                    return (
                                        <Space orientation="vertical" size={0}>
                                            <Tag color={grant.status === 'ACTIVE' ? 'green' : 'default'}>{grant.status}</Tag>
                                            <Typography.Text type="secondary">{grantContext(grant)}</Typography.Text>
                                            <Typography.Text type="secondary">Scope：{grant.allowedScopes.join(', ') || '—'}</Typography.Text>
                                        </Space>
                                    )
                                },
                            },
                            {
                                title: '操作', width: 260,
                                render: (_value: unknown, resourceServer: ResourceServerVO) => {
                                    const grant = grantsByResourceServerId.get(resourceServer.resourceServerId)
                                    const row = grant ? {resourceServer, grant} : null
                                    return (
                                        <Space size="small">
                                            {canManageGrant && (
                                                <Button size="small" icon={<LinkOutlined/>} onClick={() => openUpsert(resourceServer)}>
                                                    新建/更新 Grant
                                                </Button>
                                            )}
                                            {canManageGrant && row && grantQuery.data && (
                                                <Popconfirm
                                                    title="确认删除 Grant？"
                                                    okText="确认删除"
                                                    cancelText="取消"
                                                    onConfirm={() => confirmDelete(row)}
                                                >
                                                    <Button
                                                        danger
                                                        size="small"
                                                        icon={<DeleteOutlined/>}
                                                        loading={deleteMutation.isPending}
                                                    >
                                                        删除 Grant
                                                    </Button>
                                                </Popconfirm>
                                            )}
                                        </Space>
                                    )
                                },
                            },
                        ]}
                    />
                </PageState>
            </Card>

            <Modal
                title={`${selectedRs ? `为 ${selectedRs.displayName} 配置 Grant` : ''}`}
                open={upsertOpen}
                width={520}
                confirmLoading={upsertMutation.isPending}
                onCancel={() => {
                    setUpsertOpen(false)
                    form.resetFields()
                }}
                onOk={() => {
                    void form.validateFields().then((values) => {
                        if (!selectedRs) return
                        const scopesStr = values.allowedScopes || ''
                        const scopes = scopesStr.split(/\s+/).filter(Boolean)
                        const expectedGrantVersion = values.expectedGrantVersion === undefined
                            || values.expectedGrantVersion === ''
                            ? undefined
                            : Number(values.expectedGrantVersion)
                        const data: UpsertClientResourceGrantDTO = {
                            grantType: values.grantType,
                            tenantId: values.grantType === 'CLIENT_CREDENTIALS'
                            && values.scopeContext === 'TENANT'
                                ? values.tenantId?.trim() || undefined
                                : undefined,
                            allowedScopes: values.grantType === 'CLIENT_CREDENTIALS' ? scopes : [],
                            expectedResourceVersion: selectedRs.version,
                            ...(expectedGrantVersion === undefined ? {} : {expectedGrantVersion}),
                        }
                        upsertMutation.mutate({rsId: selectedRs.resourceServerId, data})
                    })
                }}
                destroyOnHidden
            >
                {selectedRs && (
                    <>
                        <Descriptions column={2} bordered size="small" style={{marginBottom: 16}}>
                            <Descriptions.Item label="Resource Server">{selectedRs.displayName}</Descriptions.Item>
                            <Descriptions.Item label="RS 版本">{selectedRs.version}</Descriptions.Item>
                        </Descriptions>
                        <Form form={form} layout="vertical" preserve={false}>
                            <Form.Item name="grantType" label="授权类型" rules={[{required: true}]}>
                                <Select options={[
                                    {label: GRANT_TYPE_LABELS.USER_DELEGATION, value: 'USER_DELEGATION'},
                                    {label: GRANT_TYPE_LABELS.CLIENT_CREDENTIALS, value: 'CLIENT_CREDENTIALS'},
                                ]}/>
                            </Form.Item>
                            <Form.Item noStyle dependencies={['grantType']}>
                                {({getFieldValue}) => getFieldValue('grantType') === 'CLIENT_CREDENTIALS' && (
                                    <Form.Item name="scopeContext" label="SERVICE 上下文" rules={[{required: true}]}>
                                        <Select options={[
                                            {label: '租户上下文 (TENANT)', value: 'TENANT'},
                                            {label: '平台上下文 (PLATFORM)', value: 'PLATFORM'},
                                        ]}/>
                                    </Form.Item>
                                )}
                            </Form.Item>
                            <Form.Item noStyle dependencies={['grantType', 'scopeContext']}>
                                {({getFieldValue}) => getFieldValue('grantType') === 'CLIENT_CREDENTIALS'
                                    && getFieldValue('scopeContext') === 'TENANT' && (
                                    <Form.Item name="tenantId" label="租户 ID" rules={[{required: true}]}>
                                        <Input placeholder="TENANT 上下文必填"/>
                                    </Form.Item>
                                )}
                            </Form.Item>
                            <Form.Item noStyle dependencies={['grantType']}>
                                {({getFieldValue}) => getFieldValue('grantType') === 'CLIENT_CREDENTIALS' && (
                                    <Form.Item name="allowedScopes" label="允许的 Scope（空格分隔）">
                                        <Input placeholder="read write admin"/>
                                    </Form.Item>
                                )}
                            </Form.Item>
                            <Form.Item name="expectedGrantVersion" label="Grant 版本（更新时填写，新建留空）">
                                <Input placeholder="留空表示新建" type="number"/>
                            </Form.Item>
                        </Form>
                    </>
                )}
            </Modal>
        </>
    )
}
