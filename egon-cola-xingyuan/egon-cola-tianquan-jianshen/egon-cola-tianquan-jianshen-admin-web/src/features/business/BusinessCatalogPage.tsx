import {useQuery} from '@tanstack/react-query'
import {Button, Card, Descriptions, Drawer, Input, Space, Table, Tag, Typography} from 'antd'
import {useState} from 'react'
import {useRbac3Authorization} from '@egon-cola/tianquan-jianshen-react-sdk'
import {PageState} from '@egon-cola/xingyuan-admin-web-shared'
import {useFeatureApi, useFeatureTenantContext} from '../shared/FeatureApi'
import {businessApi, type BusinessCatalogView} from './business.api'

export const BusinessCatalogPage = () => {
  const {status} = useRbac3Authorization()
  const {effectiveTenantId} = useFeatureTenantContext()
  const api = businessApi(useFeatureApi())
  const tenant = effectiveTenantId ?? 'none'
  const [draftKeyword, setDraftKeyword] = useState('')
  const [keyword, setKeyword] = useState('')
  const [selectedBusiness, setSelectedBusiness] = useState<BusinessCatalogView | null>(null)
  const businesses = useQuery({
    queryKey: ['rbac3', 'business-catalog', tenant, keyword],
    queryFn: () => api.businesses(keyword),
    enabled: status === 'READY',
  })
  const applications = useQuery({
    queryKey: ['rbac3', 'business-applications', tenant, selectedBusiness?.ddcBusinessId ?? 'none'],
    queryFn: () => api.applications(selectedBusiness!.ddcBusinessId),
    enabled: status === 'READY' && selectedBusiness !== null,
  })

  return (
    <Card title="业务目录">
      <Typography.Paragraph type="secondary">
        查看 DDC 注册的业务域及其应用目录；目录由平台侧投影，当前页面不直接访问 DDC。
      </Typography.Paragraph>
      <Space.Compact block style={{marginBottom: 16}}>
        <Input
          aria-label="业务目录搜索"
          placeholder="业务编码或名称"
          value={draftKeyword}
          onChange={(event) => setDraftKeyword(event.target.value)}
          onPressEnter={() => setKeyword(draftKeyword.trim())}
        />
        <Button type="primary" onClick={() => setKeyword(draftKeyword.trim())}>查询</Button>
        <Button onClick={() => {setDraftKeyword(''); setKeyword('')}}>重置</Button>
      </Space.Compact>
      <PageState
        loading={businesses.isPending}
        error={businesses.error}
        empty={businesses.data?.length === 0}
        emptyDescription="暂无业务目录"
        onRetry={() => {void businesses.refetch()}}
      >
        <Table<BusinessCatalogView>
          rowKey="ddcBusinessId"
          dataSource={businesses.data ?? []}
          pagination={false}
          scroll={{x: 'max-content'}}
          onRow={(business) => ({
            onClick: () => setSelectedBusiness(business),
            style: {cursor: 'pointer'},
          })}
          columns={[
            {title: '业务 ID', dataIndex: 'ddcBusinessId'},
            {title: '业务编码', dataIndex: 'bizCode'},
            {title: '业务名称', dataIndex: 'bizName'},
            {title: '状态', dataIndex: 'enabled', render: (value: boolean) => <Tag>{value ? '启用' : '停用'}</Tag>},
            {
              title: '操作',
              render: (_value: unknown, business: BusinessCatalogView) => (
                <Button size="small" onClick={(event) => {
                  event.stopPropagation()
                  setSelectedBusiness(business)
                }}>查看应用</Button>
              ),
            },
          ]}
        />
      </PageState>
      <Drawer
        title={selectedBusiness ? `业务：${selectedBusiness.bizName}` : ''}
        open={selectedBusiness !== null}
        onClose={() => setSelectedBusiness(null)}
        size="large"
      >
        {selectedBusiness && (
          <Space orientation="vertical" size="middle" style={{width: '100%'}}>
            <Descriptions bordered column={1} size="small">
              <Descriptions.Item label="业务 ID">{selectedBusiness.ddcBusinessId}</Descriptions.Item>
              <Descriptions.Item label="业务编码">{selectedBusiness.bizCode}</Descriptions.Item>
              <Descriptions.Item label="业务名称">{selectedBusiness.bizName}</Descriptions.Item>
              <Descriptions.Item label="状态">{selectedBusiness.enabled ? '启用' : '停用'}</Descriptions.Item>
            </Descriptions>
            <Typography.Title level={5}>业务下的应用</Typography.Title>
            <PageState
              loading={applications.isPending}
              error={applications.error}
              empty={applications.data?.length === 0}
              emptyDescription="该业务暂无应用"
              onRetry={() => {void applications.refetch()}}
            >
              <Table
                rowKey="ddcApplicationId"
                dataSource={applications.data ?? []}
                pagination={false}
                scroll={{x: 'max-content'}}
                columns={[
                  {title: '应用 ID', dataIndex: 'ddcApplicationId'},
                  {title: '应用编码', dataIndex: 'appCode'},
                  {title: '应用名称', dataIndex: 'appName'},
                  {title: '应用状态', dataIndex: 'applicationEnabled', render: (value: boolean) => <Tag>{value ? '启用' : '停用'}</Tag>},
                  {title: '业务状态', dataIndex: 'businessEnabled', render: (value: boolean) => <Tag>{value ? '启用' : '停用'}</Tag>},
                ]}
              />
            </PageState>
          </Space>
        )}
      </Drawer>
    </Card>
  )
}
