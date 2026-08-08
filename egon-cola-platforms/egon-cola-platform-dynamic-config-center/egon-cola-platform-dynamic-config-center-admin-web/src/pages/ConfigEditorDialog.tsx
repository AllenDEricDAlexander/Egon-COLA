import { useEffect, useState } from 'react'
import { App, Form, Input, Modal, Space, Tag, Typography } from 'antd'
import { ddcApi } from '../api/client'
import type { DdcConfig, DdcNamespaceEnvAppBinding } from '../api/types'
import ScopeSelects, { type ScopeValue } from '../components/scope/ScopeSelects'

export type ConfigScope = ScopeValue

type Props = {
  open: boolean
  config: DdcConfig | null
  defaultScope: ConfigScope
  onClose: () => void
  onSaved: () => void
}

type FormValues = {
  content: string
  description?: string
  changeReason?: string
}

const ensureBinding = async (scope: ConfigScope) => {
  const params = new URLSearchParams({
    bizCode: scope.bizCode,
    namespaceCode: scope.namespaceCode,
    env: scope.env,
    appCode: scope.appCode,
  })
  const bindings = await ddcApi<DdcNamespaceEnvAppBinding[]>(
    `/api/v1/ddc/namespace-env-app-bindings?${params.toString()}`,
  )
  if (!bindings.some((binding) => binding.enabled)) {
    throw new Error('当前命名空间、环境与应用尚未绑定，请先在命名空间管理中配置绑定')
  }
}

export default function ConfigEditorDialog({ open, config, defaultScope, onClose, onSaved }: Props) {
  const [form] = Form.useForm<FormValues>()
  const { message } = App.useApp()
  const [scope, setScope] = useState<ConfigScope>(defaultScope)
  const [saving, setSaving] = useState(false)
  const editing = Boolean(config?.id)

  useEffect(() => {
    if (!open) return
    const scope = editing && config
      ? {
          bizCode: config.bizCode,
          namespaceCode: config.visibleNamespaces[0] ?? '',
          env: config.env,
          appCode: config.appCode,
        }
      : defaultScope
    let cancelled = false
    void Promise.resolve().then(() => {
      if (cancelled) return
      setScope(scope)
      form.setFieldsValue({
        content: config?.content ?? '',
        description: config?.description ?? '',
        changeReason: '',
      })
    })
    return () => {
      cancelled = true
    }
  }, [open, config, defaultScope, editing, form])

  const save = async () => {
    let values: FormValues
    try {
      values = await form.validateFields()
    } catch {
      return // antd 表单校验失败已就地展示
    }
    if (scope.bizCode.trim() === '' || scope.namespaceCode.trim() === '' || scope.env.trim() === '' || scope.appCode.trim() === '') {
      message.warning('请填写业务域 / 命名空间 / 环境 / 应用')
      return
    }
    setSaving(true)
    try {
      if (editing && config) {
        await ddcApi(`/api/v1/ddc/configs/${encodeURIComponent(config.id)}`, {
          method: 'PUT',
          body: {
            content: values.content,
            changeReason: values.changeReason || 'DDC Admin Web update',
            currentVersion: config.currentVersion,
          },
        })
      } else {
        await ensureBinding(scope)
        await ddcApi('/api/v1/ddc/configs', {
          method: 'POST',
          body: {
            ...scope,
            resourceName: 'application.yml',
            content: values.content,
            format: 'YAML',
            description: values.description,
          },
        })
      }
      message.success('配置已保存')
      onSaved()
    } catch (error) {
      message.error(error instanceof Error ? error.message : String(error))
    } finally {
      setSaving(false)
    }
  }

  return (
    <Modal
      open={open}
      title={editing ? '编辑 application.yml' : '新建 application.yml'}
      onCancel={onClose}
      onOk={() => void save()}
      okText="保存"
      confirmLoading={saving}
      destroyOnHidden
      width={860}
    >
      <Form<FormValues> form={form} layout="vertical">
        <Form.Item label="业务域 / 命名空间 / 环境 / 应用" required shouldUpdate>
          {() => (
            <ScopeSelects
              value={scope}
              onChange={setScope}
              disabled={editing}
            />
          )}
        </Form.Item>
        <Space style={{ marginBottom: 12 }}>
          <Typography.Text code>application.yml</Typography.Text>
          <Tag color="blue">YAML</Tag>
        </Space>
        <Form.Item
          name="content"
          label="YAML 内容"
          rules={[{ required: true, whitespace: true }]}
        >
          <Input.TextArea
            rows={16}
            spellCheck={false}
            style={{ fontFamily: 'monospace' }}
          />
        </Form.Item>
        {!editing && (
          <Form.Item name="description" label="描述">
            <Input />
          </Form.Item>
        )}
        {editing && (
          <Form.Item name="changeReason" label="变更原因">
            <Input placeholder="DDC Admin Web update" />
          </Form.Item>
        )}
      </Form>
    </Modal>
  )
}
