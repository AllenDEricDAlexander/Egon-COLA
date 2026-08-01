import { useCallback, useEffect, useRef, useState } from 'react'
import { Button, Card, Input, Modal, Space, Table, Tag, Typography, message } from 'antd'
import { ddcApi } from '../api/client'
import type { DdcConfig, DdcConfigVersion, DdcPublishResult } from '../api/types'
import ScopeSelects, { type ScopeValue } from '../components/scope/ScopeSelects'
import { prepareConfigEditor, detectConfigFormat } from '../lib/configFormat'
import { buildQuery, formatTime } from '../lib/query'
import { uuidV7 } from '../lib/uuid'
import ConfigEditorDialog, { type ConfigScope } from './ConfigEditorDialog'

type ConfigFilter = ScopeValue & { configKey: string }

export default function ConfigsPage() {
  const [draft, setDraft] = useState<ConfigFilter>({ bizCode: '', namespaceCode: '', env: '', appCode: '', configKey: '' })
  const [configs, setConfigs] = useState<DdcConfig[]>([])
  const [loading, setLoading] = useState(false)
  const filterRef = useRef<ConfigFilter>({ bizCode: '', namespaceCode: '', env: '', appCode: '', configKey: '' })
  const [dialogOpen, setDialogOpen] = useState(false)
  const [editingConfig, setEditingConfig] = useState<DdcConfig | null>(null)
  const [versionsConfig, setVersionsConfig] = useState<DdcConfig | null>(null)
  const [versions, setVersions] = useState<DdcConfigVersion[]>([])
  const [versionsLoading, setVersionsLoading] = useState(false)

  const loadConfigs = useCallback(async () => {
    const scope = filterRef.current
    const data = await ddcApi<DdcConfig[]>(
      `/api/v1/ddc/configs?${buildQuery({ ...scope, includeDeleted: false })}`,
    )
    const unique = new Map((data ?? []).map((config) => [config.id, config]))
    setConfigs([...unique.values()])
  }, [])

  useEffect(() => {
    loadConfigs().catch((error) => {
      message.error(error instanceof Error ? error.message : String(error))
    })
  }, [loadConfigs])

  const refresh = async () => {
    setLoading(true)
    try {
      await loadConfigs()
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

  const openNewDialog = () => {
    setEditingConfig(null)
    setDialogOpen(true)
  }

  const openEditDialog = (config: DdcConfig) => {
    setEditingConfig(config)
    setDialogOpen(true)
  }

  const defaultScope: ConfigScope = {
    bizCode: draft.bizCode,
    namespaceCode: draft.namespaceCode,
    env: draft.env,
    appCode: draft.appCode,
  }

  const publish = async (config: DdcConfig) => {
    if (!window.confirm(`确认发布 ${config.configKey} 当前版本？`)) return
    try {
      const result = await ddcApi<DdcPublishResult>(`/api/v1/ddc/configs/${encodeURIComponent(config.id)}/publish`, {
        method: 'POST',
        body: {
          changeId: uuidV7(),
          configValue: config.configValue,
          expectedVersion: config.currentVersion,
          timeoutMs: 30000,
        },
      })
      message.success(`发布任务 ${result.changeId}：${result.status}`)
      await refresh()
    } catch (error) {
      message.error(error instanceof Error ? error.message : String(error))
    }
  }

  const remove = async (config: DdcConfig) => {
    if (!window.confirm(`确认删除 ${config.configKey}？`)) return
    try {
      await ddcApi(`/api/v1/ddc/configs/${encodeURIComponent(config.id)}`, {
        method: 'DELETE',
      })
      message.success('配置已删除')
      await refresh()
    } catch (error) {
      message.error(error instanceof Error ? error.message : String(error))
    }
  }

  const openVersions = async (config: DdcConfig) => {
    setVersionsConfig(config)
    setVersionsLoading(true)
    setVersions([])
    try {
      const data = await ddcApi<DdcConfigVersion[]>(`/api/v1/ddc/configs/${encodeURIComponent(config.id)}/versions`)
      setVersions(data ?? [])
    } catch (error) {
      message.error(error instanceof Error ? error.message : String(error))
    } finally {
      setVersionsLoading(false)
    }
  }

  const rollback = async (config: DdcConfig, version: DdcConfigVersion) => {
    if (!window.confirm(`确认回滚到版本 ${version.version}？`)) return
    try {
      await ddcApi(`/api/v1/ddc/configs/${encodeURIComponent(config.id)}/rollback`, {
        method: 'POST',
        body: { configId: config.id, version: version.version, reason: 'rollback from DDC Admin Web' },
      })
      message.success('已回滚')
      setVersionsConfig(null)
      await refresh()
    } catch (error) {
      message.error(error instanceof Error ? error.message : String(error))
    }
  }

  const columns = [
    { title: '配置 Key', dataIndex: 'configKey', key: 'configKey', render: (value: string) => <Typography.Text code>{value}</Typography.Text> },
    { title: '值类型', dataIndex: 'valueType', key: 'valueType' },
    {
      title: '可见命名空间',
      dataIndex: 'visibleNamespaces',
      key: 'visibleNamespaces',
      render: (values: string[] = []) => (
        <Space size={[4, 4]} wrap>
          {values.length === 0 ? '—' : values.map((value) => <Tag key={value}>{value}</Tag>)}
        </Space>
      ),
    },
    {
      title: '格式',
      key: 'format',
      render: (_: unknown, row: DdcConfig) => <Tag color="blue">{detectConfigFormat(row)}</Tag>,
    },
    {
      title: '配置值',
      key: 'value',
      render: (_: unknown, row: DdcConfig) => {
        const content = prepareConfigEditor(row).content.replace(/\s+/g, ' ').trim()
        const preview = content.length > 96 ? `${content.slice(0, 96)}…` : content
        return (
          <span>
            <Typography.Text code>{preview}</Typography.Text>
            {row.description && (
              <Typography.Text type="secondary" style={{ display: 'block', fontSize: 12 }}>
                {row.description}
              </Typography.Text>
            )}
          </span>
        )
      },
    },
    { title: '当前版本', dataIndex: 'currentVersion', key: 'currentVersion' },
    { title: '更新时间', dataIndex: 'updatedAt', key: 'updatedAt', render: formatTime },
    {
      title: '操作',
      key: 'actions',
      render: (_: unknown, row: DdcConfig) => (
        <Space>
          <Button size="small" onClick={() => openEditDialog(row)}>编辑</Button>
          <Button size="small" type="primary" onClick={() => void publish(row)}>发布</Button>
          <Button size="small" danger onClick={() => void remove(row)}>删除</Button>
          <Button size="small" onClick={() => void openVersions(row)}>版本</Button>
        </Space>
      ),
    },
  ]

  return (
    <div>
      <Typography.Title level={3}>配置中心管理</Typography.Title>
      <Card size="small" style={{ marginBottom: 16 }}>
        <Space wrap>
          <ScopeSelects
            value={{ bizCode: draft.bizCode, namespaceCode: draft.namespaceCode, env: draft.env, appCode: draft.appCode }}
            onChange={(scope) => setDraft({ ...draft, ...scope })}
          />
          <Input placeholder="configKey" value={draft.configKey} onChange={(event) => setDraft({ ...draft, configKey: event.target.value })} style={{ width: 180 }} />
          <Button type="primary" onClick={applyFilter}>查询</Button>
          <Button onClick={openNewDialog}>新建配置</Button>
        </Space>
      </Card>
      <Card size="small" title={`配置（${configs.length}）`}>
        <Table<DdcConfig>
          rowKey={(row) => row.id}
          columns={columns}
          dataSource={configs}
          loading={loading}
          size="small"
          pagination={{ pageSize: 10, size: 'small' }}
        />
      </Card>
      <ConfigEditorDialog
        open={dialogOpen}
        config={editingConfig}
        defaultScope={defaultScope}
        onClose={() => setDialogOpen(false)}
        onSaved={() => {
          setDialogOpen(false)
          void refresh()
        }}
      />
      <Modal
        open={versionsConfig !== null}
        title={`版本历史：${versionsConfig?.configKey ?? ''}`}
        onCancel={() => setVersionsConfig(null)}
        footer={null}
        width={860}
      >
        <Table<DdcConfigVersion>
          rowKey={(row) => row.id}
          dataSource={versions}
          loading={versionsLoading}
          size="small"
          pagination={{ pageSize: 10, size: 'small' }}
          columns={[
            { title: '版本', dataIndex: 'version', key: 'version' },
            { title: '类型', dataIndex: 'changeType', key: 'changeType' },
            { title: '变更原因', dataIndex: 'changeReason', key: 'changeReason' },
            { title: '操作人', dataIndex: 'operator', key: 'operator' },
            { title: '时间', dataIndex: 'createdAt', key: 'createdAt', render: formatTime },
            {
              title: '新值',
              dataIndex: 'newValue',
              key: 'newValue',
              render: (value: string) => (
                <Typography.Text code ellipsis style={{ maxWidth: 220 }}>
                  {value?.replace(/\s+/g, ' ').slice(0, 60) ?? '—'}
                </Typography.Text>
              ),
            },
            {
              title: '操作',
              key: 'actions',
              render: (_: unknown, row: DdcConfigVersion) =>
                versionsConfig && (
                  <Button size="small" onClick={() => void rollback(versionsConfig, row)}>回滚</Button>
                ),
            },
          ]}
        />
      </Modal>
    </div>
  )
}
