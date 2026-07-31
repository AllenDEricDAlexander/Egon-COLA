import { useCallback, useEffect, useRef, useState } from 'react'
import { Button, Card, Descriptions, Table, Tag, Typography, message } from 'antd'
import { ddcApi } from '../api/client'
import type { DdcInstance } from '../api/types'
import ScopeSelects, { type ScopeValue as InstanceFilter } from '../components/scope/ScopeSelects'
import { buildQuery, formatTime } from '../lib/query'

export default function InstancesPage() {
  const [draft, setDraft] = useState<InstanceFilter>({ appCode: '', env: '', namespace: '' })
  const [instances, setInstances] = useState<DdcInstance[]>([])
  const [loading, setLoading] = useState(false)
  const filterRef = useRef<InstanceFilter>({ appCode: '', env: '', namespace: '' })

  const loadInstances = useCallback(async () => {
    const scope = filterRef.current
    if (scope.appCode.trim() === '' || scope.env.trim() === '' || scope.namespace.trim() === '') {
      setInstances([])
      return
    }
    const data = await ddcApi<DdcInstance[]>(`/api/v1/ddc/instances?${buildQuery(scope)}`)
    setInstances(data ?? [])
  }, [])

  useEffect(() => {
    loadInstances().catch((error) => {
      message.error(error instanceof Error ? error.message : String(error))
    })
  }, [loadInstances])

  const applyFilter = () => {
    filterRef.current = { ...draft }
    void refresh()
  }

  const refresh = async () => {
    setLoading(true)
    try {
      await loadInstances()
    } catch (error) {
      message.error(error instanceof Error ? error.message : String(error))
    } finally {
      setLoading(false)
    }
  }

  const columns = [
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      render: (status: string) => <Tag color={status === 'ONLINE' ? 'green' : 'default'}>{status ?? 'UNKNOWN'}</Tag>,
    },
    { title: '实例 ID', dataIndex: 'instanceId', key: 'instanceId', render: (value: string) => <Typography.Text code>{value}</Typography.Text> },
    {
      title: '地址',
      key: 'address',
      render: (_: unknown, row: DdcInstance) => (
        <Typography.Text code>{`${row.host}:${row.port}`}</Typography.Text>
      ),
    },
    { title: 'PID', dataIndex: 'pid', key: 'pid' },
    { title: 'SDK 版本', dataIndex: 'sdkVersion', key: 'sdkVersion' },
    { title: 'Lease ID', dataIndex: 'leaseId', key: 'leaseId', render: (value?: string) => value ? <Typography.Text code>{value}</Typography.Text> : '—' },
    { title: 'Lease 过期', dataIndex: 'leaseExpireAt', key: 'leaseExpireAt', render: formatTime },
    { title: '最近心跳', dataIndex: 'lastHeartbeatAt', key: 'lastHeartbeatAt', render: formatTime },
  ]

  return (
    <div>
      <Typography.Title level={3}>实例管理</Typography.Title>
      <Card size="small" style={{ marginBottom: 16 }}>
        <ScopeSelects
          value={{ appCode: draft.appCode, env: draft.env, namespace: draft.namespace }}
          onChange={(scope) => setDraft({ ...draft, ...scope })}
        />
        <Button type="primary" onClick={applyFilter}>查询</Button>
      </Card>
      <Card size="small" title={`实例（${instances.length}）`}>
        <Table<DdcInstance>
          rowKey={(row) => row.id}
          columns={columns}
          dataSource={instances}
          loading={loading}
          size="small"
          pagination={{ pageSize: 10, size: 'small' }}
          expandable={{
            expandedRowRender: (row) => (
              <Descriptions
                size="small"
                column={3}
                items={Object.entries(row.runtimeMetadata ?? {}).map(([key, value]) => ({
                  key,
                  label: key,
                  children: value,
                }))}
              />
            ),
          }}
        />
      </Card>
    </div>
  )
}
