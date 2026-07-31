import { useCallback, useEffect, useState } from 'react'
import { Button, Card, Form, Input, Modal, Switch, Table, Tag, Typography, message } from 'antd'
import { ddcApi } from '../api/client'
import type { DdcApp } from '../api/types'
import { formatTime } from '../lib/query'

type AppFormValues = {
  appCode: string
  appName: string
  owner?: string
  description?: string
  enabled: boolean
}

export default function AppsPage() {
  const [apps, setApps] = useState<DdcApp[]>([])
  const [loading, setLoading] = useState(false)
  const [open, setOpen] = useState(false)
  const [form] = Form.useForm<AppFormValues>()

  const loadApps = useCallback(async () => {
    const data = await ddcApi<DdcApp[]>('/api/v1/ddc/apps')
    setApps(data ?? [])
  }, [])

  useEffect(() => {
    loadApps().catch((error) => {
      message.error(error instanceof Error ? error.message : String(error))
    })
  }, [loadApps])

  const refresh = async () => {
    setLoading(true)
    try {
      await loadApps()
    } catch (error) {
      message.error(error instanceof Error ? error.message : String(error))
    } finally {
      setLoading(false)
    }
  }

  const save = async () => {
    let values: AppFormValues
    try {
      values = await form.validateFields()
    } catch {
      return
    }
    try {
      await ddcApi('/api/v1/ddc/apps', {
        method: 'POST',
        body: {
          appCode: values.appCode,
          appName: values.appName,
          owner: values.owner || 'local-admin',
          description: values.description ?? '',
          enabled: values.enabled,
        },
      })
      message.success('应用已保存')
      setOpen(false)
      form.resetFields()
      await refresh()
    } catch (error) {
      message.error(error instanceof Error ? error.message : String(error))
    }
  }

  const columns = [
    { title: '应用编码', dataIndex: 'appCode', key: 'appCode', render: (value: string) => <Typography.Text code>{value}</Typography.Text> },
    { title: '应用名称', dataIndex: 'appName', key: 'appName' },
    { title: '负责人', dataIndex: 'owner', key: 'owner' },
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
      <Typography.Title level={3}>应用管理</Typography.Title>
      <Card
        size="small"
        title={`应用（${apps.length}）`}
        extra={<Button type="primary" onClick={() => setOpen(true)}>新建应用</Button>}
        style={{ marginBottom: 16 }}
      >
        <Table<DdcApp>
          rowKey={(row) => row.id}
          columns={columns}
          dataSource={apps}
          loading={loading}
          size="small"
          pagination={{ pageSize: 10, size: 'small' }}
        />
      </Card>
      <Modal
        open={open}
        title="新建应用"
        onCancel={() => setOpen(false)}
        onOk={() => void save()}
        okText="保存"
        destroyOnHidden
      >
        <Form<AppFormValues> form={form} layout="vertical" initialValues={{ enabled: true }}>
          <Form.Item name="appCode" label="应用编码" rules={[{ required: true }]}>
            <Input />
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
