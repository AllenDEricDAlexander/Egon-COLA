import { useCallback, useEffect, useRef, useState } from 'react'
import { Button, Card, Form, Input, Modal, Popconfirm, Space, Switch, Table, Tag, Typography, message } from 'antd'
import { ddcApi } from '../api/client'
import type { DdcBiz } from '../api/types'
import { formatTime } from '../lib/query'

type BizFormValues = {
  bizCode: string
  bizName: string
  description?: string
  enabled: boolean
}

export default function BizsPage() {
  const [draft, setDraft] = useState('')
  const [bizs, setBizs] = useState<DdcBiz[]>([])
  const [loading, setLoading] = useState(false)
  const [open, setOpen] = useState(false)
  const [editing, setEditing] = useState<DdcBiz | null>(null)
  const keywordRef = useRef('')
  const [form] = Form.useForm<BizFormValues>()

  const loadBizs = useCallback(async () => {
    const keyword = keywordRef.current
    const data = await ddcApi<DdcBiz[]>(keyword === ''
      ? '/api/v1/ddc/bizs'
      : `/api/v1/ddc/bizs?keyword=${encodeURIComponent(keyword)}`)
    setBizs(data ?? [])
  }, [])

  useEffect(() => {
    loadBizs().catch((error) => {
      message.error(error instanceof Error ? error.message : String(error))
    })
  }, [loadBizs])

  const refresh = async () => {
    setLoading(true)
    try {
      await loadBizs()
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
    let values: BizFormValues
    try {
      values = await form.validateFields()
    } catch {
      return
    }
    try {
      if (editing) {
        await ddcApi(`/api/v1/ddc/bizs/${encodeURIComponent(editing.bizCode)}`, {
          method: 'PUT',
          body: { ...values },
        })
      } else {
        await ddcApi('/api/v1/ddc/bizs', {
          method: 'POST',
          body: { ...values },
        })
      }
      message.success('业务域已保存')
      setOpen(false)
      await refresh()
    } catch (error) {
      message.error(error instanceof Error ? error.message : String(error))
    }
  }

  const toggleEnabled = async (biz: DdcBiz, enabled: boolean) => {
    try {
      await ddcApi(`/api/v1/ddc/bizs/${encodeURIComponent(biz.bizCode)}/enabled?enabled=${enabled}`, {
        method: 'PUT',
      })
      await refresh()
    } catch (error) {
      message.error(error instanceof Error ? error.message : String(error))
    }
  }

  const remove = async (biz: DdcBiz) => {
    try {
      await ddcApi(`/api/v1/ddc/bizs/${encodeURIComponent(biz.bizCode)}`, {
        method: 'DELETE',
      })
      message.success('业务域已删除')
      await refresh()
    } catch (error) {
      message.error(error instanceof Error ? error.message : String(error))
    }
  }

  const columns = [
    { title: '业务域编码', dataIndex: 'bizCode', key: 'bizCode', render: (value: string) => <Typography.Text code>{value}</Typography.Text> },
    { title: '名称', dataIndex: 'bizName', key: 'bizName' },
    { title: '描述', dataIndex: 'description', key: 'description' },
    {
      title: '启用',
      key: 'enabled',
      render: (_: unknown, row: DdcBiz) => (
        <Switch checked={row.enabled} onChange={(checked) => void toggleEnabled(row, checked)} />
      ),
    },
    { title: '更新时间', dataIndex: 'updatedAt', key: 'updatedAt', render: formatTime },
    {
      title: '操作',
      key: 'actions',
      render: (_: unknown, row: DdcBiz) => (
        <Space>
          <Button size="small" onClick={() => openEdit(row)}>编辑</Button>
          <Popconfirm title={`确认删除业务域 ${row.bizCode}？`} onConfirm={() => void remove(row)}>
            <Button size="small" danger>删除</Button>
          </Popconfirm>
        </Space>
      ),
    },
  ]

  return (
    <div>
      <Typography.Title level={3}>业务域管理</Typography.Title>
      <Card size="small" style={{ marginBottom: 16 }}>
        <Space wrap>
          <Input
            placeholder="名称模糊查询"
            value={draft}
            onChange={(event) => setDraft(event.target.value)}
            style={{ width: 200 }}
          />
          <Button type="primary" onClick={applyFilter}>查询</Button>
          <Button onClick={openCreate}>新建业务域</Button>
        </Space>
      </Card>
      <Card size="small" title={`业务域（${bizs.length}）`}>
        <Table<DdcBiz>
          rowKey={(row) => row.id}
          columns={columns}
          dataSource={bizs}
          loading={loading}
          size="small"
          pagination={{ pageSize: 10, size: 'small' }}
        />
      </Card>
      <Modal
        open={open}
        title={editing ? '编辑业务域' : '新建业务域'}
        onCancel={() => setOpen(false)}
        onOk={() => void save()}
        okText="保存"
        destroyOnHidden
      >
        <Form<BizFormValues> form={form} layout="vertical" initialValues={{ enabled: true }}>
          <Form.Item name="bizCode" label="业务域编码" rules={[{ required: true }]}>
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
