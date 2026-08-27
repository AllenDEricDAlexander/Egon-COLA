import {useQuery} from '@tanstack/react-query'
import {Button, Card, Descriptions, Drawer, Form, Input, Space, Table, Tag, Typography} from 'antd'
import {useState} from 'react'
import {useSearchParams} from 'react-router-dom'
import {useRbac3Authorization} from '@egon-cola/rbac3-react-sdk'
import {PageState} from '@egon-cola/admin-web-shared'
import {useFeatureApi, useFeatureTenantContext} from '../shared/FeatureApi'
import {directoryApi, type PositionView} from './directory.api'

export const PositionPage = () => {
    const {status} = useRbac3Authorization()
    const {effectiveTenantId} = useFeatureTenantContext()
    const api = directoryApi(useFeatureApi())
    const [searchParams, setSearchParams] = useSearchParams()
    const [selectedPosition, setSelectedPosition] = useState<PositionView | null>(null)
    const [filterForm] = Form.useForm<{orgUnitId?: string}>()
    const orgUnitId = searchParams.get('orgUnitId')?.trim() || undefined
    const tenant = effectiveTenantId ?? 'none'
    const query = useQuery({
        queryKey: ['rbac3', 'positions', tenant, orgUnitId ?? 'all'],
        queryFn: () => api.positions(orgUnitId),
        enabled: status === 'READY',
    })

    return (
        <Card
            title="岗位"
            extra={<Button onClick={() => {void query.refetch()}}>刷新</Button>}
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
                    <Input allowClear placeholder="留空查看全部岗位"/>
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
                emptyDescription="暂无岗位"
                onRetry={() => {void query.refetch()}}
            >
                <Table<PositionView>
                    rowKey="positionId"
                    dataSource={query.data ?? []}
                    pagination={false}
                    onRow={(position) => ({
                        onClick: () => setSelectedPosition(position),
                        style: {cursor: 'pointer'},
                    })}
                    columns={[
                        {title: '岗位名称', dataIndex: 'name'},
                        {title: '岗位编码', dataIndex: 'code'},
                        {title: '组织 ID', dataIndex: 'orgUnitId'},
                        {title: '状态', dataIndex: 'status', render: (value: string) => <Tag>{value}</Tag>},
                        {
                            title: '操作',
                            render: (_value: unknown, position: PositionView) => (
                                <Button size="small" onClick={() => setSelectedPosition(position)}>查看详情</Button>
                            ),
                        },
                    ]}
                />
            </PageState>
            <Drawer
                title={selectedPosition ? `岗位：${selectedPosition.name}` : ''}
                open={selectedPosition !== null}
                onClose={() => setSelectedPosition(null)}
                width={520}
            >
                {selectedPosition && (
                    <Descriptions bordered column={1} size="small">
                        <Descriptions.Item label="岗位 ID">{selectedPosition.positionId}</Descriptions.Item>
                        <Descriptions.Item label="岗位编码">{selectedPosition.code}</Descriptions.Item>
                        <Descriptions.Item label="名称">{selectedPosition.name}</Descriptions.Item>
                        <Descriptions.Item label="组织 ID">{selectedPosition.orgUnitId}</Descriptions.Item>
                        <Descriptions.Item label="来源快照">{selectedPosition.snapshotId ?? '手工维护'}</Descriptions.Item>
                        <Descriptions.Item label="状态">{selectedPosition.status}</Descriptions.Item>
                    </Descriptions>
                )}
            </Drawer>
        </Card>
    )
}
