import { useCallback, useEffect, useRef, useState } from 'react'
import { Button, Card, Col, Drawer, Row, Space, Statistic, Table, Tag, Typography, message } from 'antd'
import { ddcApi } from '../api/client'
import type { RegistryInstance, RegistryService } from '../api/types'
import AppSelect from '../components/scope/AppSelect'
import BizSelect from '../components/scope/BizSelect'
import EnvSelect from '../components/scope/EnvSelect'
import NamespaceSelect from '../components/scope/NamespaceSelect'
import { buildQuery, formatTime } from '../lib/query'
import { configuredInitialScope } from '../lib/scopeDefaults'

const serviceQueries = [
  { serviceKind: 'HTTP_PROVIDER', protocol: 'http', label: 'HTTP Provider' },
  { serviceKind: 'HTTP_PROVIDER', protocol: 'https', label: 'HTTPS Provider' },
  { serviceKind: 'RPC_PROVIDER', protocol: 'grpc', label: 'RPC Provider' },
  { serviceKind: 'INTERNAL_GATEWAY', protocol: 'grpc', label: 'Internal Gateway' },
]

type ServiceRow = RegistryService & { label: string }

type AppRow = {
  appCode: string
  bizCode: string
  services: ServiceRow[]
}

const serviceIdentity = (service: RegistryService): string =>
  [service.bizCode, service.appCode, service.serviceKind, service.protocol, service.serviceName, service.group ?? '', service.version ?? ''].join('|')

export default function RegistryPage() {
  const [draft, setDraft] = useState(() => ({ ...configuredInitialScope }))
  const [rows, setRows] = useState<AppRow[]>([])
  const [instanceGroups, setInstanceGroups] = useState<{ service: ServiceRow; instances: RegistryInstance[] }[]>([])
  const [loading, setLoading] = useState(false)
  const [drawerApp, setDrawerApp] = useState<AppRow | null>(null)
  const [drawerLoading, setDrawerLoading] = useState(false)
  const filterRef = useRef({ ...configuredInitialScope })

  const loadRegistry = useCallback(async () => {
    const scope = filterRef.current
    const snapshots = await Promise.all(serviceQueries.map(async (item) => {
      const data = await ddcApi<{ services: RegistryService[] }>(
        `/api/v1/ddc/registry/services?${buildQuery({
          ...scope,
          serviceKind: item.serviceKind,
          protocol: item.protocol,
        })}`,
      )
      return (data?.services ?? []).map((service) => ({ ...service, label: item.label }))
    }))
    const unique = new Map<string, ServiceRow>()
    snapshots.flat().forEach((service) => unique.set(serviceIdentity(service), service))
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
    loadRegistry().catch((error) => {
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
      const scope = filterRef.current
      const snapshots = await Promise.all(app.services.map(async (service) => {
        const data = await ddcApi<{ instances: RegistryInstance[] }>(
          `/api/v1/ddc/registry/instances?${buildQuery({
            ...scope,
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
        <Col><span style={{ width: 200, display: 'inline-block' }}><BizSelect value={draft.bizCode} onChange={(bizCode) => setDraft({ ...draft, bizCode, appCode: '', namespace: '' })} /></span></Col>
        <Col><span style={{ width: 200, display: 'inline-block' }}><AppSelect value={draft.appCode} biz={draft.bizCode} onChange={(appCode) => setDraft({ ...draft, appCode, namespace: '' })} /></span></Col>
        <Col><span style={{ width: 200, display: 'inline-block' }}><NamespaceSelect value={draft.namespace} appCode={draft.appCode} onChange={(namespace) => setDraft({ ...draft, namespace })} /></span></Col>
        <Col><span style={{ width: 140, display: 'inline-block' }}><EnvSelect value={draft.env} onChange={(env) => setDraft({ ...draft, env })} /></span></Col>
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
