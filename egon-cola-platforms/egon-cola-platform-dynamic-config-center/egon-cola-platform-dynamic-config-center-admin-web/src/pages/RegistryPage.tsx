import { useCallback, useEffect, useRef, useState } from 'react'
import { Button, Card, Col, Drawer, Row, Space, Statistic, Table, Tag, Typography, message } from 'antd'
import { ddcApi } from '../api/client'
import type { RegistryInstance, RegistryService } from '../api/types'
import ScopeSelects from '../components/scope/ScopeSelects'
import { buildQuery, formatTime } from '../lib/query'
import { emptyScope } from '../lib/scopeDefaults'

type ServiceRow = RegistryService & { label: string }

type AppRow = {
  appCode: string
  bizCode: string
  services: ServiceRow[]
}

const serviceIdentity = (service: RegistryService): string => service.serviceId

const serviceLabel = (service: RegistryService): string =>
  `${service.serviceKind} / ${service.protocol}`

export default function RegistryPage() {
  const [draft, setDraft] = useState(() => ({ ...emptyScope }))
  const [rows, setRows] = useState<AppRow[]>([])
  const [instanceGroups, setInstanceGroups] = useState<{ service: ServiceRow; instances: RegistryInstance[] }[]>([])
  const [loading, setLoading] = useState(false)
  const [drawerApp, setDrawerApp] = useState<AppRow | null>(null)
  const [drawerLoading, setDrawerLoading] = useState(false)
  const filterRef = useRef({ ...emptyScope })

  const loadRegistry = useCallback(async () => {
    const scope = filterRef.current
    const query = buildQuery(scope)
    const data = await ddcApi<{ services: RegistryService[] }>(
      `/api/v1/ddc/registry/services${query === '' ? '' : `?${query}`}`,
    )
    const unique = new Map<string, ServiceRow>()
    ;(data?.services ?? []).forEach((service) => unique.set(
      serviceIdentity(service),
      { ...service, label: serviceLabel(service) },
    ))
    const byApp = new Map<string, AppRow>()
    unique.forEach((service) => {
      const key = `${service.bizCode}|${service.appCode}`
      const existing = byApp.get(key)
      if (existing) {
        existing.services.push(service)
      } else {
        byApp.set(key, { appCode: service.appCode, bizCode: service.bizCode, services: [service] })
      }
    })
    setRows([...byApp.values()].sort((left, right) => left.appCode.localeCompare(right.appCode)))
  }, [])

  useEffect(() => {
    void Promise.resolve().then(loadRegistry).catch((error) => {
      message.error(error instanceof Error ? error.message : String(error))
    })
  }, [loadRegistry])

  const refresh = async () => {
    setLoading(true)
    try {
      await loadRegistry()
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

  const openDrawer = async (app: AppRow) => {
    setDrawerApp(app)
    setDrawerLoading(true)
    setInstanceGroups([])
    try {
      const snapshots = await Promise.all(app.services.map(async (service) => {
        const data = await ddcApi<{ instances: RegistryInstance[] }>(
          `/api/v1/ddc/registry/instances?${buildQuery({
            bizCode: service.bizCode,
            env: service.env,
            appCode: service.appCode,
            serviceKind: service.serviceKind,
            protocol: service.protocol,
            serviceName: service.serviceName,
            group: service.group,
            version: service.version,
          })}`,
        )
        return { service, instances: data?.instances ?? [] }
      }))
      setInstanceGroups(snapshots)
    } catch (error) {
      message.error(error instanceof Error ? error.message : String(error))
    } finally {
      setDrawerLoading(false)
    }
  }

  const serviceCount = rows.reduce((sum, row) => sum + row.services.length, 0)
  const onlineCount = instanceGroups.reduce(
    (sum, group) => sum + group.instances.filter((item) => item.status === 'ONLINE').length,
    0,
  )

  const columns = [
    { title: '业务域', dataIndex: 'bizCode', key: 'bizCode', render: (value: string) => <Typography.Text code>{value}</Typography.Text> },
    { title: '应用', dataIndex: 'appCode', key: 'appCode', render: (value: string) => <Typography.Text code>{value}</Typography.Text> },
    {
      title: '服务数',
      key: 'serviceCount',
      render: (_: unknown, row: AppRow) => row.services.length,
    },
  ]

  return (
    <div>
      <Typography.Title level={3}>服务注册目录</Typography.Title>
      <Row gutter={16} style={{ marginBottom: 16 }}>
        <Col><ScopeSelects value={draft} onChange={setDraft} /></Col>
        <Col><Button type="primary" onClick={applyFilter}>刷新</Button></Col>
      </Row>
      <Row gutter={16} style={{ marginBottom: 16 }}>
        <Col span={8}><Card size="small"><Statistic title="应用数" value={rows.length} /></Card></Col>
        <Col span={8}><Card size="small"><Statistic title="服务数" value={serviceCount} /></Card></Col>
        <Col span={8}><Card size="small"><Statistic title="在线实例（当前抽屉）" value={onlineCount} /></Card></Col>
      </Row>
      <Card size="small" title={`应用（${rows.length}）`}>
        <Table<AppRow>
          rowKey={(row) => `${row.bizCode}|${row.appCode}`}
          columns={columns}
          dataSource={rows}
          loading={loading}
          size="small"
          pagination={{ pageSize: 10, size: 'small' }}
          onRow={(row) => ({ onClick: () => void openDrawer(row), style: { cursor: 'pointer' } })}
        />
      </Card>
      <Drawer
        open={drawerApp !== null}
        title={`${drawerApp?.appCode ?? ''} 实例`}
        onClose={() => setDrawerApp(null)}
        width={860}
      >
        {drawerApp && (
          <Space direction="vertical" style={{ width: '100%' }} size="large">
            {instanceGroups.map(({ service, instances: groupInstances }) => (
              <Card
                key={serviceIdentity(service)}
                size="small"
                title={`${service.label} / ${service.serviceName}（${service.group ?? '—'} / ${service.version ?? '—'}）`}
                extra={(
                  <Typography.Text
                    code
                    copyable={{ text: service.serviceId }}
                    title={service.serviceId}
                  >
                    {service.serviceId.slice(0, 12)}
                  </Typography.Text>
                )}
              >
                <Table<RegistryInstance>
                  rowKey={(row) => row.instanceId}
                  size="small"
                  loading={drawerLoading}
                  pagination={{ pageSize: 10, size: 'small' }}
                  dataSource={groupInstances}
                  columns={[
                    {
                      title: '状态',
                      dataIndex: 'status',
                      key: 'status',
                      render: (status: string) => (
                        <Tag color={status === 'ONLINE' ? 'green' : 'default'}>{status ?? 'UNKNOWN'}</Tag>
                      ),
                    },
                    { title: '实例 ID', dataIndex: 'instanceId', key: 'instanceId', render: (value: string) => <Typography.Text code>{value}</Typography.Text> },
                    {
                      title: '地址',
                      key: 'address',
                      render: (_: unknown, row: RegistryInstance) => (
                        <Typography.Text code>{`${row.secure ? 'tls://' : ''}${row.host}:${row.port}`}</Typography.Text>
                      ),
                    },
                    { title: '最近心跳', dataIndex: 'lastHeartbeatAt', key: 'lastHeartbeatAt', render: formatTime },
                    { title: '过期时间', dataIndex: 'expireAt', key: 'expireAt', render: formatTime },
                  ]}
                />
              </Card>
            ))}
          </Space>
        )}
      </Drawer>
    </div>
  )
}
