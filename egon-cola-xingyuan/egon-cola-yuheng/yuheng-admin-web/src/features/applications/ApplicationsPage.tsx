import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  Alert,
  Button,
  Card,
  Descriptions,
  Drawer,
  Form,
  Input,
  Modal,
  Popconfirm,
  Select,
  Space,
  Table,
  Tag,
  Typography,
  message,
} from 'antd'
import { useMemo, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { gatewayApi } from '../../api/gatewayApi'
import { GatewayApiError } from '../../api/client'
import type {
  Application,
  Credential,
  GatewayOpenApiSyncState,
  IssuedCredential,
} from '../../api/types'
import { useCapability } from '../../app/capabilities'
import { JsonPanel } from '../../components/JsonPanel'
import { LoadingBlock, QueryFailure } from '../../components/QueryState'
import { GatewayScopeFilter } from '../../components/GatewayScopeFilter'
import { useGatewayScopeBindings } from '../../hooks/useGatewayScopeBindings'
import { readScopeSearchParams, writeScopeSearchParams } from '../../hooks/scopeSearchParams'

const openApiStatusOrder = [
  'INCONSISTENT_BUILD',
  'INVALID',
  'INGEST_FAILED',
  'FETCH_FAILED',
  'STALE',
  'FETCHING',
  'VALIDATING',
  'INGESTING',
  'DISCOVERED',
  'NOT_DISCOVERED',
  'VALID',
] as const

const openApiStatusRank = (status: string): number => {
  const index = openApiStatusOrder.indexOf(status as typeof openApiStatusOrder[number])
  return index < 0 ? openApiStatusOrder.length + 1 : openApiStatusOrder.length - index
}

const observedAt = (state: GatewayOpenApiSyncState): number => Math.max(
  Date.parse(state.lastAttemptAt ?? '') || 0,
  Date.parse(state.lastSuccessAt ?? '') || 0,
  Date.parse(state.nextRetryAt ?? '') || 0,
)

const primaryBuild = (states: GatewayOpenApiSyncState[]): string | undefined => [...new Set(
  states.map((state) => state.buildId),
)].sort((left, right) => {
  const leftTime = Math.max(...states.filter((state) => state.buildId === left).map(observedAt))
  const rightTime = Math.max(...states.filter((state) => state.buildId === right).map(observedAt))
  return rightTime - leftTime || right.localeCompare(left)
})[0]

const OpenApiStatusTag = ({ status }: { status: string }) => {
  const normalized = status || 'NOT_DISCOVERED'
  const color = normalized === 'VALID'
    ? 'success'
    : ['INCONSISTENT_BUILD', 'INVALID', 'INGEST_FAILED', 'FETCH_FAILED'].includes(normalized)
      ? 'error'
      : ['STALE', 'FETCHING', 'VALIDATING', 'INGESTING'].includes(normalized)
        ? 'processing'
        : 'default'
  return <Tag color={color}>{normalized}</Tag>
}

export const ApplicationsPage = () => {
  const [searchParams, setSearchParams] = useSearchParams()
  const filters = readScopeSearchParams(searchParams, ['bizCode', 'namespace', 'env', 'appCode'])
  const bindings = useGatewayScopeBindings()
  const queryClient = useQueryClient()
  const canWrite = useCapability('gateway:applications:write')
  const canWriteCredential = useCapability('gateway:credentials:write')
  const [form] = Form.useForm()
  const [editing, setEditing] = useState<Application>()
  const [application, setApplication] = useState<Application>()
  const [detailApplicationId, setDetailApplicationId] = useState<string>()
  const [issued, setIssued] = useState<IssuedCredential>()
  const applications = useQuery({
    queryKey: ['applications', filters],
    queryFn: ({ signal }) => gatewayApi.applications(filters, signal),
  })
  const openapiSync = useQuery({
    queryKey: ['openapi-sync-states', filters],
    queryFn: ({ signal }) => gatewayApi.openapiSyncStates(filters, signal),
  })
  const syncStatesByApplication = useMemo(() => {
    const states = new Map<string, GatewayOpenApiSyncState[]>()
    for (const state of openapiSync.data ?? []) {
      const current = states.get(state.applicationId) ?? []
      current.push(state)
      states.set(state.applicationId, current)
    }
    return states
  }, [openapiSync.data])
  const credentials = useQuery({
    queryKey: ['credentials', application?.id],
    queryFn: ({ signal }) => gatewayApi.credentials(application!.id, signal),
    enabled: Boolean(application),
  })
  const applicationDetail = useQuery({
    queryKey: ['application-detail', detailApplicationId],
    queryFn: ({ signal }) => gatewayApi.application(detailApplicationId!, signal),
    enabled: Boolean(detailApplicationId),
  })
  const save = useMutation({
    mutationFn: (values: any) => editing?.id
      ? gatewayApi.updateApplication(editing.id, {
          displayName: values.displayName,
          description: values.description,
          expectedRevision: editing.revision,
        })
      : gatewayApi.createApplication({
        bizCode: bindings.data?.find((binding) => binding.bindingId === values.bindingId)?.bizCode ?? '',
        namespace: bindings.data?.find((binding) => binding.bindingId === values.bindingId)?.namespace ?? '',
        env: bindings.data?.find((binding) => binding.bindingId === values.bindingId)?.env ?? '',
        applicationCode: bindings.data?.find((binding) => binding.bindingId === values.bindingId)?.appCode ?? '',
        displayName: values.displayName,
        description: values.description,
      }),
    onSuccess: async () => {
      setEditing(undefined)
      form.resetFields()
      await queryClient.invalidateQueries({ queryKey: ['applications'] })
      void message.success('Application 已保存')
    },
    onError: async (error) => {
      if (error instanceof GatewayApiError
        && error.code === 'GATEWAY_ADMIN_APPLICATION_ALREADY_EXISTS') {
        await queryClient.invalidateQueries({
          queryKey: ['applications'],
        })
      }
    },
  })
  const createCredential = useMutation({
    mutationFn: () => gatewayApi.createCredential(application!.id),
    onSuccess: async (value) => {
      setIssued(value)
      await queryClient.invalidateQueries({ queryKey: ['credentials', application?.id] })
    },
  })
  const rotate = useMutation({
    mutationFn: ({ credential, overlapMinutes }: {
      credential: Credential
      overlapMinutes: number
    }) => gatewayApi.rotateCredential(application!.id, credential.id, overlapMinutes),
    onSuccess: async (value) => {
      setIssued(value)
      await queryClient.invalidateQueries({ queryKey: ['credentials', application?.id] })
    },
  })
  const revoke = useMutation({
    mutationFn: (credential: Credential) =>
      gatewayApi.revokeCredential(application!.id, credential.id),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['credentials', application?.id] })
      void message.success('Credential 已吊销')
    },
  })

  if (applications.isLoading) return <LoadingBlock />
  if (applications.error) return <QueryFailure error={applications.error} />
  const openApiStates = (application: Application) =>
    syncStatesByApplication.get(application.id) ?? []
  const openApiSummary = (application: Application) => {
    const states = openApiStates(application)
    if (openapiSync.isLoading) return <Tag>加载中</Tag>
    if (!states.length) return <OpenApiStatusTag status="NOT_DISCOVERED" />
    const buildId = primaryBuild(states)
    const currentBuildStates = states.filter((state) => state.buildId === buildId)
    const worst = [...currentBuildStates].sort(
      (left, right) => openApiStatusRank(right.status) - openApiStatusRank(left.status),
    )[0]
    const setIds = [...new Set(currentBuildStates
      .map((state) => state.definitionSetId)
      .filter((value): value is string => Boolean(value)))]
    const aggregateComplete = currentBuildStates.length > 0
      && currentBuildStates.every((state) => state.status === 'VALID')
      && setIds.length === 1
    return (
      <Space direction="vertical" size={0}>
        <Space size={4}>
          <OpenApiStatusTag status={worst.status} />
          <Typography.Text type="secondary">Build {buildId}</Typography.Text>
        </Space>
        <Typography.Text type="secondary">
          {aggregateComplete ? `Aggregate · ${setIds[0]}` : 'Aggregate · set 未完成'}
        </Typography.Text>
      </Space>
    )
  }
  const openApiDetail = (application: Application) => {
    const states = openApiStates(application)
    const buildId = primaryBuild(states)
    const rows = states
      .sort((left, right) => {
        const leftPrimary = left.buildId === buildId
        const rightPrimary = right.buildId === buildId
        if (leftPrimary !== rightPrimary) return leftPrimary ? -1 : 1
        return left.buildId.localeCompare(right.buildId)
          || left.openapiGroup.localeCompare(right.openapiGroup)
      })
    return (
      <Table<GatewayOpenApiSyncState>
        size="small"
        pagination={false}
        rowKey="id"
        dataSource={rows}
        columns={[
          { title: 'Group', dataIndex: 'openapiGroup' },
          { title: 'Source', dataIndex: 'sourceType' },
          {
            title: '状态',
            dataIndex: 'status',
            render: (status: string) => <OpenApiStatusTag status={status} />,
          },
          { title: 'Build', dataIndex: 'buildId' },
          {
            title: 'Canonical SHA-256',
            dataIndex: 'canonicalSha256',
            render: (value: string | null) => value
              ? <Typography.Text code copyable={{ text: value }}>{value}</Typography.Text>
              : '—',
          },
          {
            title: '错误',
            render: (_: unknown, state: GatewayOpenApiSyncState) => state.lastErrorCode
              ? <Typography.Text type="danger">{state.lastErrorCode}: {state.lastErrorMessage}</Typography.Text>
              : '—',
          },
        ]}
      />
    )
  }
  return (
    <section>
      <Space className="page-title" align="center">
        <Typography.Title level={2}>Application / Credential</Typography.Title>
        <Button
          type="primary"
          disabled={!canWrite}
          onClick={() => {
            save.reset()
            setEditing({} as Application)
            form.resetFields()
          }}
        >
          新建 Application
        </Button>
      </Space>
      <GatewayScopeFilter
        fields={['bizCode', 'namespace', 'env', 'appCode']}
        value={filters}
        onChange={(value) => setSearchParams(
          writeScopeSearchParams(searchParams, value, ['bizCode', 'namespace', 'env', 'appCode']),
        )}
      />
      <Table<Application>
        rowKey="id"
        dataSource={applications.data}
        expandable={{
          rowExpandable: (application) => openApiStates(application).length > 0,
          expandedRowRender: openApiDetail,
          expandIcon: ({ expanded, onExpand, record }) => openApiStates(record).length > 0
            ? (
              <Button
                type="link"
                size="small"
                aria-label={`${expanded ? '收起' : '展开'} OpenAPI Groups ${record.displayName}`}
                onClick={(event) => onExpand(record, event)}
              >
                {expanded ? '收起 Groups' : '查看 Groups'}
              </Button>
            )
            : null,
        }}
        columns={[
          { title: 'Biz', dataIndex: 'bizCode' },
          { title: 'Code', dataIndex: 'applicationCode' },
          { title: '名称', dataIndex: 'displayName' },
          { title: 'Env', dataIndex: 'env' },
          { title: 'Namespace', dataIndex: 'namespace' },
          { title: 'OpenAPI 状态', key: 'openapi', render: (_, row) => openApiSummary(row) },
          { title: 'Revision', dataIndex: 'revision' },
          {
            title: '操作',
            render: (_, row) => (
              <Space>
                <Button onClick={() => setDetailApplicationId(row.id)}>详情</Button>
                <Button onClick={() => setApplication(row)}>Credential</Button>
                <Button
                  disabled={!canWrite}
                  onClick={() => {
                    setEditing(row)
                    form.setFieldsValue(row)
                  }}
                >
                  编辑
                </Button>
              </Space>
            ),
          },
        ]}
      />
      {openapiSync.error && (
        <Alert
          className="section-row"
          type="warning"
          showIcon
          message="OpenAPI 同步状态暂不可用"
          description={<QueryFailure error={openapiSync.error} />}
          action={<Button onClick={() => void openapiSync.refetch()}>重试</Button>}
        />
      )}
      <Modal
        title={editing?.id ? '编辑 Application' : '新建 Application'}
        open={Boolean(editing)}
        onCancel={() => {
          save.reset()
          setEditing(undefined)
        }}
        onOk={() => form.submit()}
        confirmLoading={save.isPending}
        destroyOnHidden
      >
        {save.error && <QueryFailure error={save.error} />}
        <Form form={form} layout="vertical" onFinish={(values) => save.mutate(values)}>
          {!editing?.id && (
            <Form.Item name="bindingId" label="Scope Binding" rules={[{ required: true }]}>
              <Select
                aria-label="Scope Binding"
                placeholder="选择未连接的 Scope Binding"
                options={(bindings.data ?? []).map((binding) => ({
                  value: binding.bindingId,
                  label: [
                    binding.bizCode,
                    binding.appCode,
                    binding.env,
                    binding.namespace,
                    binding.appName,
                  ].join(' / '),
                  disabled: binding.connected,
                }))}
              />
            </Form.Item>
          )}
          <Form.Item name="displayName" label="名称" rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item name="description" label="描述"><Input.TextArea /></Form.Item>
        </Form>
      </Modal>
      <Drawer
        title={applicationDetail.data ? `Application 详情 · ${applicationDetail.data.displayName}` : 'Application 详情'}
        open={Boolean(detailApplicationId)}
        onClose={() => setDetailApplicationId(undefined)}
        size="large"
      >
        {applicationDetail.isLoading ? <LoadingBlock />
          : applicationDetail.error ? <QueryFailure error={applicationDetail.error} retry={() => void applicationDetail.refetch()} />
            : applicationDetail.data ? (
              <Descriptions bordered column={1} size="small">
                <Descriptions.Item label="Application ID">{applicationDetail.data.id}</Descriptions.Item>
                <Descriptions.Item label="业务编码">{applicationDetail.data.bizCode}</Descriptions.Item>
                <Descriptions.Item label="应用编码">{applicationDetail.data.applicationCode}</Descriptions.Item>
                <Descriptions.Item label="名称">{applicationDetail.data.displayName}</Descriptions.Item>
                <Descriptions.Item label="环境">{applicationDetail.data.env}</Descriptions.Item>
                <Descriptions.Item label="命名空间">{applicationDetail.data.namespace}</Descriptions.Item>
                <Descriptions.Item label="DDC 匹配">{applicationDetail.data.ddcMatched ? '已匹配' : '未匹配'}</Descriptions.Item>
                <Descriptions.Item label="Revision">{applicationDetail.data.revision}</Descriptions.Item>
                <Descriptions.Item label="描述">{applicationDetail.data.description ?? '—'}</Descriptions.Item>
              </Descriptions>
            ) : <Typography.Text type="secondary">暂无 Application 详情</Typography.Text>}
      </Drawer>
      <Modal
        width={900}
        title={`Credential · ${application?.displayName ?? ''}`}
        open={Boolean(application)}
        onCancel={() => {
          setApplication(undefined)
          setIssued(undefined)
        }}
        footer={null}
      >
        {issued && (
          <Alert
            type="warning"
            showIcon
            message="Secret 只显示一次，请立即保存到 Secret 管理系统"
            description={<JsonPanel title="新 Credential" value={issued} />}
          />
        )}
        <Card
          extra={(
            <Button
              type="primary"
              disabled={!canWriteCredential}
              loading={createCredential.isPending}
              onClick={() => createCredential.mutate()}
            >
              签发 Credential
            </Button>
          )}
        >
          <Table<Credential>
            rowKey="id"
            loading={credentials.isLoading}
            dataSource={credentials.data}
            columns={[
              { title: 'Access Key', dataIndex: 'accessKey' },
              { title: '状态', dataIndex: 'status' },
              { title: '生效时间', dataIndex: 'validFrom' },
              { title: '失效时间', dataIndex: 'validUntil' },
              {
                title: '操作',
                render: (_, row) => (
                  <Space>
                    <Popconfirm
                      title="轮换 Credential"
                      description="旧 Credential 将保留 10 分钟重叠窗口。"
                      onConfirm={() => rotate.mutate({
                        credential: row,
                        overlapMinutes: 10,
                      })}
                    >
                      <Button disabled={!canWriteCredential || row.status === 'REVOKED'}>轮换</Button>
                    </Popconfirm>
                    <Popconfirm
                      title="确认吊销？"
                      description="吊销后该 Access Key 将立即失效。"
                      onConfirm={() => revoke.mutate(row)}
                    >
                      <Button danger disabled={!canWriteCredential || row.status === 'REVOKED'}>
                        吊销
                      </Button>
                    </Popconfirm>
                  </Space>
                ),
              },
            ]}
          />
        </Card>
      </Modal>
    </section>
  )
}
