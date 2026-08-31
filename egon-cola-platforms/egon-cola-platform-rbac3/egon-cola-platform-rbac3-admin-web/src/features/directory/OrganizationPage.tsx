import {PermissionGuard, useRbac3Authorization} from '@egon-cola/rbac3-react-sdk'
import {useMutation, useQuery, useQueryClient} from '@tanstack/react-query'
import {Alert, Button, Card, Descriptions, Drawer, Form, Input, Popconfirm, Select, Space, Table, Tag, Typography} from 'antd'
import {useState} from 'react'
import {useSearchParams} from 'react-router-dom'
import {PageState} from '@egon-cola/admin-web-shared'
import {useFeatureApi, useFeatureTenantContext} from '../shared/FeatureApi'
import {directoryApi, type OrganizationCommand, type OrganizationUpdateCommand, type OrganizationView} from './directory.api'

interface OrganizationFormValues {
    readonly type: string
    readonly code: string
    readonly name: string
    readonly parentId?: string
    readonly externalId?: string
    readonly validFrom: string
    readonly validTo?: string
}

const organizationTypes = [
    {value: 'ORG', label: '组织'},
    {value: 'DEPT', label: '部门'},
]

const normalizeNullable = (value?: string) => value?.trim() || null

export const OrganizationPage = () => {
    const {status} = useRbac3Authorization()
    const {effectiveTenantId} = useFeatureTenantContext()
    const api = directoryApi(useFeatureApi())
    const queryClient = useQueryClient()
    const [searchParams, setSearchParams] = useSearchParams()
    const [selectedOrganization, setSelectedOrganization] = useState<OrganizationView | null>(null)
    const [editingOrganization, setEditingOrganization] = useState<OrganizationView | null>(null)
    const [editorOpen, setEditorOpen] = useState(false)
    const [filterForm] = Form.useForm<{parentId?: string}>()
    const [editorForm] = Form.useForm<OrganizationFormValues>()
    const parentId = searchParams.get('parentId')?.trim() || undefined
    const tenant = effectiveTenantId ?? 'none'
    const queryKey = ['rbac3', 'organizations', tenant, parentId ?? 'root']
    const query = useQuery({
        queryKey,
        queryFn: () => api.organizations(parentId),
        enabled: status === 'READY',
    })
    const closeEditor = () => {
        setEditorOpen(false)
        setEditingOrganization(null)
        setSelectedOrganization(null)
        editorForm.resetFields()
    }
    const openCreate = () => {
        setSelectedOrganization(null)
        setEditingOrganization(null)
        editorForm.resetFields()
        editorForm.setFieldsValue({type: 'DEPT', validFrom: new Date().toISOString()})
        setEditorOpen(true)
    }
    const openDetails = (organization: OrganizationView) => {
        setSelectedOrganization(organization)
        setEditingOrganization(null)
        setEditorOpen(false)
    }
    const openEdit = (organization: OrganizationView) => {
        setSelectedOrganization(organization)
        setEditingOrganization(organization)
        editorForm.setFieldsValue({
            type: organization.type,
            code: organization.code,
            name: organization.name,
            parentId: organization.parentId ?? '',
            externalId: organization.externalId ?? '',
            validFrom: organization.validFrom ?? '',
            validTo: organization.validTo ?? '',
        })
        setEditorOpen(true)
    }
    const save = useMutation({
        mutationFn: (values: OrganizationFormValues) => {
            if (editingOrganization) {
                const command: OrganizationUpdateCommand = {
                    type: values.type,
                    name: values.name.trim(),
                    parentId: normalizeNullable(values.parentId),
                    externalId: normalizeNullable(values.externalId),
                    validFrom: values.validFrom.trim(),
                    validTo: normalizeNullable(values.validTo),
                    expectedVersion: editingOrganization.version,
                }
                return api.updateOrganization(editingOrganization.orgUnitId, command)
            }
            const command: OrganizationCommand = {
                type: values.type,
                code: values.code.trim(),
                name: values.name.trim(),
                parentId: normalizeNullable(values.parentId),
                externalId: normalizeNullable(values.externalId),
                validFrom: values.validFrom.trim(),
                validTo: normalizeNullable(values.validTo),
            }
            return api.createOrganization(command)
        },
        onSuccess: async () => {
            closeEditor()
            await queryClient.invalidateQueries({queryKey: ['rbac3', 'organizations']})
        },
    })
    const archive = useMutation({
        mutationFn: (organization: OrganizationView) => api.deleteOrganization(
            organization.orgUnitId,
            organization.version,
        ),
        onSuccess: async () => {
            closeEditor()
            await queryClient.invalidateQueries({queryKey: ['rbac3', 'organizations']})
        },
    })
    const mutationError = save.error ?? archive.error

    return (
        <Card
            title="组织"
            extra={(
                <Space>
                    <Button onClick={() => {void query.refetch()}}>刷新</Button>
                    <PermissionGuard permission="system:organization:manage">
                        <Button type="primary" onClick={openCreate}>新增组织</Button>
                    </PermissionGuard>
                </Space>
            )}
        >
            <Typography.Paragraph type="secondary">
                维护 RBAC3 手工组织层级；租户范围由当前授权上下文决定，组织节点 ID 始终按字符串展示。
            </Typography.Paragraph>
            <Form
                form={filterForm}
                layout="inline"
                initialValues={{parentId}}
                onFinish={(values) => {
                    const nextParentId = values.parentId?.trim()
                    setSearchParams(nextParentId ? {parentId: nextParentId} : {})
                }}
                style={{marginBottom: 16}}
            >
                <Form.Item name="parentId" label="上级组织 ID">
                    <Input allowClear placeholder="留空查看全部组织" />
                </Form.Item>
                <Form.Item>
                    <Space>
                        <Button type="primary" htmlType="submit">查询</Button>
                        <Button onClick={() => {
                            filterForm.resetFields()
                            setSearchParams({})
                        }}>重置</Button>
                    </Space>
                </Form.Item>
            </Form>
            {mutationError && (
                <Alert
                    type="error"
                    showIcon
                    title={mutationError instanceof Error ? mutationError.message : String(mutationError)}
                    action={<Button size="small" onClick={() => {void query.refetch()}}>刷新</Button>}
                    style={{marginBottom: 16}}
                />
            )}
            <PageState
                loading={query.isPending}
                error={query.error}
                empty={query.data?.length === 0}
                emptyDescription="暂无组织"
                onRetry={() => {void query.refetch()}}
            >
                <Table<OrganizationView>
                    rowKey="orgUnitId"
                    dataSource={query.data ?? []}
                    pagination={false}
                    scroll={{x: 'max-content'}}
                    onRow={(organization) => ({
                        onClick: () => openDetails(organization),
                        style: {cursor: 'pointer'},
                    })}
                    columns={[
                        {title: '组织名称', dataIndex: 'name'},
                        {title: '组织编码', dataIndex: 'code'},
                        {title: '类型', dataIndex: 'type'},
                        {title: '路径', dataIndex: 'path'},
                        {title: '层级', dataIndex: 'depth'},
                        {title: '状态', dataIndex: 'status', render: (value: string) => <Tag>{value}</Tag>},
                        {title: '版本', dataIndex: 'version'},
                        {
                            title: '操作',
                            render: (_value: unknown, organization: OrganizationView) => (
                                <Button size="small" onClick={(event) => {
                                    event.stopPropagation()
                                    openDetails(organization)
                                }}>查看详情</Button>
                            ),
                        },
                    ]}
                />
            </PageState>
            <Drawer
                title={editingOrganization ? `编辑组织：${editingOrganization.name}` : selectedOrganization ? `组织：${selectedOrganization.name}` : '新增组织'}
                open={editorOpen || selectedOrganization !== null}
                onClose={closeEditor}
                size="large"
            >
                {editingOrganization || editorOpen ? (
                    <Form form={editorForm} layout="vertical" onFinish={(values) => save.mutate(values)}>
                        <Form.Item name="type" label="组织类型" rules={[{required: true}]}>
                            <Select options={organizationTypes} />
                        </Form.Item>
                        <Form.Item name="code" label="组织编码" rules={[{required: !editingOrganization, whitespace: true}]}>
                            <Input disabled={editingOrganization !== null} />
                        </Form.Item>
                        <Form.Item name="name" label="组织名称" rules={[{required: true, whitespace: true}]}>
                            <Input />
                        </Form.Item>
                        <Form.Item name="parentId" label="上级组织 ID">
                            <Input allowClear />
                        </Form.Item>
                        <Form.Item name="externalId" label="外部 ID">
                            <Input allowClear />
                        </Form.Item>
                        <Form.Item name="validFrom" label="生效时间" rules={[{required: true, whitespace: true}]}>
                            <Input placeholder="2026-08-29T00:00:00Z" />
                        </Form.Item>
                        <Form.Item name="validTo" label="失效时间">
                            <Input placeholder="留空表示长期有效" />
                        </Form.Item>
                        <Space>
                            <Button onClick={closeEditor}>关闭编辑</Button>
                            <Button type="primary" htmlType="submit" loading={save.isPending}>保存</Button>
                        </Space>
                    </Form>
                ) : selectedOrganization ? (
                    <Space direction="vertical" size="middle" style={{width: '100%'}}>
                        <Descriptions bordered column={1} size="small">
                            <Descriptions.Item label="组织 ID">{selectedOrganization.orgUnitId}</Descriptions.Item>
                            <Descriptions.Item label="组织编码">{selectedOrganization.code}</Descriptions.Item>
                            <Descriptions.Item label="名称">{selectedOrganization.name}</Descriptions.Item>
                            <Descriptions.Item label="父组织 ID">{selectedOrganization.parentId ?? '根组织'}</Descriptions.Item>
                            <Descriptions.Item label="路径">{selectedOrganization.path}</Descriptions.Item>
                            <Descriptions.Item label="来源快照">{selectedOrganization.snapshotId ?? '手工维护'}</Descriptions.Item>
                            <Descriptions.Item label="状态">{selectedOrganization.status}</Descriptions.Item>
                            <Descriptions.Item label="版本">{selectedOrganization.version}</Descriptions.Item>
                        </Descriptions>
                        <PermissionGuard permission="system:organization:manage">
                            <Space>
                                {selectedOrganization.snapshotId === null && (
                                    <Button onClick={() => openEdit(selectedOrganization)}>编辑</Button>
                                )}
                                {selectedOrganization.snapshotId === null && selectedOrganization.status === 'ACTIVE' && (
                                    <Popconfirm
                                        title="确认停用该手工组织？"
                                        okText="确认停用组织"
                                        cancelText="取消"
                                        onConfirm={() => archive.mutate(selectedOrganization)}
                                    >
                                        <Button danger loading={archive.isPending}>归档</Button>
                                    </Popconfirm>
                                )}
                            </Space>
                        </PermissionGuard>
                    </Space>
                ) : null}
            </Drawer>
        </Card>
    )
}
