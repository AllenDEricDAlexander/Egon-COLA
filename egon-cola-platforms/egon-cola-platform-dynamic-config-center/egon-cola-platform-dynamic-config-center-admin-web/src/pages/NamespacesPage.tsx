import { useCallback, useEffect, useRef, useState } from 'react'
import { Button, Card, Form, Input, Modal, Popconfirm, Space, Switch, Table, Tag, Typography, message } from 'antd'
import { ddcApi } from '../api/client'
import type { DdcNamespace } from '../api/types'
import AppSelect from '../components/scope/AppSelect'
import { formatTime } from '../lib/query'

type NamespaceFilter = { appCode: string; keyword: string }

type NamespaceFormValues = {
  appCode: string
  namespace: string
  description?: string
  enabled: boolean
}

export default function NamespacesPage() {
  const [draft, setDraft] = useState<NamespaceFilter>({ appCode: '', keyword: '' })
  const [namespaces, setNamespaces] = useState<DdcNamespace[]>([])
  const [loading, setLoading] = useState(false)
  const [open, setOpen] = useState(false)
  const [editing, setEditing] = useState<DdcNamespace | null>(null)
  const filterRef = useRef<NamespaceFilter>({ appCode: '', keyword: '' })
  const [form] = Form.useForm<NamespaceFormValues>()

  const loadNamespaces = useCallback(async () => {
    const { appCode, keyword } = filterRef.current
    const params = new URLSearchParams()
    if (appCode.trim() !== '') params.set('appCode', appCode.trim())
    if (keyword.trim() !== '') params.set('keyword', keyword.trim())
    const query = params.toString()
    const data = await ddcApi<DdcNamespace[]>(
      `/api/v1/ddc/namespaces${query === '' ? '' : `?${query}`}`,
    )
    setNamespaces(data ?? [])
  }, [])

  useEffect(() => {
    loadNamespaces().catch((error) => {
      message.error(error instanceof Error ? error.message : String(error))
    })
  }, [loadNamespaces])

  const refresh = async () => {
    setLoading(true)
    try {
      await loadNamespaces()
    } catch (error) {
      message.error(error instanceof Error ? error.message : String(error))
    } finally {
      setLoading(false)
    }
  }

  const applyFilter = () => {
    filterRef.current = { ...draft }
    void refresh()
  }

  const openCreate = () => {
    setEditing(null)
    form.resetFields()
    setOpen(true)
  }

  const openEdit = (item: DdcNamespace) => {
    setEditing(item)
    form.setFieldsValue({
      appCode: item.appCode,
      namespace: item.namespace,
      description: item.description ?? '',
      enabled: item.enabled,
    })
    setOpen(true)
  }

  const save = async () => {
    let values: NamespaceFormValues
    try {
      values = await form.validateFields()
    } catch {
      return
    }
    try {
      if (editing) {
        await ddcApi(`/api/v1/ddc/namespaces/${encodeURIComponent(editing.id)}`, {
          method: 'PUT',
          body: { ...values },
        })
      } else {
        await ddcApi('/api/v1/ddc/namespaces', {
          method: 'POST',
          body: { ...values },
        })
      }
      message.success('命名空间已保存')
      setOpen(false)
      await refresh()
    } catch (error) {
      message.error(error instanceof Error ? error.message : String(error))
    }
  }

  const toggleEnabled = async (item: DdcNamespace, enabled: boolean) => {
    try {
      await ddcApi(`/api/v1/ddc/namespaces/${encodeURIComponent(item.id)}/enabled?enabled=${enabled}`, {
        method: 'PUT',
      })
      await refresh()
    } catch (error) {
      message.error(error instanceof Error ? error.message : String(error))
    }
  }

  const remove = async (item: DdcNamespace) => {
    try {
      await ddcApi(`/api/v1/ddc/namespaces/${encodeURIComponent(item.id)}`, {
        method: 'DELETE',
      })
      message.success('命名空间已删除')
      await refresh()
    } catch (error) {
      message.error(error instanceof Error ? error.message : String(error))
    }
  }

  const columns = [
    { title: '应用编码', dataIndex: 'appCode', key: 'appCode', render: (value: string) => <Typography.Text code>{value}</Typography.Text> },
    { title: '命名空间', dataIndex: 'namespace', key: 'namespace' },
    { title: '描述', dataIndex: 'description', key: 'description' },
    {
      title: '启用',
      key: 'enabled',
      render: (_: unknown, row: DdcNamespace) => (
        <Switch checked={row.enabled} onChange={(checked) => void toggleEnabled(row, checked)} />
      ),
    },
    { title: '更新时间', dataIndex: 'updatedAt', key: 'updatedAt', render: formatTime },
    {
      title: '操作',
      key: 'actions',
      render: (_: unknown, row: DdcNamespace) => (
        <Space>
          <Button size="small" onClick={() => openEdit(row)}>编辑</Button>
          <Popconfirm title={`确认删除命名空间 ${row.namespace}？`} onConfirm={() => void remove(row)}>
            <Button size="small" danger>删除</Button>
          </Popconfirm>
        </Space>
      ),
    },
  ]

  return (
    <div>
      <Typography.Title level={3}>命名空间管理</Typography.Title>
      <Card size="small" style={{ marginBottom: 16 }}>
        <Space wrap>
          <span style={{ width: 200, display: 'inline-block' }}>
            <AppSelect
              value={draft.appCode}
              onChange={(appCode) => setDraft({ ...draft, appCode })}
            />
          </span>
          <Input
            placeholder="命名空间模糊查询"
            value={draft.keyword}
            onChange={(event) => setDraft({ ...draft, keyword: event.target.value })}
            style={{ width: 200 }}
          />
          <Button type="primary" onClick={applyFilter}>查询</Button>
          <Button onClick={openCreate}>新建命名空间</Button>
        </Space>
      </Card>
      <Card size="small" title={`命名空间（${namespaces.length}）`}>
        <Table<DdcNamespace>
          rowKey={(row) => row.id}
          columns={columns}
          dataSource={namespaces}
          loading={loading}
          size="small"
          pagination={{ pageSize: 10, size: 'small' }}
        />
      </Card>
      <Modal
        open={open}
        title={editing ? '编辑命名空间' : '新建命名空间'}
        onCancel={() => setOpen(false)}
        onOk={() => void save()}
        okText="保存"
        destroyOnHidden
      >
        <Form<NamespaceFormValues> form={form} layout="vertical" initialValues={{ enabled: true }}>
          <Form.Item name="appCode" label="应用" rules={[{ required: true }]}>
            <AppSelect disabled={Boolean(editing)} />
          </Form.Item>
          <Form.Item name="namespace" label="命名空间" rules={[{ required: true }]}>
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
