import {
    Alert,
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
import {PlayCircleOutlined, PlusOutlined, ReloadOutlined, StopOutlined} from '@ant-design/icons'
import {useState} from 'react'
import {useMutation, useQuery, useQueryClient} from '@tanstack/react-query'
import {useSearchParams} from 'react-router-dom'
import {httpClient, useAuth} from '../../auth/AuthContext'
import {PageState, usePermission} from '@egon-cola/xingyuan-admin-web-shared'
import {normalizePage} from '../../api/page'
import type {
    BatchResourceServerActionDTO,
    CreateResourceServerDTO,
    IdentityListFilter,
    ResourceServerPageVO,
    ResourceServerVO,
} from '../../api/types'

const STATUS_COLORS: Record<string, string> = {
    ACTIVE: 'green',
    DISABLED: 'default',
}

const PAGE_SIZE = 20

type ResourceServerFilterForm = Pick<IdentityListFilter, 'query' | 'status'>

const readResourceServerFilter = (searchParams: URLSearchParams): IdentityListFilter => {
    const pageValue = Number.parseInt(searchParams.get('page') ?? '0', 10)
    const sizeValue = Number.parseInt(searchParams.get('size') ?? String(PAGE_SIZE), 10)

    return {
        page: Number.isInteger(pageValue) && pageValue >= 0 ? pageValue : 0,
        size: Number.isInteger(sizeValue) && sizeValue > 0 ? sizeValue : PAGE_SIZE,
        query: searchParams.get('query')?.trim() || undefined,
        status: searchParams.get('status') || undefined,
    }
}

const buildResourceServerQuery = (filter: IdentityListFilter, includePaging: boolean): string => {
    const query = new URLSearchParams()
    if (includePaging) {
        query.set('page', String(filter.page))
        query.set('size', String(filter.size))
    }
    if (filter.query) query.set('query', filter.query)
    if (filter.status) query.set('status', filter.status)
    return query.toString()
}

const toResourceServerSearchParams = ({page, size, query, status}: IdentityListFilter): URLSearchParams => {
    const searchParams = new URLSearchParams({page: String(page), size: String(size)})
    if (query?.trim()) searchParams.set('query', query.trim())
    if (status) searchParams.set('status', status)
    return searchParams
}

export const ResourceServerListPage = () => {
    const auth = useAuth()
    const queryClient = useQueryClient()
    const {has} = usePermission(auth.bootstrap?.permissions ?? [])
    const [searchParams, setSearchParams] = useSearchParams()
    const [createOpen, setCreateOpen] = useState(false)
    const [detailRs, setDetailRs] = useState<ResourceServerVO | null>(null)
    const [selectedRows, setSelectedRows] = useState<readonly ResourceServerVO[]>([])
    const [statusError, setStatusError] = useState<string | null>(null)
    const [createForm] = Form.useForm()
    const [filterForm] = Form.useForm<ResourceServerFilterForm>()
    const [messageApi, contextHolder] = message.useMessage()
    const submitted = readResourceServerFilter(searchParams)
    const requestQuery = buildResourceServerQuery(submitted, searchParams.has('page') || searchParams.has('size'))

    const rsQuery = useQuery({
        queryKey: ['idp', 'resource-servers', requestQuery],
        queryFn: () => httpClient
            .request<ResourceServerVO[] | ResourceServerPageVO>(`/api/v1/identity/resource-servers${requestQuery ? `?${requestQuery}` : ''}`)
            .then(normalizePage),
    })

    const createMutation = useMutation({
        mutationFn: (v: CreateResourceServerDTO) =>
            httpClient.request<ResourceServerVO>('/api/v1/identity/resource-servers', {
                method: 'POST',
                body: JSON.stringify(v),
            }),
        onSuccess: async () => {
            setCreateOpen(false)
            createForm.resetFields()
            await queryClient.invalidateQueries({queryKey: ['idp', 'resource-servers']})
            messageApi.success('Resource Server 已创建')
        },
        onError: (err) => {
            messageApi.error(err instanceof Error ? err.message : '创建失败')
        },
    })

    const enableMutation = useMutation({
        mutationFn: ({id, version}: { id: string; version: number }) =>
            httpClient.request<ResourceServerVO>(
                `/api/v1/identity/resource-servers/${encodeURIComponent(id)}/enable`,
                {method: 'POST', body: JSON.stringify({expectedVersion: version})},
            ),
        onSuccess: async (result) => {
            setStatusError(null)
            setDetailRs(result)
            await queryClient.invalidateQueries({queryKey: ['idp', 'resource-servers']})
            messageApi.success('已启用')
        },
        onError: (err) => {
            setStatusError(err instanceof Error ? err.message : '启用失败')
            messageApi.error(err instanceof Error ? err.message : '操作失败')
        },
    })

    const disableMutation = useMutation({
        mutationFn: ({id, version}: { id: string; version: number }) =>
            httpClient.request<ResourceServerVO>(
                `/api/v1/identity/resource-servers/${encodeURIComponent(id)}/disable`,
                {method: 'POST', body: JSON.stringify({expectedVersion: version})},
            ),
        onSuccess: async (result) => {
            setStatusError(null)
            setDetailRs(result)
            await queryClient.invalidateQueries({queryKey: ['idp', 'resource-servers']})
            messageApi.success('已禁用')
        },
        onError: (err) => {
            setStatusError(err instanceof Error ? err.message : '禁用失败')
            messageApi.error(err instanceof Error ? err.message : '操作失败')
        },
    })

    const batchMutation = useMutation({
        mutationFn: ({action, rows}: {action: BatchResourceServerActionDTO['action']; rows: readonly ResourceServerVO[]}) => {
            const first = rows[0]
            if (!first) throw new Error('请选择 Resource Server')
            return httpClient.request('/api/v1/identity/resource-servers/actions/batch', {
                method: 'POST',
                body: JSON.stringify({
                    bizCode: first.bizCode,
                    environment: first.environment,
                    appCodes: rows.map((row) => row.appCode),
                    action,
                    expectedVersions: Object.fromEntries(rows.map((row) => [row.appCode, row.version])),
                } satisfies BatchResourceServerActionDTO),
            })
        },
        onSuccess: async () => {
            setSelectedRows([])
            setStatusError(null)
            await queryClient.invalidateQueries({queryKey: ['idp', 'resource-servers']})
            messageApi.success('批量操作已提交')
        },
        onError: (err) => {
            setStatusError(err instanceof Error ? err.message : '批量操作失败')
            messageApi.error(err instanceof Error ? err.message : '批量操作失败')
        },
    })

    return (
        <>
            {contextHolder}
            <Card
                title="Resource Server"
                extra={
                    <Space>
                        <Button icon={<ReloadOutlined/>} onClick={() => {void rsQuery.refetch()}}>刷新</Button>
                        {has('idp:resource-server:create') && (
                            <Button type="primary" icon={<PlusOutlined/>} onClick={() => setCreateOpen(true)}>创建</Button>
                        )}
                    </Space>
                }
            >
                <Form<ResourceServerFilterForm>
                    form={filterForm}
                    layout="inline"
                    initialValues={{query: submitted.query, status: submitted.status}}
                    onFinish={(values) => {
                        setSearchParams(toResourceServerSearchParams({
                            page: 0,
                            size: PAGE_SIZE,
                            query: values.query,
                            status: values.status,
                        }))
                    }}
                    style={{marginBottom: 16}}
                >
                    <Form.Item name="query" label="资源">
                        <Input allowClear placeholder="ID/展示名/应用"/>
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
                                setSearchParams(toResourceServerSearchParams({page: 0, size: PAGE_SIZE}))
                            }}>重置</Button>
                        </Space>
                    </Form.Item>
                </Form>
                {has('idp:resource-server:status') && selectedRows.length > 0 && (
                    <Space style={{marginBottom: 16}}>
                        <Typography.Text>已选 {selectedRows.length} 个</Typography.Text>
                        <Button
                            loading={batchMutation.isPending}
                            onClick={() => batchMutation.mutate({action: 'ENABLE', rows: selectedRows})}
                        >批量启用</Button>
                        <Button
                            danger
                            loading={batchMutation.isPending}
                            onClick={() => batchMutation.mutate({action: 'DISABLE', rows: selectedRows})}
                        >批量禁用</Button>
                    </Space>
                )}
                <PageState
                    loading={rsQuery.isPending}
                    error={rsQuery.error}
                    empty={rsQuery.data?.content.length === 0}
                    emptyDescription="暂无 Resource Server"
                    onRetry={() => {void rsQuery.refetch()}}
                >
                    <Table<ResourceServerVO>
                        rowKey="resourceServerId"
                        dataSource={rsQuery.data?.content ?? []}
                        rowSelection={{
                            selectedRowKeys: selectedRows.map((row) => row.resourceServerId),
                            onChange: (_keys, rows) => setSelectedRows(rows),
                        }}
                        pagination={{
                            current: (rsQuery.data?.page ?? submitted.page) + 1,
                            pageSize: rsQuery.data?.size || submitted.size,
                            total: rsQuery.data?.totalElements ?? 0,
                            showTotal: (total) => `共 ${total} 条`,
                            onChange: (page, size) => {
                                setSearchParams(toResourceServerSearchParams({...submitted, page: page - 1, size}))
                            },
                        }}
                        onRow={(row) => ({onClick: () => setDetailRs(row), style: {cursor: 'pointer'}})}
                        columns={[
                            {title: 'ID', dataIndex: 'resourceServerId', ellipsis: true},
                            {title: '展示名', dataIndex: 'displayName'},
                            {title: '业务域', dataIndex: 'bizCode'},
                            {title: '应用', dataIndex: 'appCode'},
                            {title: '环境', dataIndex: 'environment'},
                            {
                                title: '状态',
                                dataIndex: 'status',
                                render: (v: string) => <Tag color={STATUS_COLORS[v] ?? 'default'}>{v}</Tag>,
                            },
                            {title: '版本', dataIndex: 'version', width: 80},
                            {
                                title: '操作',
                                width: 100,
                                render: (_value: unknown, row: ResourceServerVO) => (
                                    <Button size="small" onClick={() => {
                                        setStatusError(null)
                                        setDetailRs(row)
                                    }}>查看详情</Button>
                                ),
                            },
                        ]}
                    />
                </PageState>
            </Card>

            <Drawer
                title={detailRs ? `Resource Server：${detailRs.displayName}` : ''}
                open={!!detailRs}
                onClose={() => setDetailRs(null)}
                width={640}
                extra={
                    detailRs && (
                        <Space>
                            {has('idp:resource-server:status') && detailRs.status === 'DISABLED' && (
                                <Button
                                    icon={<PlayCircleOutlined/>}
                                    loading={enableMutation.isPending}
                                    onClick={() => enableMutation.mutate({id: detailRs.resourceServerId, version: detailRs.version})}
                                >启用</Button>
                            )}
                            {has('idp:resource-server:status') && detailRs.status === 'ACTIVE' && (
                                <Button
                                    icon={<StopOutlined/>}
                                    danger
                                    loading={disableMutation.isPending}
                                    onClick={() => disableMutation.mutate({id: detailRs.resourceServerId, version: detailRs.version})}
                                >禁用</Button>
                            )}
                        </Space>
                    )
                }
            >
                {detailRs && (
                    <>
                        {statusError && <Alert type="error" showIcon title={statusError} style={{marginBottom: 16}}/>}
                        <Descriptions column={1} bordered size="small">
                            <Descriptions.Item label="ID">{detailRs.resourceServerId}</Descriptions.Item>
                            <Descriptions.Item label="展示名">{detailRs.displayName}</Descriptions.Item>
                            <Descriptions.Item label="Resource URI">{detailRs.resourceUri}</Descriptions.Item>
                            <Descriptions.Item label="业务域">{detailRs.bizCode}</Descriptions.Item>
                            <Descriptions.Item label="应用">{detailRs.appCode}</Descriptions.Item>
                            <Descriptions.Item label="环境">{detailRs.environment}</Descriptions.Item>
                            <Descriptions.Item label="管理 Client">{detailRs.managementClientId}</Descriptions.Item>
                            <Descriptions.Item label="RBAC3 应用">{detailRs.rbacApplicationCode}</Descriptions.Item>
                            <Descriptions.Item label="入口权限">{detailRs.entryPermissionCode}</Descriptions.Item>
                            <Descriptions.Item label="状态">
                                <Tag color={STATUS_COLORS[detailRs.status] ?? 'default'}>{detailRs.status}</Tag>
                            </Descriptions.Item>
                            <Descriptions.Item label="版本">{detailRs.version}</Descriptions.Item>
                            <Descriptions.Item label="创建时间">{detailRs.createdAt}</Descriptions.Item>
                            <Descriptions.Item label="更新时间">{detailRs.updatedAt}</Descriptions.Item>
                        </Descriptions>
                    </>
                )}
            </Drawer>

            <Modal
                title="创建 Resource Server"
                open={createOpen}
                width={600}
                confirmLoading={createMutation.isPending}
                onCancel={() => {
                    setCreateOpen(false)
                    createForm.resetFields()
                }}
                onOk={() => {
                    void createForm.validateFields().then((v) => {
                        createMutation.mutate({
                            resourceServerId: v.resourceServerId,
                            resourceUri: v.resourceUri,
                            bizCode: v.bizCode,
                            appCode: v.appCode,
                            environment: v.environment,
                            displayName: v.displayName,
                            managementClientId: v.managementClientId,
                            rbacApplicationCode: v.rbacApplicationCode,
                            entryPermissionCode: v.entryPermissionCode,
                        })
                    })
                }}
                destroyOnHidden
            >
                <Form form={createForm} layout="vertical" preserve={false}>
                    <Typography.Title level={5}>基本信息</Typography.Title>
                    <Form.Item name="resourceServerId" label="ID" rules={[{required: true}]}>
                        <Input placeholder="my-api-server"/>
                    </Form.Item>
                    <Form.Item name="displayName" label="展示名" rules={[{required: true}]}>
                        <Input placeholder="我的 API 服务"/>
                    </Form.Item>
                    <Form.Item name="resourceUri" label="Resource URI" rules={[{required: true, type: 'url'}]}>
                        <Input placeholder="https://api.example.com"/>
                    </Form.Item>
                    <Space style={{display: 'flex'}} size="middle">
                        <Form.Item name="bizCode" label="业务域" rules={[{required: true}]} style={{flex: 1}}>
                            <Input placeholder="erp"/>
                        </Form.Item>
                        <Form.Item name="appCode" label="应用" rules={[{required: true}]} style={{flex: 1}}>
                            <Input placeholder="order-svc"/>
                        </Form.Item>
                        <Form.Item name="environment" label="环境" rules={[{required: true}]} style={{flex: 1}}>
                            <Input placeholder="prod"/>
                        </Form.Item>
                    </Space>
                    <Typography.Title level={5}>管理配置</Typography.Title>
                    <Form.Item name="managementClientId" label="管理 Client ID" rules={[{required: true}]}>
                        <Input placeholder="用于机器认证的 Client"/>
                    </Form.Item>
                    <Form.Item name="rbacApplicationCode" label="RBAC3 应用" rules={[{required: true}]}>
                        <Input placeholder="RBAC3 中的应用标识"/>
                    </Form.Item>
                    <Form.Item name="entryPermissionCode" label="入口权限" rules={[{required: true}]}>
                        <Input placeholder="app:resource:read"/>
                    </Form.Item>
                </Form>
            </Modal>
        </>
    )
}
