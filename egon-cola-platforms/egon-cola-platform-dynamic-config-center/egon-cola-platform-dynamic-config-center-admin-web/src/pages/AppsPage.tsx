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
  Form,
  Input,
  Modal,
  Popconfirm,
  Space,
  Switch,
  Table,
  Typography,
} from 'antd'
import { useState } from 'react'
import { ddcApi, ddcPageApi } from '../api/client'
import type { DdcApp } from '../api/types'
import AdminPageHeader from '../components/page/AdminPageHeader'
import BizSelect from '../components/scope/BizSelect'
import ScopeSelects, { type ScopeValue } from '../components/scope/ScopeSelects'
import { scopeOptionQueryKey } from '../components/scope/useScopeOptions'
import { usePageState } from '../hooks/usePageState'
import { buildQuery, formatTime } from '../lib/query'

type AppFilter = ScopeValue & { keyword: string }

const emptyFilter: AppFilter = {
  bizCode: '',
  namespaceCode: '',
  env: '',
  appCode: '',
  keyword: '',
}

type AppFormValues = {
  bizCode: string
  appCode: string
  appName: string
  owner?: string
  description?: string
  enabled: boolean
}

export default function AppsPage() {
  const { message } = App.useApp()
  const queryClient = useQueryClient()
  const pageState = usePageState()
  const [draft, setDraft] = useState<AppFilter>({ ...emptyFilter })
  const [submitted, setSubmitted] = useState<AppFilter>({ ...emptyFilter })
  const [open, setOpen] = useState(false)
  const [editing, setEditing] = useState<DdcApp | null>(null)
  const [form] = Form.useForm<AppFormValues>()

  const queryString = buildQuery({
    bizCode: submitted.bizCode,
    namespaceCode: submitted.namespaceCode,
    env: submitted.env,
    keyword: submitted.keyword,
    pageNo: pageState.page.pageNo,
    pageSize: pageState.page.pageSize,
  })
  const query = useQuery({
    queryKey: ['ddc', 'apps', submitted, pageState.page],
    queryFn: ({ signal }) => ddcPageApi<DdcApp>(
      `/api/v1/ddc/apps/page?${queryString}`,
      { signal },
    ),
    placeholderData: keepPreviousData,
    staleTime: 0,
  })

  const invalidate = async () => {
    await queryClient.invalidateQueries({ queryKey: ['ddc', 'apps'] })
    await queryClient.invalidateQueries({ queryKey: scopeOptionQueryKey })
  }

  const saveMutation = useMutation({
    mutationFn: ({ item, values }: {
      item: DdcApp | null
      values: AppFormValues
    }) => item
      ? ddcApi(`/api/v1/ddc/apps/${encodeURIComponent(item.id)}`, {
        method: 'PUT',
        body: values,
      })
      : ddcApi('/api/v1/ddc/apps', { method: 'POST', body: values }),
    onSuccess: async () => {
      setOpen(false)
      await invalidate()
      message.success('应用保存成功')
    },
    onError: (error) => message.error(
      error instanceof Error ? error.message : String(error),
    ),
  })

  const toggleMutation = useMutation({
    mutationFn: ({ item, enabled }: { item: DdcApp; enabled: boolean }) =>
      ddcApi(
        `/api/v1/ddc/apps/${encodeURIComponent(item.id)}/enabled?enabled=${enabled}`,
        { method: 'PUT' },
      ),
    onSuccess: async () => {
      await invalidate()
      message.success('应用状态更新完成')
    },
    onError: (error) => message.error(
      error instanceof Error ? error.message : String(error),
    ),
  })

  const removeMutation = useMutation({
    mutationFn: (item: DdcApp) => ddcApi(
      `/api/v1/ddc/apps/${encodeURIComponent(item.id)}`,
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
      message.success('应用删除完成')
    },
    onError: (error) => message.error(
      error instanceof Error ? error.message : String(error),
    ),
  })

  const applyFilter = () => {
    setSubmitted({
      ...draft,
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

  const openEdit = (app: DdcApp) => {
    setEditing(app)
    form.setFieldsValue({
      bizCode: app.bizCode,
      appCode: app.appCode,
      appName: app.appName,
      owner: app.owner ?? '',
      description: app.description ?? '',
      enabled: app.enabled,
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

  const columns = [
    {
      title: '业务域',
      dataIndex: 'bizCode',
      key: 'bizCode',
      render: (value: string) => <Typography.Text code>{value}</Typography.Text>,
    },
    {
      title: '应用编码',
      dataIndex: 'appCode',
      key: 'appCode',
      render: (value: string) => <Typography.Text code>{value}</Typography.Text>,
    },
    { title: '应用名称', dataIndex: 'appName', key: 'appName' },
    { title: '负责人', dataIndex: 'owner', key: 'owner' },
    {
      title: '启用',
      key: 'enabled',
      render: (_: unknown, row: DdcApp) => (
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
      render: (_: unknown, row: DdcApp) => (
        <Space wrap={false}>
          <Button size="small" onClick={() => openEdit(row)}>编辑</Button>
          <Popconfirm
            title={`确认删除应用 ${row.appCode}？`}
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
        title="应用管理"
        description="维护业务域中的应用及其作用域可见性。"
        extra={<Button type="primary" onClick={openCreate}>新建应用</Button>}
      />
      <Card size="small" style={{ marginBottom: 16 }}>
        <Space wrap>
          <ScopeSelects
            value={draft}
            includeApp={false}
            onChange={(scope) => setDraft({ ...draft, ...scope })}
          />
          <Input
            placeholder="appCode / 名称模糊查询"
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
      <Card size="small" title="应用列表">
        <PageState
          loading={query.isPending}
          error={query.error}
          empty={(query.data?.records.length ?? 0) === 0}
          onRetry={() => { void query.refetch() }}
        >
          <Table<DdcApp>
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
        title={editing ? '编辑应用' : '新建应用'}
        onCancel={() => setOpen(false)}
        onOk={() => { void save() }}
        okText="保存"
        confirmLoading={saveMutation.isPending}
        destroyOnHidden
      >
        <Form<AppFormValues>
          form={form}
          layout="vertical"
          initialValues={{ enabled: true }}
        >
          <Form.Item name="bizCode" label="业务域" rules={[{ required: true }]}>
            <BizSelect disabled={Boolean(editing)} />
          </Form.Item>
          <Form.Item name="appCode" label="应用编码" rules={[{ required: true }]}>
            <Input disabled={Boolean(editing)} />
          </Form.Item>
          <Form.Item name="appName" label="应用名称" rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item name="owner" label="负责人">
            <Input placeholder="local-admin" />
          </Form.Item>
          <Form.Item name="description" label="描述">
            <Input />
          </Form.Item>
          <Form.Item name="enabled" label="启用" valuePropName="checked">
            <Switch />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}
