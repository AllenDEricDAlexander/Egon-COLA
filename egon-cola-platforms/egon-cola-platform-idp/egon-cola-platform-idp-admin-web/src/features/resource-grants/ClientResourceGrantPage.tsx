import {
    Button,
    Card,
    Descriptions,
    Form,
    Input,
    message,
    Modal,
    Result,
    Select,
    Space,
    Table,
    Tag,
    Typography,
} from 'antd'
import {ArrowLeftOutlined, LinkOutlined, ReloadOutlined,} from '@ant-design/icons'
import {useState} from 'react'
import {useNavigate, useParams} from 'react-router-dom'
import {useMutation, useQuery, useQueryClient} from '@tanstack/react-query'
import {httpClient, useAuth} from '../../auth/AuthContext'
import {PageState, usePermission} from '@egon-cola/admin-web-shared'
import type {
    ClientResourceGrantVO,
    ResourceServerVO,
    UpsertClientResourceGrantDTO
} from '../../api/types'

const GRANT_TYPE_LABELS: Record<string, string> = {
    USER_DELEGATION: '用户委托 (USER_DELEGATION)',
    CLIENT_CREDENTIALS: '服务访问 (CLIENT_CREDENTIALS)',
}

export const ClientResourceGrantPage = () => {
    const {clientId} = useParams<{ clientId: string }>()
    const auth = useAuth()
    const navigate = useNavigate()
    const queryClient = useQueryClient()
    const {has} = usePermission(auth.bootstrap?.permissions ?? [])
    const [upsertOpen, setUpsertOpen] = useState(false)
    const [selectedRs, setSelectedRs] = useState<ResourceServerVO | null>(null)
    const [form] = Form.useForm()
    const [messageApi, contextHolder] = message.useMessage()

    const rsQuery = useQuery({
        queryKey: ['idp', 'resource-servers'],
        queryFn: () => httpClient.request<ResourceServerVO[]>('/api/v1/identity/resource-servers'),
    })

    const upsertMutation = useMutation({
        mutationFn: ({rsId, data}: { rsId: string; data: UpsertClientResourceGrantDTO }) =>
            httpClient.request<ClientResourceGrantVO>(
                `/api/v1/identity/clients/${encodeURIComponent(clientId!)}/resources/${encodeURIComponent(rsId)}`,
                {method: 'PUT', body: JSON.stringify(data)},
            ),
        onSuccess: async () => {
            setUpsertOpen(false)
            form.resetFields()
            await queryClient.invalidateQueries({queryKey: ['idp', 'resource-servers']})
            messageApi.success('Grant 已保存')
        },
        onError: (err) => {
            messageApi.error(err instanceof Error ? err.message : '操作失败')
        },
    })

    if (!clientId) {
        return <Result status="error" title="缺少 clientId 参数"
                       extra={<Button onClick={() => navigate('/clients')}>返回客户端列表</Button>}/>
    }

    const openUpsert = (rs: ResourceServerVO) => {
        setSelectedRs(rs)
        form.setFieldsValue({
            grantType: 'USER_DELEGATION',
            scopeContext: 'TENANT',
            allowedScopes: '',
            tenantId: '',
        })
        setUpsertOpen(true)
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
                        void rsQuery.refetch()
                    }}>刷新</Button>
                }
            >
                <Typography.Paragraph type="secondary" style={{marginBottom: 16}}>
                    为当前 Client 向 Resource Server 建立授权委托（Grant）。SERVICE Grant 必须显式选择 TENANT 或 PLATFORM 上下文。
                </Typography.Paragraph>
                <PageState
                    loading={rsQuery.isPending}
                    error={rsQuery.error}
                    empty={rsQuery.data?.length === 0}
                    emptyDescription="暂无 Resource Server，请先创建"
                    onRetry={() => {
                        void rsQuery.refetch()
                    }}
                >
                    <Table<ResourceServerVO>
                        rowKey="resourceServerId"
                        dataSource={rsQuery.data ?? []}
                        columns={[
                            {title: 'Resource Server', dataIndex: 'displayName'},
                            {title: 'ID', dataIndex: 'resourceServerId', ellipsis: true},
                            {title: '业务域', dataIndex: 'bizCode'},
                            {title: '应用', dataIndex: 'appCode'},
                            {title: '环境', dataIndex: 'environment'},
                            {
                                title: '状态',
                                dataIndex: 'status',
                                render: (v: string) => <Tag color={v === 'ACTIVE' ? 'green' : 'default'}>{v}</Tag>
                            },
                            {
                                title: '操作', width: 200,
                                render: (_: unknown, row: ResourceServerVO) => (
                                    <Space size="small">
                                        {has('idp:resource-server:grant') && (
                                            <Button size="small" icon={<LinkOutlined/>} onClick={() => openUpsert(row)}>新建/更新
                                                Grant</Button>
                                        )}
                                    </Space>
                                ),
                            },
                        ]}
                    />
                </PageState>
            </Card>

            {/* Upsert Grant Modal */}
            <Modal
                title={`${selectedRs ? `为 ${selectedRs.displayName} 配置 Grant` : ''}`}
                open={upsertOpen}
                width={520}
                confirmLoading={upsertMutation.isPending}
                onCancel={() => {
                    setUpsertOpen(false);
                    form.resetFields()
                }}
                onOk={() => {
                    void form.validateFields().then((v) => {
                        if (selectedRs) {
                            const scopesStr: string = v.allowedScopes || ''
                            const scopes = scopesStr ? scopesStr.split(/\s+/).filter(Boolean) : []
                            upsertMutation.mutate({
                                rsId: selectedRs.resourceServerId,
                                data: {
                                    grantType: v.grantType,
                                    scopeContext: v.grantType === 'CLIENT_CREDENTIALS' ? v.scopeContext : undefined,
                                    tenantId: v.grantType === 'CLIENT_CREDENTIALS' && v.scopeContext === 'TENANT'
                                        ? v.tenantId || undefined
                                        : undefined,
                                    allowedScopes: v.grantType === 'CLIENT_CREDENTIALS' ? scopes : [],
                                    expectedResourceVersion: selectedRs.version,
                                    expectedGrantVersion: v.expectedGrantVersion || undefined,
                                },
                            })
                        }
                    })
                }}
                destroyOnClose
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
                                    <Form.Item name="allowedScopes" label="允许的 Scope（空格分隔）" rules={[{required: true}]}>
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
