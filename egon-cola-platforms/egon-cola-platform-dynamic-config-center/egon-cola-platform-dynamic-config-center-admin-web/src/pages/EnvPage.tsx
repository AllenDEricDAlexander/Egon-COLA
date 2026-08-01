import { useCallback, useEffect, useRef, useState } from 'react'
import { Button, Card, Form, Input, InputNumber, Modal, Popconfirm, Space, Switch, Table, Typography, message } from 'antd'
import { ddcApi } from '../api/client'
import type { DdcEnv } from '../api/types'
import { formatTime } from '../lib/query'

type EnvFormValues = {
  envCode: string
  description?: string
  sortOrder: number
  enabled: boolean
}

export default function EnvPage() {
  const [draft, setDraft] = useState('')
  const [envs, setEnvs] = useState<DdcEnv[]>([])
  const [loading, setLoading] = useState(false)
  const [open, setOpen] = useState(false)
  const [editing, setEditing] = useState<DdcEnv | null>(null)
  const keywordRef = useRef('')
  const [form] = Form.useForm<EnvFormValues>()

  const loadEnvs = useCallback(async () => {
    const keyword = keywordRef.current
    const data = await ddcApi<DdcEnv[]>(keyword === ''
      ? '/api/v1/ddc/envs'
      : `/api/v1/ddc/envs?keyword=${encodeURIComponent(keyword)}`)
    setEnvs(data ?? [])
  }, [])

  useEffect(() => {
    loadEnvs().catch((error) => {
      message.error(error instanceof Error ? error.message : String(error))
    })
  }, [loadEnvs])

  const refresh = async () => {
    setLoading(true)
    try {
      await loadEnvs()
    } catch (error) {
      message.error(error instanceof Error ? error.message : String(error))
    } finally {
      setLoading(false)
    }
  }

  const applyFilter = () => {
    keywordRef.current = draft
    void refresh()
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
    let values: EnvFormValues
    try {
      values = await form.validateFields()
    } catch {
      return
    }
    try {
      if (editing) {
        await ddcApi(`/api/v1/ddc/envs/${encodeURIComponent(editing.envCode)}`, {
          method: 'PUT',
          body: { ...values },
        })
      } else {
        await ddcApi('/api/v1/ddc/envs', {
          method: 'POST',
          body: { ...values },
        })
      }
      message.success('环境已保存')
      setOpen(false)
      await refresh()
    } catch (error) {
      message.error(error instanceof Error ? error.message : String(error))
    }
  }

  const toggleEnabled = async (env: DdcEnv, enabled: boolean) => {
    try {
      await ddcApi(`/api/v1/ddc/envs/${encodeURIComponent(env.envCode)}/enabled?enabled=${enabled}`, {
        method: 'PUT',
      })
      await refresh()
    } catch (error) {
      message.error(error instanceof Error ? error.message : String(error))
    }
  }

  const remove = async (env: DdcEnv) => {
    try {
      await ddcApi(`/api/v1/ddc/envs/${encodeURIComponent(env.envCode)}`, {
        method: 'DELETE',
      })
      message.success('环境已删除')
      await refresh()
    } catch (error) {
      message.error(error instanceof Error ? error.message : String(error))
    }
  }

  const columns = [
    { title: '环境编码', dataIndex: 'envCode', key: 'envCode', render: (value: string) => <Typography.Text code>{value}</Typography.Text> },
    { title: '描述', dataIndex: 'description', key: 'description' },
    { title: '排序', dataIndex: 'sortOrder', key: 'sortOrder' },
    {
      title: '启用',
      key: 'enabled',
      render: (_: unknown, row: DdcEnv) => (
        <Switch checked={row.enabled} onChange={(checked) => void toggleEnabled(row, checked)} />
      ),
    },
    { title: '更新时间', dataIndex: 'updatedAt', key: 'updatedAt', render: formatTime },
    {
      title: '操作',
      key: 'actions',
      render: (_: unknown, row: DdcEnv) => (
        <Space>
          <Button size="small" onClick={() => openEdit(row)}>编辑</Button>
          <Popconfirm title={`确认删除环境 ${row.envCode}？`} onConfirm={() => void remove(row)}>
            <Button size="small" danger>删除</Button>
          </Popconfirm>
        </Space>
      ),
    },
  ]

  return (
    <div>
      <Typography.Title level={3}>环境管理</Typography.Title>
      <Card size="small" style={{ marginBottom: 16 }}>
        <Space wrap>
          <Input
            placeholder="名称模糊查询"
            value={draft}
            onChange={(event) => setDraft(event.target.value)}
            style={{ width: 200 }}
          />
          <Button type="primary" onClick={applyFilter}>查询</Button>
          <Button onClick={openCreate}>新建环境</Button>
        </Space>
      </Card>
      <Card size="small" title={`环境（${envs.length}）`}>
        <Table<DdcEnv>
          rowKey={(row) => row.id}
          columns={columns}
          dataSource={envs}
          loading={loading}
          size="small"
          pagination={{ pageSize: 10, size: 'small' }}
        />
      </Card>
      <Modal
        open={open}
        title={editing ? '编辑环境' : '新建环境'}
        onCancel={() => setOpen(false)}
        onOk={() => void save()}
        okText="保存"
        destroyOnHidden
      >
        <Form<EnvFormValues> form={form} layout="vertical" initialValues={{ enabled: true, sortOrder: 0 }}>
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
