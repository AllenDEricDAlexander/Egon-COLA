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
  Form,
  Grid,
  Input,
  Modal,
  Popconfirm,
  Select,
  Space,
  Switch,
  Table,
  Typography,
} from 'antd'
import { useState } from 'react'
import { ddcApi, ddcPageApi } from '../api/client'
import type {
  DdcApp,
  DdcEnv,
  DdcNamespace,
  DdcNamespaceEnvAppBinding,
} from '../api/types'
import AdminPageHeader from '../components/page/AdminPageHeader'
import BizSelect from '../components/scope/BizSelect'
import { scopeOptionQueryKey } from '../components/scope/useScopeOptions'
import { usePageState } from '../hooks/usePageState'
import { buildQuery, formatTime } from '../lib/query'

type NamespaceFilter = { bizCode: string; keyword: string }

const emptyFilter: NamespaceFilter = { bizCode: '', keyword: '' }

type NamespaceFormValues = {
  bizCode: string
  namespaceCode: string
  namespace: string
  description?: string
  enabled: boolean
}

export default function NamespacesPage() {
  const { message } = App.useApp()
  const screens = Grid.useBreakpoint()
  const queryClient = useQueryClient()
  const pageState = usePageState()
  const [draft, setDraft] = useState<NamespaceFilter>({ ...emptyFilter })
  const [submitted, setSubmitted] = useState<NamespaceFilter>({ ...emptyFilter })
  const [open, setOpen] = useState(false)
  const [editing, setEditing] = useState<DdcNamespace | null>(null)
  const [form] = Form.useForm<NamespaceFormValues>()
  const [bindingNamespace, setBindingNamespace] =
    useState<DdcNamespace | null>(null)
  const [bindingLoading, setBindingLoading] = useState(false)
  const [bindingSaving, setBindingSaving] = useState(false)
  const [bindings, setBindings] =
    useState<DdcNamespaceEnvAppBinding[]>([])
  const [bindingApps, setBindingApps] = useState<DdcApp[]>([])
  const [bindingEnvs, setBindingEnvs] = useState<DdcEnv[]>([])
  const [bindingDraft, setBindingDraft] =
    useState<Record<string, string[]>>({})

  const queryString = buildQuery({
    bizCode: submitted.bizCode,
    keyword: submitted.keyword,
    pageNo: pageState.page.pageNo,
    pageSize: pageState.page.pageSize,
  })
  const query = useQuery({
    queryKey: ['ddc', 'namespaces', submitted, pageState.page],
    queryFn: ({ signal }) => ddcPageApi<DdcNamespace>(
      `/api/v1/ddc/namespaces/page?${queryString}`,
      { signal },
    ),
    placeholderData: keepPreviousData,
    staleTime: 0,
  })

  const invalidate = async () => {
    await queryClient.invalidateQueries({ queryKey: ['ddc', 'namespaces'] })
    await queryClient.invalidateQueries({ queryKey: scopeOptionQueryKey })
  }

  const saveMutation = useMutation({
    mutationFn: ({ item, values }: {
      item: DdcNamespace | null
      values: NamespaceFormValues
    }) => item
      ? ddcApi(`/api/v1/ddc/namespaces/${encodeURIComponent(item.id)}`, {
        method: 'PUT',
        body: values,
      })
      : ddcApi('/api/v1/ddc/namespaces', { method: 'POST', body: values }),
    onSuccess: async () => {
      setOpen(false)
      await invalidate()
      message.success('命名空间保存成功')
    },
    onError: (error) => message.error(
      error instanceof Error ? error.message : String(error),
    ),
  })

  const toggleMutation = useMutation({
    mutationFn: ({ item, enabled }: {
      item: DdcNamespace
      enabled: boolean
    }) => ddcApi(
      `/api/v1/ddc/namespaces/${encodeURIComponent(item.id)}/enabled?enabled=${enabled}`,
      { method: 'PUT' },
    ),
    onSuccess: async () => {
      await invalidate()
      message.success('命名空间状态更新完成')
    },
    onError: (error) => message.error(
      error instanceof Error ? error.message : String(error),
    ),
  })

  const removeMutation = useMutation({
    mutationFn: (item: DdcNamespace) => ddcApi(
      `/api/v1/ddc/namespaces/${encodeURIComponent(item.id)}`,
      { method: 'DELETE' },
    ),
    onSuccess: async () => {
      if ((query.data?.records.length ?? 0) === 1
          && pageState.page.pageNo > 1) {
        pageState.onTableChange(
          pageState.page.pageNo - 1,
          pageState.page.pageSize,
        )
      }
      await invalidate()
      message.success('命名空间删除完成')
    },
    onError: (error) => message.error(
      error instanceof Error ? error.message : String(error),
    ),
  })

  const applyFilter = () => {
    setSubmitted({
      bizCode: draft.bizCode.trim(),
      keyword: draft.keyword.trim(),
    })
    pageState.resetPage()
  }

  const resetFilter = () => {
    setDraft({ ...emptyFilter })
    setSubmitted({ ...emptyFilter })
    pageState.resetPage()
  }

  const openCreate = () => {
    setEditing(null)
    form.resetFields()
    setOpen(true)
  }

  const openEdit = (item: DdcNamespace) => {
    setEditing(item)
    form.setFieldsValue({
      bizCode: item.bizCode,
      namespaceCode: item.namespaceCode,
      namespace: item.namespace,
      description: item.description ?? '',
      enabled: item.enabled,
    })
    setOpen(true)
  }

  const save = async () => {
    try {
      const values = await form.validateFields()
      saveMutation.mutate({ item: editing, values })
    } catch {
      return
    }
  }

  const closeBindings = () => {
    setBindingNamespace(null)
    setBindings([])
    setBindingApps([])
    setBindingEnvs([])
    setBindingDraft({})
  }

  const openBindings = async (item: DdcNamespace) => {
    setBindingNamespace(item)
    setBindingLoading(true)
    try {
      const params = new URLSearchParams({
        bizCode: item.bizCode,
        namespaceCode: item.namespaceCode,
      })
      const [currentBindings, envs, apps] = await Promise.all([
        ddcApi<DdcNamespaceEnvAppBinding[]>(
          `/api/v1/ddc/namespace-env-app-bindings?${params.toString()}`,
        ),
        ddcApi<DdcEnv[]>('/api/v1/ddc/envs'),
        ddcApi<DdcApp[]>(
          `/api/v1/ddc/apps?bizCode=${encodeURIComponent(item.bizCode)}`,
        ),
      ])
      const nextDraft: Record<string, string[]> = {}
      envs.forEach((env) => {
        nextDraft[env.envCode] = currentBindings
          .filter((binding) => binding.enabled && binding.env === env.envCode)
          .map((binding) => binding.appCode)
      })
      setBindings(currentBindings)
      setBindingEnvs(envs)
      setBindingApps(apps)
      setBindingDraft(nextDraft)
    } catch (error) {
      closeBindings()
      message.error(error instanceof Error ? error.message : String(error))
    } finally {
      setBindingLoading(false)
    }
  }

  const saveBindings = async () => {
    if (!bindingNamespace) return
    setBindingSaving(true)
    try {
      const changes: Promise<unknown>[] = []
      bindingEnvs.forEach((env) => {
        const desired = new Set(bindingDraft[env.envCode] ?? [])
        const current = bindings.filter((binding) => binding.env === env.envCode)
        desired.forEach((appCode) => {
          const existing = current.find((binding) => binding.appCode === appCode)
          const body = {
            bizCode: bindingNamespace.bizCode,
            namespaceCode: bindingNamespace.namespaceCode,
            env: env.envCode,
            appCode,
            enabled: true,
          }
          if (!existing) {
            changes.push(ddcApi('/api/v1/ddc/namespace-env-app-bindings', {
              method: 'POST',
              body,
            }))
          } else if (!existing.enabled) {
            changes.push(ddcApi(
              `/api/v1/ddc/namespace-env-app-bindings/${encodeURIComponent(existing.id)}`,
              { method: 'PUT', body },
            ))
          }
        })
        current
          .filter((binding) => binding.enabled && !desired.has(binding.appCode))
          .forEach((binding) => changes.push(ddcApi(
            `/api/v1/ddc/namespace-env-app-bindings/${encodeURIComponent(binding.id)}`,
            { method: 'DELETE' },
          )))
      })
      await Promise.all(changes)
      message.success('命名空间绑定保存成功')
      closeBindings()
    } catch (error) {
      message.error(error instanceof Error ? error.message : String(error))
    } finally {
      setBindingSaving(false)
    }
  }

  const columns = [
    {
      title: '业务域',
      dataIndex: 'bizCode',
      key: 'bizCode',
      render: (value: string) => <Typography.Text code>{value}</Typography.Text>,
    },
    {
      title: '编码',
      dataIndex: 'namespaceCode',
      key: 'namespaceCode',
      render: (value: string) => <Typography.Text code>{value}</Typography.Text>,
    },
    { title: '名称', dataIndex: 'namespace', key: 'namespace' },
    { title: '描述', dataIndex: 'description', key: 'description' },
    {
      title: '启用',
      key: 'enabled',
      render: (_: unknown, row: DdcNamespace) => (
        <Switch
          checked={row.enabled}
          disabled={toggleMutation.isPending
            && toggleMutation.variables?.item.id === row.id}
          onChange={(enabled) => toggleMutation.mutate({ item: row, enabled })}
        />
      ),
    },
    {
      title: '更新时间',
      dataIndex: 'updatedAt',
      key: 'updatedAt',
      render: formatTime,
    },
    {
      title: '操作',
      key: 'actions',
      fixed: 'right' as const,
      render: (_: unknown, row: DdcNamespace) => (
        <Space wrap={false}>
          <Button size="small" onClick={() => openEdit(row)}>编辑</Button>
          <Button size="small" onClick={() => { void openBindings(row) }}>
            管理绑定
          </Button>
          <Popconfirm
            title={`确认删除命名空间 ${row.namespace}？`}
            onConfirm={() => removeMutation.mutateAsync(row)}
          >
            <Button size="small" danger>删除</Button>
          </Popconfirm>
        </Space>
      ),
    },
  ]

  return (
    <div>
      <AdminPageHeader
        title="命名空间管理"
        description="维护命名空间及其环境与应用可见性绑定。"
        extra={(
          <Button type="primary" onClick={openCreate}>新建命名空间</Button>
        )}
      />
      <Card size="small" style={{ marginBottom: 16 }}>
        <Space wrap>
          <span style={{ width: 220, display: 'inline-block' }}>
            <BizSelect
              value={draft.bizCode}
              onChange={(bizCode) => setDraft({ ...draft, bizCode })}
            />
          </span>
          <Input
            placeholder="命名空间模糊查询"
            value={draft.keyword}
            onChange={(event) => setDraft({
              ...draft,
              keyword: event.target.value,
            })}
            onPressEnter={applyFilter}
            style={{ width: 220 }}
          />
          <Button type="primary" onClick={applyFilter}>查询</Button>
          <Button onClick={resetFilter}>重置</Button>
        </Space>
      </Card>
      <Card size="small" title="命名空间列表">
        <PageState
          loading={query.isPending}
          error={query.error}
          empty={(query.data?.records.length ?? 0) === 0}
          onRetry={() => { void query.refetch() }}
        >
          <Table<DdcNamespace>
            rowKey={(row) => row.id}
            columns={columns}
            dataSource={query.data?.records ?? []}
            loading={query.isFetching}
            size="small"
            scroll={{ x: 'max-content' }}
            pagination={{
              current: query.data?.page.pageNo ?? pageState.page.pageNo,
              pageSize: query.data?.page.pageSize ?? pageState.page.pageSize,
              total: query.data?.page.total ?? 0,
              showSizeChanger: true,
              pageSizeOptions: [10, 20, 50],
              showTotal: (total) => `共 ${total} 条`,
              onChange: pageState.onTableChange,
            }}
          />
        </PageState>
      </Card>
      <Modal
        open={open}
        title={editing ? '编辑命名空间' : '新建命名空间'}
        onCancel={() => setOpen(false)}
        onOk={() => { void save() }}
        okText="保存"
        confirmLoading={saveMutation.isPending}
        destroyOnHidden
      >
        <Form<NamespaceFormValues>
          form={form}
          layout="vertical"
          initialValues={{ enabled: true }}
        >
          <Form.Item name="bizCode" label="业务域" rules={[{ required: true }]}>
            <BizSelect disabled={Boolean(editing)} />
          </Form.Item>
          <Form.Item name="namespaceCode" label="编码" rules={[{ required: true }]}>
            <Input disabled={Boolean(editing)} placeholder="全局唯一" />
          </Form.Item>
          <Form.Item name="namespace" label="名称" rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item name="description" label="描述">
            <Input />
          </Form.Item>
          <Form.Item name="enabled" label="启用" valuePropName="checked">
            <Switch />
          </Form.Item>
        </Form>
      </Modal>
      <Drawer
        open={bindingNamespace !== null}
        title={`管理绑定：${bindingNamespace?.bizCode ?? ''}/${bindingNamespace?.namespaceCode ?? ''}`}
        size={screens.md ? 860 : '100%'}
        onClose={closeBindings}
        extra={(
          <Button
            type="primary"
            loading={bindingSaving}
            onClick={() => { void saveBindings() }}
          >
            保存绑定
          </Button>
        )}
      >
        <Table<DdcEnv>
          rowKey={(row) => row.envCode}
          loading={bindingLoading}
          dataSource={bindingEnvs}
          pagination={false}
          size="small"
          scroll={{ x: 'max-content' }}
          columns={[
            {
              title: '环境',
              dataIndex: 'envCode',
              key: 'envCode',
              width: 160,
              render: (value: string) => (
                <Typography.Text code>{value}</Typography.Text>
              ),
            },
            {
              title: '可见应用（多选）',
              key: 'apps',
              width: 520,
              render: (_: unknown, env: DdcEnv) => (
                <Select
                  mode="multiple"
                  showSearch
                  maxTagCount="responsive"
                  optionFilterProp="label"
                  value={bindingDraft[env.envCode] ?? []}
                  options={bindingApps.map((app) => ({
                    value: app.appCode,
                    label: `${app.appCode}（${app.appName}）`,
                  }))}
                  onChange={(values) => setBindingDraft((current) => ({
                    ...current,
                    [env.envCode]: values,
                  }))}
                  style={{ width: '100%' }}
                />
              ),
            },
          ]}
        />
      </Drawer>
    </div>
  )
}
