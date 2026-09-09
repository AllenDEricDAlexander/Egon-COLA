import {
  keepPreviousData,
  useMutation,
  useQuery,
  useQueryClient,
} from '@tanstack/react-query'
import { PageState } from '@egon-cola/xingyuan-admin-web-shared'
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
import type {
  DdcNamespaceEnvAppBinding,
  DdcNamespaceEnvAppBindingRequest,
} from '../api/types'
import AdminPageHeader from '../components/page/AdminPageHeader'
import { usePageState } from '../hooks/usePageState'
import { buildQuery } from '../lib/query'

type BindingFilter = {
  bizCode: string
  namespaceCode: string
  env: string
  appCode: string
}

const emptyFilter: BindingFilter = {
  bizCode: '',
  namespaceCode: '',
  env: '',
  appCode: '',
}

export default function BindingsPage() {
  const { message } = App.useApp()
  const queryClient = useQueryClient()
  const pageState = usePageState()
  const [draft, setDraft] = useState<BindingFilter>({ ...emptyFilter })
  const [submitted, setSubmitted] = useState<BindingFilter>({ ...emptyFilter })
  const [editing, setEditing] = useState<DdcNamespaceEnvAppBinding | null>(null)
  const [open, setOpen] = useState(false)
  const [form] = Form.useForm<DdcNamespaceEnvAppBindingRequest>()

  const queryString = buildQuery({
    ...submitted,
    pageNo: pageState.page.pageNo,
    pageSize: pageState.page.pageSize,
  })
  const query = useQuery({
    queryKey: ['ddc', 'bindings', submitted, pageState.page],
    queryFn: ({ signal }) => ddcPageApi<DdcNamespaceEnvAppBinding>(
      `/api/v1/ddc/namespace-env-app-bindings/page?${queryString}`,
      { signal },
    ),
    placeholderData: keepPreviousData,
    staleTime: 0,
  })

  const queryKey = ['ddc', 'bindings']
  const invalidate = async () => queryClient.invalidateQueries({ queryKey })

  const saveMutation = useMutation({
    mutationFn: ({ id, values }: {
      id?: string
      values: DdcNamespaceEnvAppBindingRequest
    }) => id
      ? ddcApi<DdcNamespaceEnvAppBinding>(
        `/api/v1/ddc/namespace-env-app-bindings/${encodeURIComponent(id)}`,
        { method: 'PUT', body: values },
      )
      : ddcApi<DdcNamespaceEnvAppBinding>(
        '/api/v1/ddc/namespace-env-app-bindings',
        { method: 'POST', body: values },
      ),
    onSuccess: async () => {
      setOpen(false)
      setEditing(null)
      await invalidate()
      message.success('作用域绑定保存成功')
    },
    onError: (error) => message.error(
      error instanceof Error ? error.message : String(error),
    ),
  })

  const removeMutation = useMutation({
    mutationFn: (item: DdcNamespaceEnvAppBinding) => ddcApi<void>(
      `/api/v1/ddc/namespace-env-app-bindings/${encodeURIComponent(item.id)}`,
      { method: 'DELETE' },
    ),
    onSuccess: async () => {
      if ((query.data?.records.length ?? 0) === 1 && pageState.page.pageNo > 1) {
        pageState.onTableChange(pageState.page.pageNo - 1, pageState.page.pageSize)
      }
      await invalidate()
      message.success('作用域绑定删除完成')
    },
    onError: (error) => message.error(
      error instanceof Error ? error.message : String(error),
    ),
  })

  const applyFilter = () => {
    setSubmitted({
      bizCode: draft.bizCode.trim(),
      namespaceCode: draft.namespaceCode.trim(),
      env: draft.env.trim(),
      appCode: draft.appCode.trim(),
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
    form.setFieldValue('enabled', true)
    setOpen(true)
  }

  const openEdit = (item: DdcNamespaceEnvAppBinding) => {
    setEditing(item)
    form.setFieldsValue({
      bizCode: item.bizCode,
      namespaceCode: item.namespaceCode,
      env: item.env,
      appCode: item.appCode,
      enabled: item.enabled,
    })
    setOpen(true)
  }

  const save = async () => {
    try {
      const values = await form.validateFields()
      saveMutation.mutate({ id: editing?.id, values })
    } catch {
      return
    }
  }

  return (
    <div>
      <AdminPageHeader
        title="作用域绑定管理"
        description="维护命名空间、环境与应用之间的可见性绑定。"
        extra={<Button type="primary" onClick={openCreate}>新增绑定</Button>}
      />
      <Card size="small" style={{ marginBottom: 16 }}>
        <Space wrap>
          <Input
            aria-label="业务域"
            placeholder="业务域"
            value={draft.bizCode}
            onChange={(event) => setDraft({ ...draft, bizCode: event.target.value })}
          />
          <Input
            aria-label="命名空间编码"
            placeholder="命名空间编码"
            value={draft.namespaceCode}
            onChange={(event) => setDraft({ ...draft, namespaceCode: event.target.value })}
          />
          <Input
            aria-label="环境"
            placeholder="环境"
            value={draft.env}
            onChange={(event) => setDraft({ ...draft, env: event.target.value })}
          />
          <Input
            aria-label="应用编码"
            placeholder="应用编码"
            value={draft.appCode}
            onChange={(event) => setDraft({ ...draft, appCode: event.target.value })}
            onPressEnter={applyFilter}
          />
          <Button type="primary" onClick={applyFilter}>查询</Button>
          <Button onClick={resetFilter}>重置</Button>
        </Space>
      </Card>
      <Card size="small" title="绑定列表">
        <PageState
          loading={query.isPending}
          error={query.error}
          empty={(query.data?.records.length ?? 0) === 0}
          onRetry={() => { void query.refetch() }}
        >
          <Table<DdcNamespaceEnvAppBinding>
            rowKey={(row) => row.id}
            size="small"
            scroll={{ x: 'max-content' }}
            loading={query.isFetching}
            dataSource={query.data?.records ?? []}
            columns={[
              { title: '业务域', dataIndex: 'bizCode', render: (value: string) => <Typography.Text code>{value}</Typography.Text> },
              { title: '命名空间', dataIndex: 'namespaceCode', render: (value: string) => <Typography.Text code>{value}</Typography.Text> },
              { title: '环境', dataIndex: 'env' },
              { title: '应用', dataIndex: 'appCode', render: (value: string) => <Typography.Text code>{value}</Typography.Text> },
              { title: '应用名称', dataIndex: 'appName' },
              { title: '启用', dataIndex: 'enabled', render: (enabled: boolean) => <Switch checked={enabled} disabled /> },
              {
                title: '操作',
                fixed: 'right' as const,
                render: (_value: unknown, item: DdcNamespaceEnvAppBinding) => (
                  <Space>
                    <Button size="small" onClick={() => openEdit(item)}>编辑</Button>
                    <Popconfirm
                      title={`确认删除 ${item.namespaceCode}/${item.env}/${item.appCode} 绑定？`}
                      onConfirm={() => removeMutation.mutate(item)}
                    >
                      <Button size="small" danger loading={removeMutation.isPending && removeMutation.variables?.id === item.id}>
                        删除
                      </Button>
                    </Popconfirm>
                  </Space>
                ),
              },
            ]}
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
        title={editing ? '编辑作用域绑定' : '新增作用域绑定'}
        onCancel={() => { setOpen(false); setEditing(null) }}
        onOk={() => { void save() }}
        okText="保存"
        confirmLoading={saveMutation.isPending}
        destroyOnHidden
      >
        <Form<DdcNamespaceEnvAppBindingRequest>
          form={form}
          layout="vertical"
          initialValues={{ enabled: true }}
        >
          <Form.Item name="bizCode" label="业务域" rules={[{ required: true }]}>
            <Input disabled={Boolean(editing)} />
          </Form.Item>
          <Form.Item name="namespaceCode" label="命名空间编码" rules={[{ required: true }]}>
            <Input disabled={Boolean(editing)} />
          </Form.Item>
          <Form.Item name="env" label="环境" rules={[{ required: true }]}>
            <Input disabled={Boolean(editing)} />
          </Form.Item>
          <Form.Item name="appCode" label="应用编码" rules={[{ required: true }]}>
            <Input disabled={Boolean(editing)} />
          </Form.Item>
          <Form.Item name="enabled" label="启用" valuePropName="checked">
            <Switch />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}
