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
  InputNumber,
  Modal,
  Popconfirm,
  Space,
  Switch,
  Table,
  Typography,
} from 'antd'
import { useState } from 'react'
import { ddcApi, ddcPageApi } from '../api/client'
import type { DdcEnv } from '../api/types'
import AdminPageHeader from '../components/page/AdminPageHeader'
import { scopeOptionQueryKey } from '../components/scope/useScopeOptions'
import { usePageState } from '../hooks/usePageState'
import { buildQuery, formatTime } from '../lib/query'

type EnvFormValues = {
  envCode: string
  description?: string
  sortOrder: number
  enabled: boolean
}

export default function EnvPage() {
  const { message } = App.useApp()
  const queryClient = useQueryClient()
  const pageState = usePageState()
  const [draft, setDraft] = useState('')
  const [keyword, setKeyword] = useState('')
  const [open, setOpen] = useState(false)
  const [editing, setEditing] = useState<DdcEnv | null>(null)
  const [form] = Form.useForm<EnvFormValues>()

  const queryString = buildQuery({
    keyword,
    pageNo: pageState.page.pageNo,
    pageSize: pageState.page.pageSize,
  })
  const query = useQuery({
    queryKey: ['ddc', 'envs', keyword, pageState.page],
    queryFn: ({ signal }) => ddcPageApi<DdcEnv>(
      `/api/v1/ddc/envs/page?${queryString}`,
      { signal },
    ),
    placeholderData: keepPreviousData,
    staleTime: 0,
  })

  const invalidate = async () => {
    await queryClient.invalidateQueries({ queryKey: ['ddc', 'envs'] })
    await queryClient.invalidateQueries({ queryKey: scopeOptionQueryKey })
  }

  const saveMutation = useMutation({
    mutationFn: ({ item, values }: {
      item: DdcEnv | null
      values: EnvFormValues
    }) => item
      ? ddcApi(`/api/v1/ddc/envs/${encodeURIComponent(item.envCode)}`, {
        method: 'PUT',
        body: values,
      })
      : ddcApi('/api/v1/ddc/envs', { method: 'POST', body: values }),
    onSuccess: async () => {
      setOpen(false)
      await invalidate()
      message.success('环境保存成功')
    },
    onError: (error) => message.error(
      error instanceof Error ? error.message : String(error),
    ),
  })

  const toggleMutation = useMutation({
    mutationFn: ({ item, enabled }: { item: DdcEnv; enabled: boolean }) =>
      ddcApi(
        `/api/v1/ddc/envs/${encodeURIComponent(item.envCode)}/enabled?enabled=${enabled}`,
        { method: 'PUT' },
      ),
    onSuccess: async () => {
      await invalidate()
      message.success('环境状态更新完成')
    },
    onError: (error) => message.error(
      error instanceof Error ? error.message : String(error),
    ),
  })

  const removeMutation = useMutation({
    mutationFn: (item: DdcEnv) => ddcApi(
      `/api/v1/ddc/envs/${encodeURIComponent(item.envCode)}`,
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
      message.success('环境删除完成')
    },
    onError: (error) => message.error(
      error instanceof Error ? error.message : String(error),
    ),
  })

  const applyFilter = () => {
    setKeyword(draft.trim())
    pageState.resetPage()
  }

  const resetFilter = () => {
    setDraft('')
    setKeyword('')
    pageState.resetPage()
  }

  const openCreate = () => {
    setEditing(null)
    form.resetFields()
    setOpen(true)
  }

  const openEdit = (env: DdcEnv) => {
    setEditing(env)
    form.setFieldsValue({
      envCode: env.envCode,
      description: env.description ?? '',
      sortOrder: env.sortOrder,
      enabled: env.enabled,
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
      title: '环境编码',
      dataIndex: 'envCode',
      key: 'envCode',
      render: (value: string) => <Typography.Text code>{value}</Typography.Text>,
    },
    { title: '描述', dataIndex: 'description', key: 'description' },
    { title: '排序', dataIndex: 'sortOrder', key: 'sortOrder' },
    {
      title: '启用',
      key: 'enabled',
      render: (_: unknown, row: DdcEnv) => (
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
      render: (_: unknown, row: DdcEnv) => (
        <Space wrap={false}>
          <Button size="small" onClick={() => openEdit(row)}>编辑</Button>
          <Popconfirm
            title={`确认删除环境 ${row.envCode}？`}
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
        title="环境管理"
        description="维护 DDC 环境编码、排序和启用状态。"
        extra={<Button type="primary" onClick={openCreate}>新建环境</Button>}
      />
      <Card size="small" style={{ marginBottom: 16 }}>
        <Space wrap>
          <Input
            placeholder="名称模糊查询"
            value={draft}
            onChange={(event) => setDraft(event.target.value)}
            onPressEnter={applyFilter}
            style={{ width: 220 }}
          />
          <Button type="primary" onClick={applyFilter}>查询</Button>
          <Button onClick={resetFilter}>重置</Button>
        </Space>
      </Card>
      <Card size="small" title="环境列表">
        <PageState
          loading={query.isPending}
          error={query.error}
          empty={(query.data?.records.length ?? 0) === 0}
          onRetry={() => { void query.refetch() }}
        >
          <Table<DdcEnv>
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
        title={editing ? '编辑环境' : '新建环境'}
        onCancel={() => setOpen(false)}
        onOk={() => { void save() }}
        okText="保存"
        confirmLoading={saveMutation.isPending}
        destroyOnHidden
      >
        <Form<EnvFormValues>
          form={form}
          layout="vertical"
          initialValues={{ enabled: true, sortOrder: 0 }}
        >
          <Form.Item name="envCode" label="环境编码" rules={[{ required: true }]}>
            <Input disabled={Boolean(editing)} />
          </Form.Item>
          <Form.Item name="description" label="描述">
            <Input />
          </Form.Item>
          <Form.Item name="sortOrder" label="排序">
            <InputNumber min={0} style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item name="enabled" label="启用" valuePropName="checked">
            <Switch />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}
