import { useCallback, useEffect, useRef, useState } from 'react'
import { Button, Card, Col, Row, Statistic, Table, Tag, Typography, message } from 'antd'
import { ddcApi } from '../api/client'
import type { RegistryInstance, RegistryService } from '../api/types'
import EnvSelect from '../components/scope/EnvSelect'
import NamespaceSelect from '../components/scope/NamespaceSelect'
import { buildQuery, formatTime } from '../lib/query'

const serviceQueries = [
  { serviceKind: 'HTTP_PROVIDER', protocol: 'http', label: 'HTTP Provider' },
  { serviceKind: 'HTTP_PROVIDER', protocol: 'https', label: 'HTTPS Provider' },
  { serviceKind: 'RPC_PROVIDER', protocol: 'grpc', label: 'RPC Provider' },
  { serviceKind: 'INTERNAL_GATEWAY', protocol: 'grpc', label: 'Internal Gateway' },
]

type ServiceRow = RegistryService & { label: string }

const serviceIdentity = (service: RegistryService): string =>
  [service.serviceKind, service.protocol, service.serviceName, service.group ?? '', service.version ?? ''].join('|')

export default function RegistryPage() {
  const [draft, setDraft] = useState({ env: '', namespace: '' })
  const [services, setServices] = useState<ServiceRow[]>([])
  const [instances, setInstances] = useState<RegistryInstance[]>([])
  const [selected, setSelected] = useState<ServiceRow | null>(null)
  const [loading, setLoading] = useState(false)
  const filterRef = useRef({ env: '', namespace: '' })
  const selectedRef = useRef<ServiceRow | null>(null)

  const loadInstances = useCallback(async (service: ServiceRow) => {
    const scope = filterRef.current
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
    setInstances(data?.instances ?? [])
  }, [])

  const loadRegistry = useCallback(async () => {
    const scope = filterRef.current
    const snapshots = await Promise.all(serviceQueries.map(async (item) => {
      const data = await ddcApi<{ services: RegistryService[] }>(
        `/api/v1/ddc/registry/services?${buildQuery({ ...scope, serviceKind: item.serviceKind, protocol: item.protocol })}`,
      )
      return (data?.services ?? []).map((service) => ({ ...service, label: item.label }))
    }))
    const unique = new Map<string, ServiceRow>()
    snapshots.flat().forEach((service) => unique.set(serviceIdentity(service), service))
    const next = [...unique.values()].sort((left, right) =>
      `${left.serviceKind}:${left.serviceName}`.localeCompare(`${right.serviceKind}:${right.serviceName}`))
    setServices(next)
    const current = selectedRef.current
    if (current) {
      const found = next.find((item) => serviceIdentity(item) === serviceIdentity(current))
      if (found) {
        selectedRef.current = found
        setSelected(found)
        await loadInstances(found)
      } else {
        setInstances([])
      }
    }
  }, [loadInstances])

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
    filterRef.current = { env: draft.env, namespace: draft.namespace }
    void refresh()
  }

  const selectService = (service: ServiceRow) => {
    selectedRef.current = service
    setSelected(service)
    loadInstances(service).catch((error) => {
      message.error(error instanceof Error ? error.message : String(error))
    })
  }

  const httpCount = services.filter((item) => item.serviceKind === 'HTTP_PROVIDER').length
  const rpcCount = services.filter((item) => item.serviceKind === 'RPC_PROVIDER').length
  const gatewayCount = services.filter((item) => item.serviceKind === 'INTERNAL_GATEWAY').length
  const onlineCount = instances.filter((item) => item.status === 'ONLINE').length

  const serviceColumns = [
    { title: '类型', dataIndex: 'label', key: 'label' },
    { title: '服务名', dataIndex: 'serviceName', key: 'serviceName' },
    { title: '协议', dataIndex: 'protocol', key: 'protocol' },
    {
      title: '分组 / 版本',
      key: 'group',
      render: (_: unknown, row: ServiceRow) => `${row.group || '—'} / ${row.version || '—'}`,
    },
  ]

  const instanceColumns = [
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      render: (status: string) => (
        <Tag color={status === 'ONLINE' ? 'green' : 'default'}>{status ?? 'UNKNOWN'}</Tag>
      ),
    },
    {
      title: '实例',
      dataIndex: 'instanceId',
      key: 'instanceId',
      render: (instanceId: string, row: RegistryInstance) => (
        <span>
          <Typography.Text code>{instanceId}</Typography.Text>
          {row.metadata?.buildId && (
            <Typography.Text type="secondary" style={{ display: 'block', fontSize: 12 }}>
              {row.metadata.buildId}
            </Typography.Text>
          )}
        </span>
      ),
    },
    {
      title: '地址',
      key: 'address',
      render: (_: unknown, row: RegistryInstance) => (
        <Typography.Text code>{`${row.secure ? 'tls://' : ''}${row.host}:${row.port}`}</Typography.Text>
      ),
    },
    { title: '最近心跳', dataIndex: 'lastHeartbeatAt', key: 'lastHeartbeatAt', render: formatTime },
    { title: '过期时间', dataIndex: 'expireAt', key: 'expireAt', render: formatTime },
  ]

  return (
    <div>
      <Typography.Title level={3}>服务注册目录</Typography.Title>
      <Row gutter={16} style={{ marginBottom: 16 }}>
        <Col>
          <span style={{ width: 140, display: 'inline-block' }}>
            <EnvSelect
              value={draft.env}
              onChange={(env) => setDraft({ ...draft, env })}
            />
          </span>
        </Col>
        <Col>
          <span style={{ width: 200, display: 'inline-block' }}>
            <NamespaceSelect
              value={draft.namespace}
              onChange={(namespace) => setDraft({ ...draft, namespace })}
            />
          </span>
        </Col>
        <Col>
          <Button type="primary" onClick={applyFilter}>刷新</Button>
        </Col>
      </Row>
      <Row gutter={16} style={{ marginBottom: 16 }}>
        <Col span={6}><Card size="small"><Statistic title="HTTP Provider" value={httpCount} /></Card></Col>
        <Col span={6}><Card size="small"><Statistic title="RPC Provider" value={rpcCount} /></Card></Col>
        <Col span={6}><Card size="small"><Statistic title="Internal Gateway" value={gatewayCount} /></Card></Col>
        <Col span={6}><Card size="small"><Statistic title="在线实例" value={onlineCount} /></Card></Col>
      </Row>
      <Card size="small" title={`服务（${services.length}）`} style={{ marginBottom: 16 }}>
        <Table<ServiceRow>
          rowKey={serviceIdentity}
          columns={serviceColumns}
          dataSource={services}
          loading={loading}
          size="small"
          pagination={{ pageSize: 10, size: 'small' }}
          rowClassName={(row) => (selected && serviceIdentity(selected) === serviceIdentity(row) ? 'ant-table-row-selected' : '')}
          onRow={(row) => ({ onClick: () => selectService(row), style: { cursor: 'pointer' } })}
        />
      </Card>
      <Card size="small" title={`实例（${instances.length}）`}>
        <Table<RegistryInstance>
          rowKey={(row) => row.instanceId}
          columns={instanceColumns}
          dataSource={instances}
          size="small"
          pagination={{ pageSize: 10, size: 'small' }}
          locale={{ emptyText: selected ? '暂无实例' : '选择左侧服务查看实例' }}
        />
      </Card>
    </div>
  )
}
