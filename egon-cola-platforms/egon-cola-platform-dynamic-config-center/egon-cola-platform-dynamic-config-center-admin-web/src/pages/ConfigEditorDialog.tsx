import { useEffect, useState } from 'react'
import { Form, Input, Modal, Select, Tag, Typography, message } from 'antd'
import { ddcApi } from '../api/client'
import type { DdcConfig, DdcNamespaceEnvAppBinding } from '../api/types'
import ScopeSelects, { type ScopeValue } from '../components/scope/ScopeSelects'
import { prepareConfigEditor, serializeConfigEditor, type ConfigEditor } from '../lib/configFormat'

export type ConfigScope = ScopeValue

type Props = {
  open: boolean
  config: DdcConfig | null
  defaultScope: ConfigScope
  onClose: () => void
  onSaved: () => void
}

type FormValues = {
  bizCode: string
  appCode: string
  env: string
  namespaceCode: string
  configKey: string
  valueType: string
  configValue: string
  defaultValue?: string
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
  const [editor, setEditor] = useState<ConfigEditor | null>(null)
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
    const valueEditor = prepareConfigEditor({
      configKey: config?.configKey ?? '',
      configValue: config?.configValue ?? '',
      valueType: config?.valueType ?? 'STRING',
    })
    let cancelled = false
    void Promise.resolve().then(() => {
      if (cancelled) return
      setEditor(valueEditor)
      form.setFieldsValue({
        bizCode: scope.bizCode,
        namespaceCode: scope.namespaceCode,
        env: scope.env,
        appCode: scope.appCode,
        configKey: config?.configKey ?? '',
        valueType: config?.valueType ?? 'STRING',
        configValue: valueEditor.content,
        defaultValue: prepareConfigEditor({
          configKey: config?.configKey ?? '',
          configValue: config?.defaultValue ?? '',
          valueType: config?.valueType ?? 'STRING',
        }).content,
        description: config?.description ?? '',
        changeReason: '',
      })
    })
    return () => {
      cancelled = true
    }
  }, [open, config, defaultScope, editing, form])

  const refreshEditor = (configValue: string, configKey?: string, valueType?: string) => {
    setEditor(prepareConfigEditor({
      configKey: configKey ?? form.getFieldValue('configKey'),
      configValue,
      valueType: valueType ?? form.getFieldValue('valueType'),
    }))
  }

  const save = async () => {
    let values: FormValues
    try {
      values = await form.validateFields()
    } catch {
      return // antd 表单校验失败已就地展示
    }
    // 作用域字段经 ScopeSelects 写入表单但未注册 Form.Item，需手动校验。
    const allValues = form.getFieldsValue() as FormValues
    const scope = {
      bizCode: allValues.bizCode ?? '',
      namespaceCode: allValues.namespaceCode ?? '',
      env: allValues.env ?? '',
      appCode: allValues.appCode ?? '',
    }
    if (scope.bizCode.trim() === '' || scope.namespaceCode.trim() === '' || scope.env.trim() === '' || scope.appCode.trim() === '') {
      message.warning('请填写业务域 / 命名空间 / 环境 / 应用')
      return
    }
    setSaving(true)
    try {
      const currentEditor = editor ?? prepareConfigEditor(values)
      const configValue = await serializeConfigEditor(currentEditor, values.configValue)
      if (editing && config) {
        await ddcApi(`/api/v1/ddc/configs/${encodeURIComponent(config.id)}`, {
          method: 'PUT',
          body: {
            configValue,
            changeReason: values.changeReason || 'DDC Admin Web update',
            currentVersion: config.currentVersion,
          },
        })
      } else {
        await ensureBinding(scope)
        let defaultValue = ''
        if (String(values.defaultValue ?? '').trim() !== '') {
          const defaultEditor = prepareConfigEditor({
            configKey: values.configKey,
            configValue: values.defaultValue,
            valueType: values.valueType,
          })
          defaultValue = await serializeConfigEditor(defaultEditor, values.defaultValue ?? '')
        }
        await ddcApi('/api/v1/ddc/configs', {
          method: 'POST',
          body: {
            ...scope,
            configKey: values.configKey,
            configValue,
            defaultValue,
            valueType: values.valueType,
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
      title={editing ? '编辑配置' : '新建配置'}
      onCancel={onClose}
      onOk={() => void save()}
      okText="保存"
      confirmLoading={saving}
      destroyOnHidden
      width={720}
    >
      <Form<FormValues> form={form} layout="vertical">
        <Form.Item label="业务域 / 命名空间 / 环境 / 应用" required shouldUpdate>
          {() => (
            <ScopeSelects
              value={{
                bizCode: form.getFieldValue('bizCode') ?? '',
                namespaceCode: form.getFieldValue('namespaceCode') ?? '',
                env: form.getFieldValue('env') ?? '',
                appCode: form.getFieldValue('appCode') ?? '',
              }}
              onChange={(scope) => form.setFieldsValue(scope)}
              disabled={editing}
            />
          )}
        </Form.Item>
        <div style={{ display: 'grid', gridTemplateColumns: '2fr 1fr', gap: 12 }}>
          <Form.Item name="configKey" label="配置 Key" rules={[{ required: true }]}>
            <Input disabled={editing} />
          </Form.Item>
          <Form.Item name="valueType" label="值类型" rules={[{ required: true }]}>
            <Select
              options={['STRING', 'JSON', 'INTEGER', 'BOOLEAN', 'YAML', 'TOML'].map((value) => ({ value, label: value }))}
            />
          </Form.Item>
        </div>
        <Form.Item
          name="configValue"
          label="配置值"
          rules={[{ required: true }]}
        >
          <Input.TextArea
            rows={10}
            onChange={(event) => refreshEditor(event.target.value)}
            style={{ fontFamily: 'monospace' }}
          />
        </Form.Item>
        <div style={{ marginTop: -12, marginBottom: 12 }}>
          {editor && (
            <>
              <Tag color="blue">{editor.format}</Tag>
              <Typography.Text type="secondary">{editor.notice}</Typography.Text>
            </>
          )}
        </div>
        {!editing && (
          <Form.Item name="defaultValue" label="默认值">
            <Input.TextArea
              rows={4}
              onChange={(event) => refreshEditor(event.target.value)}
              style={{ fontFamily: 'monospace' }}
            />
          </Form.Item>
        )}
        <Form.Item name="description" label="描述">
          <Input />
        </Form.Item>
        {editing && (
          <Form.Item name="changeReason" label="变更原因">
            <Input placeholder="DDC Admin Web update" />
          </Form.Item>
        )}
      </Form>
    </Modal>
  )
}
