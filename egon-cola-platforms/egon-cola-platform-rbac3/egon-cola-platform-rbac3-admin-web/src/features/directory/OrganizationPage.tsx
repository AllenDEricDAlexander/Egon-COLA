import {useQuery} from '@tanstack/react-query'
import {Button, Card, Descriptions, Drawer, Form, Input, Space, Table, Tag, Typography} from 'antd'
import {useState} from 'react'
import {useSearchParams} from 'react-router-dom'
import {useRbac3Authorization} from '@egon-cola/rbac3-react-sdk'
import {PageState} from '@egon-cola/admin-web-shared'
import {useFeatureApi, useFeatureTenantContext} from '../shared/FeatureApi'
import {directoryApi, type OrganizationView} from './directory.api'

export const OrganizationPage = () => {
    const {status} = useRbac3Authorization()
    const {effectiveTenantId} = useFeatureTenantContext()
    const api = directoryApi(useFeatureApi())
    const [searchParams, setSearchParams] = useSearchParams()
    const [selectedOrganization, setSelectedOrganization] = useState<OrganizationView | null>(null)
    const [filterForm] = Form.useForm<{parentId?: string}>()
    const parentId = searchParams.get('parentId')?.trim() || undefined
    const tenant = effectiveTenantId ?? 'none'
    const query = useQuery({
        queryKey: ['rbac3', 'organizations', tenant, parentId ?? 'root'],
        queryFn: () => api.organizations(parentId),
        enabled: status === 'READY',
    })

    return (
        <Card
            title="组织"
            extra={<Button onClick={() => {void query.refetch()}}>刷新</Button>}
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
                    <Input allowClear placeholder="留空查看全部组织"/>
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
                    onRow={(organization) => ({
                        onClick: () => setSelectedOrganization(organization),
                        style: {cursor: 'pointer'},
                    })}
                    columns={[
                        {title: '组织名称', dataIndex: 'name'},
                        {title: '组织编码', dataIndex: 'code'},
                        {title: '类型', dataIndex: 'type'},
                        {title: '路径', dataIndex: 'path'},
                        {title: '层级', dataIndex: 'depth'},
                        {title: '状态', dataIndex: 'status', render: (value: string) => <Tag>{value}</Tag>},
                        {
                            title: '操作',
                            render: (_value: unknown, organization: OrganizationView) => (
                                <Button size="small" onClick={() => setSelectedOrganization(organization)}>查看详情</Button>
                            ),
                        },
                    ]}
                />
            </PageState>
            <Drawer
                title={selectedOrganization ? `组织：${selectedOrganization.name}` : ''}
                open={selectedOrganization !== null}
                onClose={() => setSelectedOrganization(null)}
                width={520}
            >
                {selectedOrganization && (
                    <Descriptions bordered column={1} size="small">
                        <Descriptions.Item label="组织 ID">{selectedOrganization.orgUnitId}</Descriptions.Item>
                        <Descriptions.Item label="组织编码">{selectedOrganization.code}</Descriptions.Item>
                        <Descriptions.Item label="名称">{selectedOrganization.name}</Descriptions.Item>
                        <Descriptions.Item label="父组织 ID">{selectedOrganization.parentId ?? '根组织'}</Descriptions.Item>
                        <Descriptions.Item label="路径">{selectedOrganization.path}</Descriptions.Item>
                        <Descriptions.Item label="来源快照">{selectedOrganization.snapshotId ?? '手工维护'}</Descriptions.Item>
                        <Descriptions.Item label="状态">{selectedOrganization.status}</Descriptions.Item>
                    </Descriptions>
                )}
            </Drawer>
        </Card>
    )
}
