import { useCallback, useEffect, useRef, useState } from 'react'
import { Button, Card, Form, Input, Modal, Popconfirm, Space, Switch, Table, Tag, Typography, message } from 'antd'
import { ddcApi } from '../api/client'
import type { DdcApp } from '../api/types'
import BizSelect from '../components/scope/BizSelect'
import { formatTime } from '../lib/query'

type AppFilter = { bizCode: string; keyword: string }

type AppFormValues = {
  bizCode: string
  appCode: string
  appName: string
  owner?: string
  description?: string
  enabled: boolean
}

export default function AppsPage() {
  const [draft, setDraft] = useState<AppFilter>({ bizCode: '', keyword: '' })
  const [apps, setApps] = useState<DdcApp[]>([])
  const [loading, setLoading] = useState(false)
  const [open, setOpen] = useState(false)
  const [editing, setEditing] = useState<DdcApp | null>(null)
  const filterRef = useRef<AppFilter>({ bizCode: '', keyword: '' })
  const [form] = Form.useForm<AppFormValues>()

  const loadApps = useCallback(async () => {
    const { bizCode, keyword } = filterRef.current
    const params = new URLSearchParams()
    if (bizCode.trim() !== '') params.set('biz', bizCode.trim())
    if (keyword.trim() !== '') params.set('keyword', keyword.trim())
    const query = params.toString()
    const data = await ddcApi<DdcApp[]>(`/api/v1/ddc/apps${query === '' ? '' : `?${query}`}`)
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

  const applyFilter = () => {
    filterRef.current = { ...draft }
    void refresh()
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
    let values: AppFormValues
    try {
      values = await form.validateFields()
    } catch {
      return
    }
    try {
      if (editing) {
        await ddcApi(`/api/v1/ddc/apps/${encodeURIComponent(editing.appCode)}`, {
          method: 'PUT',
          body: { ...values },
        })
      } else {
        await ddcApi('/api/v1/ddc/apps', {
          method: 'POST',
          body: { ...values },
        })
      }
      message.success('应用已保存')
      setOpen(false)
      await refresh()
    } catch (error) {
      message.error(error instanceof Error ? error.message : String(error))
    }
  }

  const toggleEnabled = async (app: DdcApp, enabled: boolean) => {
    try {
      await ddcApi(`/api/v1/ddc/apps/${encodeURIComponent(app.appCode)}/enabled?enabled=${enabled}`, {
        method: 'PUT',
      })
      await refresh()
    } catch (error) {
      message.error(error instanceof Error ? error.message : String(error))
    }
  }

  const remove = async (app: DdcApp) => {
    try {
      await ddcApi(`/api/v1/ddc/apps/${encodeURIComponent(app.appCode)}`, {
        method: 'DELETE',
      })
      message.success('应用已删除')
      await refresh()
    } catch (error) {
      message.error(error instanceof Error ? error.message : String(error))
    }
  }

  const columns = [
    { title: '业务域', dataIndex: 'bizCode', key: 'bizCode', render: (value: string) => <Typography.Text code>{value}</Typography.Text> },
    { title: '应用编码', dataIndex: 'appCode', key: 'appCode', render: (value: string) => <Typography.Text code>{value}</Typography.Text> },
    { title: '应用名称', dataIndex: 'appName', key: 'appName' },
    { title: '负责人', dataIndex: 'owner', key: 'owner' },
    {
      title: '启用',
      key: 'enabled',
      render: (_: unknown, row: DdcApp) => (
        <Switch checked={row.enabled} onChange={(checked) => void toggleEnabled(row, checked)} />
      ),
    },
    { title: '更新时间', dataIndex: 'updatedAt', key: 'updatedAt', render: formatTime },
    {
      title: '操作',
      key: 'actions',
      render: (_: unknown, row: DdcApp) => (
        <Space>
          <Button size="small" onClick={() => openEdit(row)}>编辑</Button>
          <Popconfirm title={`确认删除应用 ${row.appCode}？`} onConfirm={() => void remove(row)}>
            <Button size="small" danger>删除</Button>
          </Popconfirm>
        </Space>
      ),
    },
  ]

  return (
    <div>
      <Typography.Title level={3}>应用管理</Typography.Title>
      <Card size="small" style={{ marginBottom: 16 }}>
        <Space wrap>
          <span style={{ width: 200, display: 'inline-block' }}>
            <BizSelect
              value={draft.bizCode}
              onChange={(bizCode) => setDraft({ ...draft, bizCode })}
            />
          </span>
          <Input
            placeholder="appCode / 名称模糊查询"
            value={draft.keyword}
            onChange={(event) => setDraft({ ...draft, keyword: event.target.value })}
            style={{ width: 200 }}
          />
          <Button type="primary" onClick={applyFilter}>查询</Button>
          <Button onClick={openCreate}>新建应用</Button>
        </Space>
      </Card>
      <Card size="small" title={`应用（${apps.length}）`}>
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
        title={editing ? '编辑应用' : '新建应用'}
        onCancel={() => setOpen(false)}
        onOk={() => void save()}
        okText="保存"
        destroyOnHidden
      >
        <Form<AppFormValues> form={form} layout="vertical" initialValues={{ enabled: true }}>
          <Form.Item name="bizCode" label="业务域" rules={[{ required: true }]}>
            <BizSelect />
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
