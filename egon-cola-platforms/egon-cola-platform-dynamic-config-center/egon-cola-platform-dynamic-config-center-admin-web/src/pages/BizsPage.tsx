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
import type { DdcBiz } from '../api/types'
import AdminPageHeader from '../components/page/AdminPageHeader'
import { scopeOptionQueryKey } from '../components/scope/useScopeOptions'
import { usePageState } from '../hooks/usePageState'
import { buildQuery, formatTime } from '../lib/query'

type BizFormValues = {
  bizCode: string
  bizName: string
  description?: string
  enabled: boolean
}

export default function BizsPage() {
  const { message } = App.useApp()
  const queryClient = useQueryClient()
  const pageState = usePageState()
  const [draft, setDraft] = useState('')
  const [keyword, setKeyword] = useState('')
  const [open, setOpen] = useState(false)
  const [editing, setEditing] = useState<DdcBiz | null>(null)
  const [form] = Form.useForm<BizFormValues>()

  const queryString = buildQuery({
    keyword,
    pageNo: pageState.page.pageNo,
    pageSize: pageState.page.pageSize,
  })
  const query = useQuery({
    queryKey: ['ddc', 'bizs', keyword, pageState.page],
    queryFn: ({ signal }) => ddcPageApi<DdcBiz>(
      `/api/v1/ddc/bizs/page?${queryString}`,
      { signal },
    ),
    placeholderData: keepPreviousData,
    staleTime: 0,
  })

  const invalidate = async () => {
    await queryClient.invalidateQueries({ queryKey: ['ddc', 'bizs'] })
    await queryClient.invalidateQueries({ queryKey: scopeOptionQueryKey })
  }

  const saveMutation = useMutation({
    mutationFn: ({ item, values }: {
      item: DdcBiz | null
      values: BizFormValues
    }) => item
      ? ddcApi(`/api/v1/ddc/bizs/${encodeURIComponent(item.bizCode)}`, {
        method: 'PUT',
        body: values,
      })
      : ddcApi('/api/v1/ddc/bizs', { method: 'POST', body: values }),
    onSuccess: async () => {
      setOpen(false)
      await invalidate()
      message.success('业务域保存成功')
    },
    onError: (error) => message.error(
      error instanceof Error ? error.message : String(error),
    ),
  })

  const toggleMutation = useMutation({
    mutationFn: ({ item, enabled }: { item: DdcBiz; enabled: boolean }) =>
      ddcApi(
        `/api/v1/ddc/bizs/${encodeURIComponent(item.bizCode)}/enabled?enabled=${enabled}`,
        { method: 'PUT' },
      ),
    onSuccess: async () => {
      await invalidate()
      message.success('业务域状态更新完成')
    },
    onError: (error) => message.error(
      error instanceof Error ? error.message : String(error),
    ),
  })

  const removeMutation = useMutation({
    mutationFn: (item: DdcBiz) => ddcApi(
      `/api/v1/ddc/bizs/${encodeURIComponent(item.bizCode)}`,
      { method: 'DELETE' },
    ),
    onSuccess: async () => {
      const moveBack = (query.data?.records.length ?? 0) === 1
        && pageState.page.pageNo > 1
      if (moveBack) {
        pageState.onTableChange(
          pageState.page.pageNo - 1,
          pageState.page.pageSize,
        )
      }
      await invalidate()
      message.success('业务域删除完成')
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

  const openEdit = (biz: DdcBiz) => {
    setEditing(biz)
    form.setFieldsValue({
      bizCode: biz.bizCode,
      bizName: biz.bizName,
      description: biz.description ?? '',
      enabled: biz.enabled,
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
      title: '业务域编码',
      dataIndex: 'bizCode',
      key: 'bizCode',
      render: (value: string) => <Typography.Text code>{value}</Typography.Text>,
    },
    { title: '名称', dataIndex: 'bizName', key: 'bizName' },
    { title: '描述', dataIndex: 'description', key: 'description' },
    {
      title: '启用',
      key: 'enabled',
      render: (_: unknown, row: DdcBiz) => (
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
      render: (_: unknown, row: DdcBiz) => (
        <Space wrap={false}>
          <Button size="small" onClick={() => openEdit(row)}>编辑</Button>
          <Popconfirm
            title={`确认删除业务域 ${row.bizCode}？`}
            onConfirm={() => removeMutation.mutateAsync(row)}
          >
            <Button size="small" danger loading={removeMutation.isPending
              && removeMutation.variables?.id === row.id}>删除</Button>
          </Popconfirm>
        </Space>
      ),
    },
  ]

  return (
    <div>
      <AdminPageHeader
        title="业务域管理"
        description="维护 DDC 业务域及其启用状态。"
        extra={<Button type="primary" onClick={openCreate}>新建业务域</Button>}
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
      <Card size="small" title="业务域列表">
        <PageState
          loading={query.isPending}
          error={query.error}
          empty={(query.data?.records.length ?? 0) === 0}
          onRetry={() => { void query.refetch() }}
        >
          <Table<DdcBiz>
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
        title={editing ? '编辑业务域' : '新建业务域'}
        onCancel={() => setOpen(false)}
        onOk={() => { void save() }}
        okText="保存"
        confirmLoading={saveMutation.isPending}
        destroyOnHidden
      >
        <Form<BizFormValues>
          form={form}
          layout="vertical"
          initialValues={{ enabled: true }}
        >
          <Form.Item
            name="bizCode"
            label="业务域编码"
            rules={[{ required: true }]}
          >
            <Input disabled={Boolean(editing)} />
          </Form.Item>
          <Form.Item name="bizName" label="名称" rules={[{ required: true }]}>
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
    </div>
  )
}
