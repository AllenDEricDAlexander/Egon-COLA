import { useRef, useState } from 'react'
import { Button, Card, Space, Table, Tag, Typography, message } from 'antd'
import { ddcApi } from '../api/client'
import type { DdcCacheCheckRow } from '../api/types'
import ScopeSelects, { type ScopeValue as CacheFilter } from '../components/scope/ScopeSelects'
import { buildQuery } from '../lib/query'

export default function CachePage() {
  const [draft, setDraft] = useState<CacheFilter>({ bizCode: '', namespaceCode: '', env: '', appCode: '' })
  const [rows, setRows] = useState<DdcCacheCheckRow[]>([])
  const [checking, setChecking] = useState(false)
  const [rebuilding, setRebuilding] = useState(false)
  const filterRef = useRef<CacheFilter>({ bizCode: '', namespaceCode: '', env: '', appCode: '' })

  const scopeReady = () => {
    const scope = filterRef.current
    return scope.bizCode.trim() !== '' && scope.appCode.trim() !== '' && scope.env.trim() !== ''
  }

  const check = async () => {
    filterRef.current = { ...draft }
    if (!scopeReady()) {
      message.warning('请填写 bizCode / env / appCode')
      return
    }
    setChecking(true)
    try {
      const { bizCode, env, appCode } = filterRef.current
      const data = await ddcApi<DdcCacheCheckRow[]>(`/api/v1/ddc/cache/check?${buildQuery({ bizCode, env, appCode })}`)
      setRows(data ?? [])
    } catch (error) {
      message.error(error instanceof Error ? error.message : String(error))
    } finally {
      setChecking(false)
    }
  }

  const rebuild = async () => {
    filterRef.current = { ...draft }
    if (!scopeReady()) {
      message.warning('请填写 bizCode / env / appCode')
      return
    }
    if (!window.confirm('确认重建该作用域下的缓存？')) return
    setRebuilding(true)
    try {
      const { bizCode, env, appCode } = filterRef.current
      const count = await ddcApi<number>(`/api/v1/ddc/cache/rebuild?${buildQuery({ bizCode, env, appCode })}`, {
        method: 'POST',
      })
      message.success(`已重建 ${count ?? 0} 项缓存`)
    } catch (error) {
      message.error(error instanceof Error ? error.message : String(error))
    } finally {
      setRebuilding(false)
    }
  }

  const columns = [
    { title: '配置 Key', dataIndex: 'configKey', key: 'configKey', render: (value: string) => <Typography.Text code>{value}</Typography.Text> },
    {
      title: '一致性',
      dataIndex: 'matched',
      key: 'matched',
      render: (matched: boolean) => <Tag color={matched ? 'green' : 'red'}>{matched ? '一致' : '不一致'}</Tag>,
    },
    { title: 'DB 版本', dataIndex: 'databaseVersion', key: 'databaseVersion' },
    { title: 'Redis 版本', dataIndex: 'redisVersion', key: 'redisVersion' },
    {
      title: 'DB 值',
      dataIndex: 'databaseValue',
      key: 'databaseValue',
      render: (value?: string) => <Typography.Text code ellipsis style={{ maxWidth: 200 }}>{value ?? '—'}</Typography.Text>,
    },
    {
      title: 'Redis 值',
      dataIndex: 'redisValue',
      key: 'redisValue',
      render: (value?: string) => <Typography.Text code ellipsis style={{ maxWidth: 200 }}>{value ?? '—'}</Typography.Text>,
    },
  ]

  return (
    <div>
      <Typography.Title level={3}>缓存管理</Typography.Title>
      <Card size="small" style={{ marginBottom: 16 }}>
        <Space wrap>
          <ScopeSelects
            value={{ bizCode: draft.bizCode, namespaceCode: draft.namespaceCode, env: draft.env, appCode: draft.appCode }}
            onChange={(scope) => setDraft({ ...draft, ...scope })}
          />
          <Button type="primary" loading={checking} onClick={() => void check()}>检查缓存</Button>
          <Button danger loading={rebuilding} onClick={() => void rebuild()}>重建缓存</Button>
        </Space>
      </Card>
      <Card size="small" title={`检查结果（${rows.length}）`}>
        <Table<DdcCacheCheckRow>
          rowKey={(row) => row.configKey}
          columns={columns}
          dataSource={rows}
          size="small"
          pagination={{ pageSize: 10, size: 'small' }}
        />
      </Card>
    </div>
  )
}
