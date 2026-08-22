import {
    Button,
    Card,
    Descriptions,
    Drawer,
    Form,
    Input,
    message,
    Modal,
    Space,
    Table,
    Tag,
    Typography,
} from 'antd'
import {PlayCircleOutlined, PlusOutlined, ReloadOutlined, StopOutlined} from '@ant-design/icons'
import {useState} from 'react'
import {useMutation, useQuery, useQueryClient} from '@tanstack/react-query'
import {httpClient, useAuth} from '../../auth/AuthContext'
import {PageState, usePermission} from '@egon-cola/admin-web-shared'
import type {CreateResourceServerDTO, ResourceServerVO} from '../../api/types'

const STATUS_COLORS: Record<string, string> = {
    ACTIVE: 'green',
    DISABLED: 'default',
}

export const ResourceServerListPage = () => {
    const auth = useAuth()
    const queryClient = useQueryClient()
    const {has} = usePermission(auth.bootstrap?.permissions ?? [])
    const [createOpen, setCreateOpen] = useState(false)
    const [detailRs, setDetailRs] = useState<ResourceServerVO | null>(null)
    const [createForm] = Form.useForm()
    const [messageApi, contextHolder] = message.useMessage()

    const rsQuery = useQuery({
        queryKey: ['idp', 'resource-servers'],
        queryFn: () => httpClient.request<ResourceServerVO[]>('/api/v1/identity/resource-servers'),
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
            setDetailRs(result)
            await queryClient.invalidateQueries({queryKey: ['idp', 'resource-servers']})
            messageApi.success('已启用')
        },
        onError: (err) => {
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
            setDetailRs(result)
            await queryClient.invalidateQueries({queryKey: ['idp', 'resource-servers']})
            messageApi.success('已禁用')
        },
        onError: (err) => {
            messageApi.error(err instanceof Error ? err.message : '操作失败')
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
                <PageState
                    loading={rsQuery.isPending}
                    error={rsQuery.error}
                    empty={rsQuery.data?.length === 0}
                    emptyDescription="暂无 Resource Server"
                    onRetry={() => {void rsQuery.refetch()}}
                >
                    <Table<ResourceServerVO>
                        rowKey="resourceServerId"
                        dataSource={rsQuery.data ?? []}
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
                destroyOnClose
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
