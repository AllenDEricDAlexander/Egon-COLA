import { DownOutlined } from '@ant-design/icons'
import {
  keepPreviousData,
  useMutation,
  useQuery,
  useQueryClient,
} from '@tanstack/react-query'
import { PageState } from '@egon-cola/admin-web-shared'
import {
  App,
  Button,
  Card,
  Drawer,
  Dropdown,
  Grid,
  Space,
  Table,
  Tag,
  Typography,
} from 'antd'
import type { MenuProps, TableColumnsType } from 'antd'
import { useMemo, useState } from 'react'
import { ddcApi, ddcPageApi } from '../api/client'
import type {
  DdcConfig,
  DdcConfigVersion,
  DdcPublishResult,
} from '../api/types'
import AdminPageHeader from '../components/page/AdminPageHeader'
import ScopeSelects, { type ScopeValue } from '../components/scope/ScopeSelects'
import { usePageState } from '../hooks/usePageState'
import { buildQuery, formatTime } from '../lib/query'
import { uuidV7 } from '../lib/uuid'
import ConfigEditorDialog, { type ConfigScope } from './ConfigEditorDialog'

const emptyScope: ScopeValue = {
  bizCode: '',
  namespaceCode: '',
  env: '',
  appCode: '',
}

export default function ConfigsPage() {
  const { message, modal } = App.useApp()
  const screens = Grid.useBreakpoint()
  const queryClient = useQueryClient()
  const configPage = usePageState()
  const versionPage = usePageState()
  const [draftScope, setDraftScope] = useState<ScopeValue>({ ...emptyScope })
  const [submittedScope, setSubmittedScope] = useState<ScopeValue>({
    ...emptyScope,
  })
  const [dialogOpen, setDialogOpen] = useState(false)
  const [editingConfig, setEditingConfig] = useState<DdcConfig | null>(null)
  const [versionsConfig, setVersionsConfig] = useState<DdcConfig | null>(null)

  const configsQuery = useQuery({
    queryKey: ['ddc', 'configs', submittedScope, configPage.page],
    queryFn: ({ signal }) => ddcPageApi<DdcConfig>(
      `/api/v1/ddc/configs/page?${buildQuery({
        ...submittedScope,
        includeDeleted: false,
        pageNo: configPage.page.pageNo,
        pageSize: configPage.page.pageSize,
      })}`,
      { signal },
    ),
    placeholderData: keepPreviousData,
    staleTime: 0,
  })

  const versionsQuery = useQuery({
    enabled: versionsConfig !== null,
    queryKey: [
      'ddc',
      'config-versions',
      versionsConfig?.id,
      versionPage.page,
    ],
    queryFn: ({ signal }) => ddcPageApi<DdcConfigVersion>(
      `/api/v1/ddc/configs/${encodeURIComponent(versionsConfig!.id)}/versions/page?${buildQuery({
        pageNo: versionPage.page.pageNo,
        pageSize: versionPage.page.pageSize,
      })}`,
      { signal },
    ),
    placeholderData: keepPreviousData,
    staleTime: 0,
  })

  const invalidateConfigs = () => queryClient.invalidateQueries({
    queryKey: ['ddc', 'configs'],
  })

  const publishMutation = useMutation({
    mutationFn: (config: DdcConfig) => ddcApi<DdcPublishResult>(
      `/api/v1/ddc/configs/${encodeURIComponent(config.id)}/publish`,
      {
        method: 'POST',
        body: {
          changeId: uuidV7(),
          content: config.content,
          expectedVersion: config.currentVersion,
          timeoutMs: 30000,
        },
      },
    ),
    onSuccess: async (result) => {
      message.success(`发布任务 ${result.changeId}：${result.status}`)
      await invalidateConfigs()
    },
    onError: (error) => message.error(
      error instanceof Error ? error.message : String(error),
    ),
  })

  const deleteMutation = useMutation({
    mutationFn: (config: DdcConfig) => ddcApi(
      `/api/v1/ddc/configs/${encodeURIComponent(config.id)}`,
      { method: 'DELETE' },
    ),
    onSuccess: async () => {
      if ((configsQuery.data?.records.length ?? 0) === 1
          && configPage.page.pageNo > 1) {
        configPage.onTableChange(
          configPage.page.pageNo - 1,
          configPage.page.pageSize,
        )
      }
      message.success('配置已删除')
      await invalidateConfigs()
    },
    onError: (error) => message.error(
      error instanceof Error ? error.message : String(error),
    ),
  })

  const rollbackMutation = useMutation({
    mutationFn: ({ config, version }: {
      config: DdcConfig
      version: DdcConfigVersion
    }) => ddcApi(
      `/api/v1/ddc/configs/${encodeURIComponent(config.id)}/rollback`,
      {
        method: 'POST',
        body: {
          configId: config.id,
          version: version.version,
          reason: 'rollback from DDC Admin Web',
        },
      },
    ),
    onSuccess: async () => {
      message.success('已回滚')
      closeVersions()
      await invalidateConfigs()
    },
    onError: (error) => message.error(
      error instanceof Error ? error.message : String(error),
    ),
  })

  const applyFilter = () => {
    setSubmittedScope({ ...draftScope })
    configPage.resetPage()
  }

  const resetFilter = () => {
    setDraftScope({ ...emptyScope })
    setSubmittedScope({ ...emptyScope })
    configPage.resetPage()
  }

  const openNewDialog = () => {
    setEditingConfig(null)
    setDialogOpen(true)
  }

  const openEditDialog = (config: DdcConfig) => {
    setEditingConfig(config)
    setDialogOpen(true)
  }

  const openVersions = (config: DdcConfig) => {
    versionPage.resetPage()
    setVersionsConfig(config)
  }

  const closeVersions = () => {
    setVersionsConfig(null)
    versionPage.onTableChange(1, 10)
  }

  const confirmPublish = (config: DdcConfig) => {
    modal.confirm({
      title: `确认发布 ${config.resourceName} 当前版本？`,
      okText: '发布',
      onOk: () => publishMutation.mutateAsync(config),
    })
  }

  const confirmDelete = (config: DdcConfig) => {
    modal.confirm({
      title: `确认删除 ${config.resourceName}？`,
      okText: '删除',
      okButtonProps: { danger: true },
      onOk: () => deleteMutation.mutateAsync(config),
    })
  }

  const confirmRollback = (
    config: DdcConfig,
    version: DdcConfigVersion,
  ) => {
    modal.confirm({
      title: `确认回滚到版本 ${version.version}？`,
      okText: '回滚',
      onOk: () => rollbackMutation.mutateAsync({ config, version }),
    })
  }

  const defaultScope: ConfigScope = useMemo(() => ({
    ...draftScope,
  }), [draftScope])

  const configColumns: TableColumnsType<DdcConfig> = [
    {
      title: '配置文件',
      dataIndex: 'resourceName',
      key: 'resourceName',
      render: (value: string, row) => (
        <Space>
          <Typography.Text code>{value}</Typography.Text>
          <Tag color="blue">{row.format}</Tag>
        </Space>
      ),
    },
    {
      title: '可见命名空间',
      dataIndex: 'visibleNamespaces',
      key: 'visibleNamespaces',
      render: (values: string[] = []) => (
        <Space size={[4, 4]} wrap>
          {values.length === 0
            ? '—'
            : values.map((value) => <Tag key={value}>{value}</Tag>)}
        </Space>
      ),
    },
    {
      title: 'YAML 内容',
      key: 'value',
      width: 360,
      ellipsis: true,
      render: (_: unknown, row) => {
        const content = row.content.replace(/\s+/g, ' ').trim()
        return (
          <div style={{ maxWidth: 340 }}>
            <Typography.Text code ellipsis title={content}>
              {content}
            </Typography.Text>
            {row.description && (
              <Typography.Text
                type="secondary"
                ellipsis
                title={row.description}
                style={{ display: 'block', fontSize: 12 }}
              >
                {row.description}
              </Typography.Text>
            )}
          </div>
        )
      },
    },
    { title: '当前版本', dataIndex: 'currentVersion', key: 'currentVersion' },
    {
      title: '更新时间',
      dataIndex: 'updatedAt',
      key: 'updatedAt',
      render: formatTime,
    },
    {
      title: '操作',
      key: 'actions',
      fixed: 'right',
      render: (_: unknown, row) => {
        const items: MenuProps['items'] = [
          { key: 'versions', label: '查看版本' },
          { key: 'delete', label: '删除', danger: true },
        ]
        return (
          <Space.Compact>
            <Button size="small" onClick={() => openEditDialog(row)}>
              编辑
            </Button>
            <Button
              size="small"
              type="primary"
              loading={publishMutation.isPending
                && publishMutation.variables?.id === row.id}
              onClick={() => confirmPublish(row)}
            >
              发布
            </Button>
            <Dropdown
              trigger={['click']}
              menu={{
                items,
                onClick: ({ key }) => {
                  if (key === 'versions') openVersions(row)
                  if (key === 'delete') confirmDelete(row)
                },
              }}
            >
              <Button size="small">
                更多操作 <DownOutlined />
              </Button>
            </Dropdown>
          </Space.Compact>
        )
      },
    },
  ]

  const versionColumns: TableColumnsType<DdcConfigVersion> = [
    { title: '版本', dataIndex: 'version', key: 'version' },
    { title: '类型', dataIndex: 'changeType', key: 'changeType' },
    { title: '变更原因', dataIndex: 'changeReason', key: 'changeReason' },
    { title: '操作人', dataIndex: 'operator', key: 'operator' },
    {
      title: '时间',
      dataIndex: 'createdAt',
      key: 'createdAt',
      render: formatTime,
    },
    {
      title: '新值',
      dataIndex: 'newContent',
      key: 'newContent',
      render: (value: string) => (
        <Typography.Text code ellipsis style={{ maxWidth: 220 }}>
          {value?.replace(/\s+/g, ' ').slice(0, 60) ?? '—'}
        </Typography.Text>
      ),
    },
    {
      title: '操作',
      key: 'actions',
      fixed: 'right',
      render: (_: unknown, row) => versionsConfig && (
        <Button
          size="small"
          onClick={() => confirmRollback(versionsConfig, row)}
        >
          回滚
        </Button>
      ),
    },
  ]

  return (
    <div>
      <AdminPageHeader
        title="配置中心管理"
        description="维护 application.yml 配置、发布与版本历史。"
        extra={(
          <Button type="primary" onClick={openNewDialog}>
            新建 application.yml
          </Button>
        )}
      />
      <Card size="small" style={{ marginBottom: 16 }}>
        <Space wrap>
          <ScopeSelects
            value={draftScope}
            onChange={setDraftScope}
          />
          <Button type="primary" onClick={applyFilter}>查询</Button>
          <Button onClick={resetFilter}>重置</Button>
        </Space>
      </Card>
      <Card size="small" title="配置列表">
        <PageState
          loading={configsQuery.isPending}
          error={configsQuery.error}
          empty={(configsQuery.data?.records.length ?? 0) === 0}
          onRetry={() => { void configsQuery.refetch() }}
        >
          <Table<DdcConfig>
            rowKey={(row) => row.id}
            columns={configColumns}
            dataSource={configsQuery.data?.records ?? []}
            loading={configsQuery.isFetching}
            size="small"
            scroll={{ x: 'max-content' }}
            pagination={{
              current: configsQuery.data?.page.pageNo
                ?? configPage.page.pageNo,
              pageSize: configsQuery.data?.page.pageSize
                ?? configPage.page.pageSize,
              total: configsQuery.data?.page.total ?? 0,
              showSizeChanger: true,
              pageSizeOptions: [10, 20, 50],
              showTotal: (total) => `共 ${total} 条`,
              onChange: configPage.onTableChange,
            }}
          />
        </PageState>
      </Card>
      <ConfigEditorDialog
        open={dialogOpen}
        config={editingConfig}
        defaultScope={defaultScope}
        onClose={() => setDialogOpen(false)}
        onSaved={() => setDialogOpen(false)}
      />
      <Drawer
        open={versionsConfig !== null}
        title="application.yml 版本历史"
        onClose={closeVersions}
        size={screens.md ? 860 : '100%'}
      >
        <PageState
          loading={versionsQuery.isPending}
          error={versionsQuery.error}
          empty={(versionsQuery.data?.records.length ?? 0) === 0}
          onRetry={() => { void versionsQuery.refetch() }}
        >
          <Table<DdcConfigVersion>
            rowKey={(row) => row.id}
            dataSource={versionsQuery.data?.records ?? []}
            loading={versionsQuery.isFetching}
            size="small"
            scroll={{ x: 'max-content' }}
            columns={versionColumns}
            pagination={{
              current: versionsQuery.data?.page.pageNo
                ?? versionPage.page.pageNo,
              pageSize: versionsQuery.data?.page.pageSize
                ?? versionPage.page.pageSize,
              total: versionsQuery.data?.page.total ?? 0,
              showSizeChanger: true,
              pageSizeOptions: [10, 20, 50],
              showTotal: (total) => `共 ${total} 条`,
              onChange: versionPage.onTableChange,
            }}
          />
        </PageState>
      </Drawer>
    </div>
  )
}
