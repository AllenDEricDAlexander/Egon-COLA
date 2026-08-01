import { useCallback, useEffect, useRef, useState } from 'react'
import { Button, Card, Checkbox, Form, Input, Modal, Popconfirm, Space, Switch, Table, Typography, message } from 'antd'
import { ddcApi } from '../api/client'
import type { DdcApp, DdcEnv, DdcNamespace, DdcNamespaceEnvAppBinding } from '../api/types'
import BizSelect from '../components/scope/BizSelect'
import { formatTime } from '../lib/query'

type NamespaceFilter = { bizCode: string; keyword: string }

type NamespaceFormValues = {
  bizCode: string
  namespaceCode: string
  namespace: string
  description?: string
  enabled: boolean
}

export default function NamespacesPage() {
  const [draft, setDraft] = useState<NamespaceFilter>({ bizCode: '', keyword: '' })
  const [namespaces, setNamespaces] = useState<DdcNamespace[]>([])
  const [loading, setLoading] = useState(false)
  const [open, setOpen] = useState(false)
  const [editing, setEditing] = useState<DdcNamespace | null>(null)
  const filterRef = useRef<NamespaceFilter>({ bizCode: '', keyword: '' })
  const [form] = Form.useForm<NamespaceFormValues>()
  const [bindingNamespace, setBindingNamespace] = useState<DdcNamespace | null>(null)
  const [bindingLoading, setBindingLoading] = useState(false)
  const [bindings, setBindings] = useState<DdcNamespaceEnvAppBinding[]>([])
  const [bindingApps, setBindingApps] = useState<DdcApp[]>([])
  const [bindingEnvs, setBindingEnvs] = useState<DdcEnv[]>([])
  const [bindingDraft, setBindingDraft] = useState<Record<string, string[]>>({})

  const loadNamespaces = useCallback(async () => {
    const { bizCode, keyword } = filterRef.current
    const params = new URLSearchParams()
    if (bizCode.trim() !== '') params.set('bizCode', bizCode.trim())
    if (keyword.trim() !== '') params.set('keyword', keyword.trim())
    const query = params.toString()
    const data = await ddcApi<DdcNamespace[]>(
      `/api/v1/ddc/namespaces${query === '' ? '' : `?${query}`}`,
    )
    setNamespaces(data ?? [])
  }, [])

  useEffect(() => {
    void Promise.resolve().then(loadNamespaces).catch((error) => {
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
      bizCode: item.bizCode,
      namespaceCode: item.namespaceCode,
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

  const openBindings = async (item: DdcNamespace) => {
    setBindingNamespace(item)
    setBindingLoading(true)
    try {
      const params = new URLSearchParams({
        bizCode: item.bizCode,
        namespaceCode: item.namespaceCode,
      })
      const [currentBindings, envs, apps] = await Promise.all([
        ddcApi<DdcNamespaceEnvAppBinding[]>(
          `/api/v1/ddc/namespace-env-app-bindings?${params.toString()}`,
        ),
        ddcApi<DdcEnv[]>('/api/v1/ddc/envs'),
        ddcApi<DdcApp[]>(`/api/v1/ddc/apps?bizCode=${encodeURIComponent(item.bizCode)}`),
      ])
      const nextDraft: Record<string, string[]> = {}
      ;(envs ?? []).forEach((env) => {
        nextDraft[env.envCode] = (currentBindings ?? [])
          .filter((binding) => binding.enabled && binding.env === env.envCode)
          .map((binding) => binding.appCode)
      })
      setBindings(currentBindings ?? [])
      setBindingEnvs(envs ?? [])
      setBindingApps(apps ?? [])
      setBindingDraft(nextDraft)
    } catch (error) {
      setBindingNamespace(null)
      message.error(error instanceof Error ? error.message : String(error))
    } finally {
      setBindingLoading(false)
    }
  }

  const saveBindings = async () => {
    if (!bindingNamespace) return
    setBindingLoading(true)
    try {
      const changes: Promise<unknown>[] = []
      bindingEnvs.forEach((env) => {
        const desired = new Set(bindingDraft[env.envCode] ?? [])
        const current = bindings.filter((binding) => binding.env === env.envCode)
        desired.forEach((appCode) => {
          const existing = current.find((binding) => binding.appCode === appCode)
          const body = {
            bizCode: bindingNamespace.bizCode,
            namespaceCode: bindingNamespace.namespaceCode,
            env: env.envCode,
            appCode,
            enabled: true,
          }
          if (!existing) {
            changes.push(ddcApi('/api/v1/ddc/namespace-env-app-bindings', {
              method: 'POST',
              body,
            }))
          } else if (!existing.enabled) {
            changes.push(ddcApi(`/api/v1/ddc/namespace-env-app-bindings/${encodeURIComponent(existing.id)}`, {
              method: 'PUT',
              body,
            }))
          }
        })
        current
          .filter((binding) => binding.enabled && !desired.has(binding.appCode))
          .forEach((binding) => changes.push(ddcApi(
            `/api/v1/ddc/namespace-env-app-bindings/${encodeURIComponent(binding.id)}`,
            { method: 'DELETE' },
          )))
      })
      await Promise.all(changes)
      message.success('命名空间绑定已保存')
      setBindingNamespace(null)
    } catch (error) {
      message.error(error instanceof Error ? error.message : String(error))
    } finally {
      setBindingLoading(false)
    }
  }

  const columns = [
    { title: '业务域', dataIndex: 'bizCode', key: 'bizCode', render: (value: string) => <Typography.Text code>{value}</Typography.Text> },
    { title: '编码', dataIndex: 'namespaceCode', key: 'namespaceCode', render: (value: string) => <Typography.Text code>{value}</Typography.Text> },
    { title: '名称', dataIndex: 'namespace', key: 'namespace' },
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
          <Button size="small" onClick={() => void openBindings(row)}>管理绑定</Button>
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
            <BizSelect
              value={draft.bizCode}
              onChange={(bizCode) => setDraft({ ...draft, bizCode })}
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
          <Form.Item name="bizCode" label="业务域" rules={[{ required: true }]}>
            <BizSelect disabled={Boolean(editing)} />
          </Form.Item>
          <Form.Item name="namespaceCode" label="编码" rules={[{ required: true }]}>
            <Input disabled={Boolean(editing)} placeholder="全局唯一" />
          </Form.Item>
          <Form.Item name="namespace" label="名称" rules={[{ required: true }]}>
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
      <Modal
        open={bindingNamespace !== null}
        title={`管理绑定：${bindingNamespace?.bizCode ?? ''}/${bindingNamespace?.namespaceCode ?? ''}`}
        onCancel={() => setBindingNamespace(null)}
        onOk={() => void saveBindings()}
        okText="保存绑定"
        confirmLoading={bindingLoading}
        width={860}
      >
        <Table<DdcEnv>
          rowKey={(row) => row.envCode}
          loading={bindingLoading}
          dataSource={bindingEnvs}
          pagination={false}
          size="small"
          columns={[
            {
              title: '环境',
              dataIndex: 'envCode',
              key: 'envCode',
              width: 160,
              render: (value: string) => <Typography.Text code>{value}</Typography.Text>,
            },
            {
              title: '可见应用（多选）',
              key: 'apps',
              render: (_: unknown, env: DdcEnv) => (
                <Checkbox.Group
                  value={bindingDraft[env.envCode] ?? []}
                  options={bindingApps.map((app) => ({
                    value: app.appCode,
                    label: `${app.appCode}（${app.appName}）`,
                  }))}
                  onChange={(values) => setBindingDraft({
                    ...bindingDraft,
                    [env.envCode]: values.map(String),
                  })}
                />
              ),
            },
          ]}
        />
      </Modal>
    </div>
  )
}
