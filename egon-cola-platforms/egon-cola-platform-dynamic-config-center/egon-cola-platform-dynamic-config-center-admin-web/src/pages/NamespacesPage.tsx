import { useCallback, useEffect, useRef, useState } from 'react'
import { Button, Card, Form, Input, Modal, Switch, Table, Tag, Typography, message } from 'antd'
import { ddcApi } from '../api/client'
import type { DdcNamespace } from '../api/types'
import { buildQuery, formatTime } from '../lib/query'

type NamespaceFilter = { appCode: string; env: string }

type NamespaceFormValues = {
  appCode: string
  env: string
  namespace: string
  description?: string
  enabled: boolean
}

export default function NamespacesPage() {
  const [draft, setDraft] = useState<NamespaceFilter>({ appCode: '', env: '' })
  const [namespaces, setNamespaces] = useState<DdcNamespace[]>([])
  const [loading, setLoading] = useState(false)
  const [open, setOpen] = useState(false)
  const filterRef = useRef<NamespaceFilter>({ appCode: '', env: '' })
  const [form] = Form.useForm<NamespaceFormValues>()

  const loadNamespaces = useCallback(async () => {
    const scope = filterRef.current
    if (scope.appCode.trim() === '' || scope.env.trim() === '') {
      setNamespaces([])
      return
    }
    const data = await ddcApi<DdcNamespace[]>(`/api/v1/ddc/namespaces?${buildQuery(scope)}`)
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
    filterRef.current = { appCode: draft.appCode, env: draft.env }
    void refresh()
  }

  const save = async () => {
    let values: NamespaceFormValues
    try {
      values = await form.validateFields()
    } catch {
      return
    }
    try {
      await ddcApi('/api/v1/ddc/namespaces', {
        method: 'POST',
        body: {
          appCode: values.appCode,
          env: values.env,
          namespace: values.namespace,
          description: values.description ?? '',
          enabled: values.enabled,
        },
      })
      message.success('命名空间已保存')
      setOpen(false)
      form.resetFields()
      await refresh()
    } catch (error) {
      message.error(error instanceof Error ? error.message : String(error))
    }
  }

  const columns = [
    { title: '应用编码', dataIndex: 'appCode', key: 'appCode', render: (value: string) => <Typography.Text code>{value}</Typography.Text> },
    { title: '环境', dataIndex: 'env', key: 'env' },
    { title: '命名空间', dataIndex: 'namespace', key: 'namespace' },
    {
      title: '启用',
      dataIndex: 'enabled',
      key: 'enabled',
      render: (enabled: boolean) => <Tag color={enabled ? 'green' : 'default'}>{enabled ? '是' : '否'}</Tag>,
    },
    { title: '描述', dataIndex: 'description', key: 'description' },
    { title: '更新时间', dataIndex: 'updatedAt', key: 'updatedAt', render: formatTime },
  ]

  return (
    <div>
      <Typography.Title level={3}>命名空间管理</Typography.Title>
      <Card size="small" style={{ marginBottom: 16 }}>
        <Button type="primary" onClick={() => setOpen(true)}>新建命名空间</Button>
      </Card>
      <Card size="small" title={`命名空间（${namespaces.length}）`}>
        <div style={{ marginBottom: 12 }}>
          <Input
            placeholder="appCode"
            value={draft.appCode}
            onChange={(event) => setDraft({ ...draft, appCode: event.target.value })}
            style={{ width: 180, marginRight: 8 }}
          />
          <Input
            placeholder="env"
            value={draft.env}
            onChange={(event) => setDraft({ ...draft, env: event.target.value })}
            style={{ width: 140, marginRight: 8 }}
          />
          <Button type="primary" onClick={applyFilter}>查询</Button>
        </div>
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
        title="新建命名空间"
        onCancel={() => setOpen(false)}
        onOk={() => void save()}
        okText="保存"
        destroyOnHidden
      >
        <Form<NamespaceFormValues> form={form} layout="vertical" initialValues={{ enabled: true }}>
          <Form.Item name="appCode" label="应用编码" rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item name="env" label="环境" rules={[{ required: true }]}>
            <Input />
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
