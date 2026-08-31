import {PermissionGuard, useRbac3Authorization} from '@egon-cola/rbac3-react-sdk'
import {useMutation, useQuery, useQueryClient} from '@tanstack/react-query'
import {Alert, Button, Card, Descriptions, Drawer, Form, Input, Popconfirm, Space, Table, Tag, Typography} from 'antd'
import {useState} from 'react'
import {useSearchParams} from 'react-router-dom'
import {PageState} from '@egon-cola/admin-web-shared'
import {useFeatureApi, useFeatureTenantContext} from '../shared/FeatureApi'
import {directoryApi, type PositionCommand, type PositionUpdateCommand, type PositionView} from './directory.api'

interface PositionFormValues {
    readonly code: string
    readonly name: string
    readonly orgUnitId: string
    readonly externalId?: string
    readonly validFrom: string
    readonly validTo?: string
}

const normalizeNullable = (value?: string) => value?.trim() || null

export const PositionPage = () => {
    const {status} = useRbac3Authorization()
    const {effectiveTenantId} = useFeatureTenantContext()
    const api = directoryApi(useFeatureApi())
    const queryClient = useQueryClient()
    const [searchParams, setSearchParams] = useSearchParams()
    const [selectedPosition, setSelectedPosition] = useState<PositionView | null>(null)
    const [editingPosition, setEditingPosition] = useState<PositionView | null>(null)
    const [editorOpen, setEditorOpen] = useState(false)
    const [filterForm] = Form.useForm<{orgUnitId?: string}>()
    const [editorForm] = Form.useForm<PositionFormValues>()
    const orgUnitId = searchParams.get('orgUnitId')?.trim() || undefined
    const tenant = effectiveTenantId ?? 'none'
    const queryKey = ['rbac3', 'positions', tenant, orgUnitId ?? 'all']
    const query = useQuery({
        queryKey,
        queryFn: () => api.positions(orgUnitId),
        enabled: status === 'READY',
    })
    const closeEditor = () => {
        setEditorOpen(false)
        setEditingPosition(null)
        setSelectedPosition(null)
        editorForm.resetFields()
    }
    const openCreate = () => {
        setSelectedPosition(null)
        setEditingPosition(null)
        editorForm.resetFields()
        editorForm.setFieldsValue({orgUnitId: orgUnitId ?? '', validFrom: new Date().toISOString()})
        setEditorOpen(true)
    }
    const openDetails = (position: PositionView) => {
        setSelectedPosition(position)
        setEditingPosition(null)
        setEditorOpen(false)
    }
    const openEdit = (position: PositionView) => {
        setSelectedPosition(position)
        setEditingPosition(position)
        editorForm.setFieldsValue({
            code: position.code,
            name: position.name,
            orgUnitId: position.orgUnitId,
            externalId: position.externalId ?? '',
            validFrom: position.validFrom,
            validTo: position.validTo ?? '',
        })
        setEditorOpen(true)
    }
    const save = useMutation({
        mutationFn: (values: PositionFormValues) => {
            if (editingPosition) {
                const command: PositionUpdateCommand = {
                    name: values.name.trim(),
                    orgUnitId: values.orgUnitId.trim(),
                    externalId: normalizeNullable(values.externalId),
                    validFrom: values.validFrom.trim(),
                    validTo: normalizeNullable(values.validTo),
                    expectedVersion: editingPosition.version,
                }
                return api.updatePosition(editingPosition.positionId, command)
            }
            const command: PositionCommand = {
                code: values.code.trim(),
                name: values.name.trim(),
                orgUnitId: values.orgUnitId.trim(),
                externalId: normalizeNullable(values.externalId),
                validFrom: values.validFrom.trim(),
                validTo: normalizeNullable(values.validTo),
            }
            return api.createPosition(command)
        },
        onSuccess: async () => {
            closeEditor()
            await queryClient.invalidateQueries({queryKey: ['rbac3', 'positions']})
        },
    })
    const archive = useMutation({
        mutationFn: (position: PositionView) => api.deletePosition(position.positionId, position.version),
        onSuccess: async () => {
            closeEditor()
            await queryClient.invalidateQueries({queryKey: ['rbac3', 'positions']})
        },
    })
    const mutationError = save.error ?? archive.error

    return (
        <Card
            title="岗位"
            extra={(
                <Space>
                    <Button onClick={() => {void query.refetch()}}>刷新</Button>
                    <PermissionGuard permission="system:position:manage">
                        <Button type="primary" onClick={openCreate}>新增岗位</Button>
                    </PermissionGuard>
                </Space>
            )}
        >
            <Typography.Paragraph type="secondary">
                维护 RBAC3 手工岗位；每个岗位必须绑定有效组织，租户和授权范围由当前上下文确定。
            </Typography.Paragraph>
            <Form
                form={filterForm}
                layout="inline"
                initialValues={{orgUnitId}}
                onFinish={(values) => {
                    const nextOrgUnitId = values.orgUnitId?.trim()
                    setSearchParams(nextOrgUnitId ? {orgUnitId: nextOrgUnitId} : {})
                }}
                style={{marginBottom: 16}}
            >
                <Form.Item name="orgUnitId" label="组织 ID">
                    <Input allowClear placeholder="留空查看全部岗位" />
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
                emptyDescription="暂无岗位"
                onRetry={() => {void query.refetch()}}
            >
                <Table<PositionView>
                    rowKey="positionId"
                    dataSource={query.data ?? []}
                    pagination={false}
                    scroll={{x: 'max-content'}}
                    onRow={(position) => ({
                        onClick: () => openDetails(position),
                        style: {cursor: 'pointer'},
                    })}
                    columns={[
                        {title: '岗位名称', dataIndex: 'name'},
                        {title: '岗位编码', dataIndex: 'code'},
                        {title: '组织 ID', dataIndex: 'orgUnitId'},
                        {title: '状态', dataIndex: 'status', render: (value: string) => <Tag>{value}</Tag>},
                        {title: '版本', dataIndex: 'version'},
                        {
                            title: '操作',
                            render: (_value: unknown, position: PositionView) => (
                                <Button size="small" onClick={(event) => {
                                    event.stopPropagation()
                                    openDetails(position)
                                }}>查看详情</Button>
                            ),
                        },
                    ]}
                />
            </PageState>
            <Drawer
                title={editingPosition ? `编辑岗位：${editingPosition.name}` : selectedPosition ? `岗位：${selectedPosition.name}` : '新增岗位'}
                open={editorOpen || selectedPosition !== null}
                onClose={closeEditor}
                size="large"
            >
                {editingPosition || editorOpen ? (
                    <Form form={editorForm} layout="vertical" onFinish={(values) => save.mutate(values)}>
                        <Form.Item name="code" label="岗位编码" rules={[{required: !editingPosition, whitespace: true}]}>
                            <Input disabled={editingPosition !== null} />
                        </Form.Item>
                        <Form.Item name="name" label="岗位名称" rules={[{required: true, whitespace: true}]}>
                            <Input />
                        </Form.Item>
                        <Form.Item name="orgUnitId" label="岗位归属组织 ID" rules={[{required: true, whitespace: true}]}>
                            <Input aria-label="岗位归属组织 ID" />
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
                ) : selectedPosition ? (
                    <Space direction="vertical" size="middle" style={{width: '100%'}}>
                        <Descriptions bordered column={1} size="small">
                            <Descriptions.Item label="岗位 ID">{selectedPosition.positionId}</Descriptions.Item>
                            <Descriptions.Item label="岗位编码">{selectedPosition.code}</Descriptions.Item>
                            <Descriptions.Item label="名称">{selectedPosition.name}</Descriptions.Item>
                            <Descriptions.Item label="组织 ID">{selectedPosition.orgUnitId}</Descriptions.Item>
                            <Descriptions.Item label="来源快照">{selectedPosition.snapshotId ?? '手工维护'}</Descriptions.Item>
                            <Descriptions.Item label="状态">{selectedPosition.status}</Descriptions.Item>
                            <Descriptions.Item label="版本">{selectedPosition.version}</Descriptions.Item>
                        </Descriptions>
                        <PermissionGuard permission="system:position:manage">
                            <Space>
                                {selectedPosition.snapshotId === null && (
                                    <Button onClick={() => openEdit(selectedPosition)}>编辑</Button>
                                )}
                                {selectedPosition.snapshotId === null && selectedPosition.status === 'ACTIVE' && (
                                    <Popconfirm
                                        title="确认停用该手工岗位？"
                                        okText="确认停用岗位"
                                        cancelText="取消"
                                        onConfirm={() => archive.mutate(selectedPosition)}
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
