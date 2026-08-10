import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { PageState } from '@egon-cola/admin-web-shared'
import {
  Button,
  Card,
  Col,
  Drawer,
  Grid,
  Row,
  Space,
  Statistic,
  Table,
  Tag,
  Typography,
} from 'antd'
import type { TableColumnsType } from 'antd'
import { useState } from 'react'
import { ddcPageApi } from '../api/client'
import type { RegistryInstance, RegistryService } from '../api/types'
import AdminPageHeader from '../components/page/AdminPageHeader'
import ScopeSelects, { type ScopeValue } from '../components/scope/ScopeSelects'
import { usePageState } from '../hooks/usePageState'
import { buildQuery, formatTime } from '../lib/query'
import { emptyScope } from '../lib/scopeDefaults'

export default function RegistryPage() {
  const screens = Grid.useBreakpoint()
  const servicePage = usePageState()
  const instancePage = usePageState()
  const [draft, setDraft] = useState<ScopeValue>(() => ({ ...emptyScope }))
  const [submitted, setSubmitted] = useState<ScopeValue>(() => ({
    ...emptyScope,
  }))
  const [selectedService, setSelectedService] =
    useState<RegistryService | null>(null)

  const servicesQuery = useQuery({
    queryKey: ['ddc', 'registry-services', submitted, servicePage.page],
    queryFn: ({ signal }) => ddcPageApi<RegistryService>(
      `/api/v1/ddc/registry/services/page?${buildQuery({
        ...submitted,
        pageNo: servicePage.page.pageNo,
        pageSize: servicePage.page.pageSize,
      })}`,
      { signal },
    ),
    placeholderData: keepPreviousData,
    staleTime: 0,
  })

  const instancesQuery = useQuery({
    enabled: selectedService !== null,
    queryKey: [
      'ddc',
      'registry-instances',
      selectedService?.serviceId,
      instancePage.page,
    ],
    queryFn: ({ signal }) => ddcPageApi<RegistryInstance>(
      `/api/v1/ddc/registry/instances/page?${buildQuery({
        bizCode: selectedService!.bizCode,
        env: selectedService!.env,
        appCode: selectedService!.appCode,
        serviceKind: selectedService!.serviceKind,
        protocol: selectedService!.protocol,
        serviceName: selectedService!.serviceName,
        group: selectedService!.group,
        version: selectedService!.version,
        pageNo: instancePage.page.pageNo,
        pageSize: instancePage.page.pageSize,
      })}`,
      { signal },
    ),
    placeholderData: keepPreviousData,
    staleTime: 0,
  })

  const applyFilter = () => {
    setSubmitted({ ...draft })
    servicePage.resetPage()
  }

  const resetFilter = () => {
    setDraft({ ...emptyScope })
    setSubmitted({ ...emptyScope })
    servicePage.resetPage()
  }

  const openInstances = (service: RegistryService) => {
    instancePage.resetPage()
    setSelectedService(service)
  }

  const closeInstances = () => {
    setSelectedService(null)
    instancePage.onTableChange(1, 10)
  }

  const serviceColumns: TableColumnsType<RegistryService> = [
    {
      title: '业务域 / 环境 / 应用',
      key: 'scope',
      render: (_: unknown, row) => (
        <Typography.Text code>
          {row.bizCode} / {row.env} / {row.appCode}
        </Typography.Text>
      ),
    },
    {
      title: '类型 / 协议',
      key: 'transport',
      render: (_: unknown, row) => (
        <Space>
          <Tag>{row.serviceKind}</Tag>
          <Tag color="blue">{row.protocol}</Tag>
        </Space>
      ),
    },
    { title: '服务名', dataIndex: 'serviceName', key: 'serviceName' },
    {
      title: '分组 / 版本',
      key: 'version',
      render: (_: unknown, row) =>
        `${row.group ?? '—'} / ${row.version ?? '—'}`,
    },
    {
      title: 'Service ID',
      dataIndex: 'serviceId',
      key: 'serviceId',
      render: (value: string) => (
        <Typography.Text code copyable={{ text: value }} title={value}>
          {value}
        </Typography.Text>
      ),
    },
    {
      title: '操作',
      key: 'actions',
      fixed: 'right',
      render: (_: unknown, row) => (
        <Button size="small" type="primary" onClick={() => openInstances(row)}>
          查看实例
        </Button>
      ),
    },
  ]

  const instanceColumns: TableColumnsType<RegistryInstance> = [
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      render: (status: string) => (
        <Tag color={status === 'ONLINE' ? 'green' : 'default'}>
          {status ?? 'UNKNOWN'}
        </Tag>
      ),
    },
    {
      title: '实例 ID',
      dataIndex: 'instanceId',
      key: 'instanceId',
      render: (value: string) => <Typography.Text code>{value}</Typography.Text>,
    },
    {
      title: '地址',
      key: 'address',
      render: (_: unknown, row) => (
        <Typography.Text code>
          {`${row.secure ? 'tls://' : ''}${row.host}:${row.port}`}
        </Typography.Text>
      ),
    },
    {
      title: '最近心跳',
      dataIndex: 'lastHeartbeatAt',
      key: 'lastHeartbeatAt',
      render: formatTime,
    },
    {
      title: '过期时间',
      dataIndex: 'expireAt',
      key: 'expireAt',
      render: formatTime,
    },
  ]

  const onlineOnPage = (instancesQuery.data?.records ?? [])
    .filter((instance) => instance.status === 'ONLINE').length

  return (
    <div>
      <AdminPageHeader
        title="服务注册目录"
        description="分页查看服务键，并按需加载单个服务的实例列表。"
      />
      <Card size="small" style={{ marginBottom: 16 }}>
        <Space wrap>
          <ScopeSelects value={draft} onChange={setDraft} />
          <Button type="primary" onClick={applyFilter}>查询</Button>
          <Button onClick={resetFilter}>重置</Button>
        </Space>
      </Card>
      <Row gutter={[16, 16]} style={{ marginBottom: 16 }}>
        <Col xs={24} sm={12}>
          <Card size="small">
            <Statistic
              title="服务总数"
              value={servicesQuery.data?.page.total ?? 0}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12}>
          <Card size="small">
            <Statistic title="当前实例页在线" value={onlineOnPage} />
          </Card>
        </Col>
      </Row>
      <Card className="ddc-admin-table-card" size="small" title="服务目录">
        <PageState
          loading={servicesQuery.isPending}
          error={servicesQuery.error}
          empty={(servicesQuery.data?.records.length ?? 0) === 0}
          onRetry={() => { void servicesQuery.refetch() }}
        >
          <Table<RegistryService>
            rowKey={(row) => row.serviceId}
            columns={serviceColumns}
            dataSource={servicesQuery.data?.records ?? []}
            loading={servicesQuery.isFetching}
            size="small"
            scroll={{ x: 'max-content' }}
            pagination={{
              current: servicesQuery.data?.page.pageNo
                ?? servicePage.page.pageNo,
              pageSize: servicesQuery.data?.page.pageSize
                ?? servicePage.page.pageSize,
              total: servicesQuery.data?.page.total ?? 0,
              showSizeChanger: true,
              pageSizeOptions: [10, 20, 50],
              showTotal: (total) => `共 ${total} 条`,
              onChange: servicePage.onTableChange,
            }}
          />
        </PageState>
      </Card>
      <Drawer
        open={selectedService !== null}
        title={`${selectedService?.serviceName ?? ''} 实例`}
        onClose={closeInstances}
        size={screens.md ? 860 : '100%'}
      >
        <PageState
          loading={instancesQuery.isPending}
          error={instancesQuery.error}
          empty={(instancesQuery.data?.records.length ?? 0) === 0}
          onRetry={() => { void instancesQuery.refetch() }}
        >
          <Table<RegistryInstance>
            rowKey={(row) => row.instanceId}
            size="small"
            loading={instancesQuery.isFetching}
            dataSource={instancesQuery.data?.records ?? []}
            columns={instanceColumns}
            scroll={{ x: 'max-content' }}
            pagination={{
              current: instancesQuery.data?.page.pageNo
                ?? instancePage.page.pageNo,
              pageSize: instancesQuery.data?.page.pageSize
                ?? instancePage.page.pageSize,
              total: instancesQuery.data?.page.total ?? 0,
              showSizeChanger: true,
              pageSizeOptions: [10, 20, 50],
              showTotal: (total) => `共 ${total} 条`,
              onChange: instancePage.onTableChange,
            }}
          />
        </PageState>
      </Drawer>
    </div>
  )
}
