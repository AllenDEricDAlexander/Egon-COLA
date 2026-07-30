import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  Alert,
  Button,
  Card,
  Form,
  Input,
  InputNumber,
  Modal,
  Popconfirm,
  Select,
  Space,
  Switch,
  Table,
  Tabs,
  Typography,
  message,
} from 'antd'
import { useRef, useState } from 'react'
import { useLocation, useParams } from 'react-router-dom'
import { GatewayApiError } from '../../api/client'
import { gatewayApi } from '../../api/gatewayApi'
import type {
  DraftPolicy,
  DraftRoute,
  GatewayRouteTransportPolicy,
} from '../../api/types'
import { JsonPanel } from '../../components/JsonPanel'
import { LoadingBlock, QueryFailure } from '../../components/QueryState'
import { useCapability } from '../../app/capabilities'
import {
  policyWarnings,
  transportFormState,
  validatePublicRoute,
} from './routeValidation'
import {
  readRouteForm,
  transportFieldPresentation,
  validateTransportRoute,
  writeCanonicalRoute,
  type RouteFormValues,
  type TransportPolicyField,
} from './routeTransport'

const json = (value: string): Record<string, unknown> => {
  const parsed = JSON.parse(value) as unknown
  if (!parsed || Array.isArray(parsed) || typeof parsed !== 'object') {
    throw new Error('内容必须是 JSON Object')
  }
  return parsed as Record<string, unknown>
}

const policyContent = (values: any): Record<string, unknown> => ({
  ...json(values.advancedContent || '{}'),
  timeoutMs: values.timeoutMs ? Number(values.timeoutMs) : undefined,
  permitsPerSecond: values.permitsPerSecond
    ? Number(values.permitsPerSecond)
    : undefined,
  maxConcurrent: values.maxConcurrent
    ? Number(values.maxConcurrent)
    : undefined,
  maxAttempts: values.maxAttempts ? Number(values.maxAttempts) : undefined,
  strategy: values.strategy,
  failureMode: values.failureMode,
  allowedOrigins: values.allowedOrigins
    ?.split(',')
    .map((value: string) => value.trim())
    .filter(Boolean),
})

type RouteEditorValues = RouteFormValues & {
  routeId: string
  operationId: string
  enabled: boolean
  changeReason: string
}

type RouteOperationState = {
  operationId: string
  protocol?: string
  externalAccessible?: boolean
  loading: boolean
  error?: string
}

const TRANSPORT_POLICY_FIELDS = [
  'profile',
  'transportProtocol',
  'requestBodyMode',
  'responseMode',
  'maxRequestBodyBytes',
  'connectTimeoutMs',
  'responseHeaderTimeoutMs',
  'streamIdleTimeoutMs',
  'totalTimeoutMs',
  'websocketIdleTimeoutMs',
  'websocketMaxFrameBytes',
  'bodyLogEnabled',
  'retryEnabled',
] as const

const mergeTransportPolicy = (
  original: GatewayRouteTransportPolicy | undefined,
  edited: GatewayRouteTransportPolicy | undefined,
): GatewayRouteTransportPolicy | undefined => {
  const merged: GatewayRouteTransportPolicy = { ...original }
  const writable = merged as Record<string, unknown>
  TRANSPORT_POLICY_FIELDS.forEach((field) => {
    writable[field] = edited?.[field]
  })
  return Object.keys(merged).length ? merged : undefined
}

const displayTransportValue = (value: unknown): string => {
  if (value === undefined) return '继承既有策略 / 不适用'
  if (value === true) return '开启'
  if (value === false) return '关闭'
  return String(value)
}

const transportFieldExtra = (
  policy: GatewayRouteTransportPolicy | undefined,
  field: TransportPolicyField,
): string => {
  const presentation = transportFieldPresentation(policy, field)
  return `${presentation.source === 'ROUTE_OVERRIDE' ? 'Route Override' : 'Profile 默认'}：${displayTransportValue(presentation.value)}`
}

export const DraftPage = () => {
  const { groupId = '' } = useParams()
  const location = useLocation()
  const queryClient = useQueryClient()
  const canWrite = useCapability('gateway:drafts:write')
  const [routeForm] = Form.useForm<RouteEditorValues>()
  const [policyForm] = Form.useForm()
  const [routeOpen, setRouteOpen] = useState(false)
  const [policyOpen, setPolicyOpen] = useState(false)
  const [routeOperation, setRouteOperation] = useState<RouteOperationState>()
  const [validatingRoute, setValidatingRoute] = useState(false)
  const [legacyHostMissing, setLegacyHostMissing] = useState(false)
  const [localConflict, setLocalConflict] = useState<GatewayApiError>()
  const operationRequest = useRef(0)
  const originalTransportPolicy = useRef<GatewayRouteTransportPolicy | undefined>(undefined)
  const draft = useQuery({
    queryKey: ['draft', groupId],
    queryFn: ({ signal }) => gatewayApi.draft(groupId, signal),
    enabled: Boolean(groupId),
  })
  const diff = useQuery({
    queryKey: ['draft-diff', groupId],
    queryFn: ({ signal }) => gatewayApi.draftDiff(groupId, signal),
    enabled: Boolean(groupId),
  })
  const saveRoute = useMutation({
    mutationFn: (values: RouteEditorValues) => gatewayApi.saveRoute(
        groupId,
        values.routeId,
        {
          operationId: values.operationId,
          content: writeCanonicalRoute(values),
          enabled: values.enabled,
          changeReason: values.changeReason,
        },
        draft.data!.revision,
      ),
    onSuccess: async () => {
      closeRoute()
      setLocalConflict(undefined)
      await queryClient.invalidateQueries({ queryKey: ['draft', groupId] })
      await queryClient.invalidateQueries({ queryKey: ['draft-diff', groupId] })
      void message.success('Route 已保存')
    },
    onError: (error) => {
      if (error instanceof GatewayApiError && error.status === 409) {
        setLocalConflict(error)
        return
      }
      void message.error('Route 保存失败，请检查表单和服务端校验结果')
    },
  })
  const savePolicy = useMutation({
    mutationFn: (values: any) =>
      gatewayApi.savePolicy(
        groupId,
        values.policyId,
        {
          policyType: values.policyType,
          policyScope: values.policyScope,
          content: policyContent(values),
          enabled: values.enabled,
          changeReason: values.changeReason,
        },
        draft.data!.revision,
      ),
    onSuccess: async () => {
      setPolicyOpen(false)
      setLocalConflict(undefined)
      await queryClient.invalidateQueries({ queryKey: ['draft', groupId] })
      await queryClient.invalidateQueries({ queryKey: ['draft-diff', groupId] })
      void message.success('Policy 已保存')
    },
    onError: (error) => {
      if (error instanceof GatewayApiError && error.status === 409) {
        setLocalConflict(error)
      }
    },
  })
  const deleteRoute = useMutation({
    mutationFn: (route: DraftRoute) =>
      gatewayApi.deleteRoute(
        groupId,
        route.routeId,
        draft.data!.revision,
        'Delete route from Gateway Admin Web',
      ),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['draft', groupId] })
      await queryClient.invalidateQueries({ queryKey: ['draft-diff', groupId] })
      void message.success('Route 已删除')
    },
  })
  const deletePolicy = useMutation({
    mutationFn: (policy: DraftPolicy) =>
      gatewayApi.deletePolicy(
        groupId,
        policy.policyId,
        draft.data!.revision,
        'Delete policy from Gateway Admin Web',
      ),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['draft', groupId] })
      await queryClient.invalidateQueries({ queryKey: ['draft-diff', groupId] })
      void message.success('Policy 已删除')
    },
  })

  const loadRouteOperation = async (rawOperationId: string) => {
    const operationId = rawOperationId.trim()
    const request = ++operationRequest.current
    if (!operationId) {
      setRouteOperation(undefined)
      return
    }
    setRouteOperation({ operationId, loading: true })
    try {
      const detail = await gatewayApi.operation(operationId)
      if (request !== operationRequest.current) return
      setRouteOperation({
        operationId,
        protocol: detail.operation.protocol.toUpperCase(),
        externalAccessible: detail.operation.externalAccessible,
        loading: false,
      })
    } catch {
      if (request !== operationRequest.current) return
      setRouteOperation({
        operationId,
        loading: false,
        error: '无法从服务端读取 Operation 协议，请确认 Operation ID。',
      })
    }
  }

  const openNewRoute = () => {
    operationRequest.current += 1
    originalTransportPolicy.current = undefined
    setLegacyHostMissing(false)
    setRouteOperation(undefined)
    routeForm.resetFields()
    routeForm.setFieldsValue({
      accessZones: ['INTERNAL'],
      httpMethod: 'GET',
      priority: 0,
      advancedContent: '{}',
      enabled: true,
    })
    setRouteOpen(true)
  }

  const openExistingRoute = (route: DraftRoute) => {
    const values = readRouteForm(route.routeContent)
    originalTransportPolicy.current = values.transportPolicy
    setLegacyHostMissing(Boolean(values.legacyHostMissing))
    routeForm.resetFields()
    routeForm.setFieldsValue({
      ...values,
      routeId: route.routeId,
      operationId: route.operationId,
      enabled: route.enabled,
      changeReason: 'Edit route from Gateway Admin Web',
    })
    setRouteOpen(true)
    void loadRouteOperation(route.operationId)
  }

  const closeRoute = () => {
    operationRequest.current += 1
    setRouteOpen(false)
    setRouteOperation(undefined)
  }

  const submitRoute = async (values: RouteEditorValues) => {
    const operationId = values.operationId.trim()
    const request = ++operationRequest.current
    setValidatingRoute(true)
    setLocalConflict(undefined)
    try {
      const detail = await gatewayApi.operation(operationId)
      if (
        request !== operationRequest.current
        || routeForm.getFieldValue('operationId')?.trim() !== operationId
      ) {
        return
      }
      const operationProtocol = detail.operation.protocol.toUpperCase()
      setRouteOperation({
        operationId,
        protocol: operationProtocol,
        externalAccessible: detail.operation.externalAccessible,
        loading: false,
      })
      const candidate: RouteEditorValues = {
        ...values,
        operationId,
        operationProtocol,
        legacyHostMissing,
        transportPolicy: operationProtocol === 'HTTP'
          ? mergeTransportPolicy(originalTransportPolicy.current, values.transportPolicy)
          : originalTransportPolicy.current,
      }
      const issues = validateTransportRoute(candidate)
      const accessIssue = validatePublicRoute(
        candidate.accessZones,
        detail.operation.externalAccessible,
      )
      if (accessIssue) {
        issues.push({
          path: 'accessZones',
          code: 'EXTERNAL_ACCESS_DENIED',
          message: accessIssue,
        })
      }
      if (issues.length) {
        routeForm.setFields(issues.map((issue) => ({
          name: issue.path.split('.'),
          errors: [issue.message],
        })))
        return
      }
      saveRoute.mutate(candidate)
    } catch {
      setRouteOperation({
        operationId,
        loading: false,
        error: '无法从服务端读取 Operation 协议，请确认 Operation ID。',
      })
      void message.error('无法读取 Operation，Route 未保存')
    } finally {
      setValidatingRoute(false)
    }
  }

  const active = location.pathname.endsWith('/policies') ? 'policies' : 'routes'
  const watchedRoute = Form.useWatch([], routeForm) as Partial<RouteEditorValues> | undefined
  const currentOperationId = watchedRoute?.operationId?.trim()
  const currentOperation = routeOperation?.operationId === currentOperationId
    ? routeOperation
    : undefined
  const currentTransportPolicy = watchedRoute?.transportPolicy
  const currentTransportState = transportFormState(
    currentOperation?.protocol,
    currentTransportPolicy,
  )
  const watchedPolicy = Form.useWatch([], policyForm) ?? {}
  const policyType = Form.useWatch('policyType', policyForm) ?? ''
  const warnings = (() => {
    try {
      return policyWarnings(policyType, policyContent(watchedPolicy))
    } catch {
      return []
    }
  })()

  if (draft.isLoading) return <LoadingBlock />
  if (draft.error || !draft.data) return <QueryFailure error={draft.error} />
  return (
    <section>
      <Typography.Title level={2}>Draft 工作区</Typography.Title>
      <Space direction="vertical" className="full-width">
        <Alert
          type="info"
          showIcon
          message={`当前 Revision：${draft.data.revision}；保存必须匹配该版本，409 不会自动覆盖。`}
        />
        {localConflict && (
          <Alert
            type="error"
            showIcon
            message="Draft 已被其他操作者修改"
            description={`服务端 Revision：${localConflict.currentRevision ?? '未知'}。本地表单仍保留，请比较后手工处理。`}
          />
        )}
        <Tabs
          activeKey={active}
          items={[
            {
              key: 'routes',
              label: 'Routes',
              children: (
                <Card
                  extra={<Button type="primary" disabled={!canWrite} onClick={openNewRoute}>新增 Route</Button>}
                >
                  <Table<DraftRoute>
                    rowKey="routeId"
                    dataSource={draft.data.routes}
                    scroll={{ x: 900 }}
                    columns={[
                      { title: 'Route ID', dataIndex: 'routeId' },
                      { title: 'Operation ID', dataIndex: 'operationId' },
                      {
                        title: 'Access Zones',
                        render: (_, row) =>
                          readRouteForm(row.routeContent).accessZones?.join(', ') ?? '-',
                      },
                      {
                        title: 'Route Profile',
                        render: (_, row) => String(
                          row.routeContent.transportPolicy?.profile ?? 'DEFAULT',
                        ),
                      },
                      { title: '状态', render: (_, row) => row.enabled ? '启用' : '禁用' },
                      {
                        title: '操作',
                        render: (_, row) => (
                          <Space>
                            <Button
                              disabled={!canWrite}
                              onClick={() => openExistingRoute(row)}
                            >
                              编辑
                            </Button>
                            <Popconfirm
                              title="确认删除 Route？"
                              description="删除会产生新的 Draft Revision。"
                              onConfirm={() => deleteRoute.mutate(row)}
                            >
                              <Button danger disabled={!canWrite}>删除</Button>
                            </Popconfirm>
                          </Space>
                        ),
                      },
                    ]}
                  />
                </Card>
              ),
            },
            {
              key: 'policies',
              label: 'Policies',
              children: (
                <Card
                  extra={<Button type="primary" disabled={!canWrite} onClick={() => setPolicyOpen(true)}>新增 Policy</Button>}
                >
                  <Table<DraftPolicy>
                    rowKey="policyId"
                    dataSource={draft.data.policies}
                    columns={[
                      { title: 'Policy ID', dataIndex: 'policyId' },
                      { title: '类型', dataIndex: 'policyType' },
                      { title: '作用域', dataIndex: 'policyScope' },
                      { title: '状态', render: (_, row) => row.enabled ? '启用' : '禁用' },
                      {
                        title: '操作',
                        render: (_, row) => (
                          <Space>
                            <Button
                              disabled={!canWrite}
                              onClick={() => {
                                policyForm.setFieldsValue({
                                  policyId: row.policyId,
                                  policyType: row.policyType,
                                  policyScope: row.policyScope,
                                  ...row.policyContent,
                                  allowedOrigins: Array.isArray(
                                    row.policyContent.allowedOrigins,
                                  )
                                    ? row.policyContent.allowedOrigins.join(',')
                                    : undefined,
                                  advancedContent: '{}',
                                  enabled: row.enabled,
                                  changeReason: 'Edit policy from Gateway Admin Web',
                                })
                                setPolicyOpen(true)
                              }}
                            >
                              编辑
                            </Button>
                            <Popconfirm
                              title="确认删除 Policy？"
                              description="删除会影响引用该策略的路由。"
                              onConfirm={() => deletePolicy.mutate(row)}
                            >
                              <Button danger disabled={!canWrite}>删除</Button>
                            </Popconfirm>
                          </Space>
                        ),
                      },
                    ]}
                  />
                </Card>
              ),
            },
          ]}
        />
        {diff.data && <JsonPanel title="与基线 Diff" value={diff.data} />}
      </Space>
      <Modal
        title="Route Transport"
        open={routeOpen}
        onCancel={closeRoute}
        onOk={() => routeForm.submit()}
        confirmLoading={saveRoute.isPending || validatingRoute}
        destroyOnHidden
        width={760}
        styles={{ body: { maxHeight: '72vh', overflowY: 'auto' } }}
      >
        <Form
          form={routeForm}
          layout="vertical"
          initialValues={{
            accessZones: ['INTERNAL'],
            enabled: true,
            httpMethod: 'GET',
            priority: 0,
            advancedContent: '{}',
          }}
          onFinish={submitRoute}
        >
          <Form.Item name="routeId" label="Route ID" rules={[{ required: true }]}><Input /></Form.Item>
          <Form.Item
            name="operationId"
            label="Operation ID"
            rules={[{ required: true, message: 'Operation ID 不能为空' }]}
          >
            <Input
              onChange={(event) => {
                if (routeOperation?.operationId !== event.target.value.trim()) {
                  operationRequest.current += 1
                  setRouteOperation(undefined)
                }
              }}
              onBlur={(event) => void loadRouteOperation(event.target.value)}
            />
          </Form.Item>
          {currentOperation?.loading && (
            <Alert type="info" showIcon message="正在从服务端读取 Operation Protocol…" />
          )}
          {currentOperation?.error && (
            <Alert type="error" showIcon message={currentOperation.error} />
          )}
          {currentOperation?.protocol && (
            <Alert
              type={currentOperation.protocol === 'HTTP' ? 'success' : 'info'}
              showIcon
              message={`Operation Protocol：${currentOperation.protocol === 'RPC' ? 'RPC / gRPC' : 'HTTP'}`}
              description={currentOperation.protocol === 'RPC'
                ? '协议来自服务端 Operation；RPC 保持既有聚合路径，不能选择 WebSocket 或 Streaming Transport。'
                : `协议来自服务端 Operation；外部访问：${currentOperation.externalAccessible ? '允许' : '不允许'}`}
            />
          )}
          {!currentOperation && currentOperationId && (
            <Alert type="info" showIcon message="移出 Operation ID 输入框后读取服务端协议。" />
          )}
          <Form.Item
            name="host"
            label="Host"
            rules={[{
              required: true,
              message: legacyHostMissing
                ? '历史草稿缺少 Host，请补录'
                : 'Host 不能为空',
            }]}
          >
            <Input placeholder="api.example.com" />
          </Form.Item>
          <Form.Item
            name="accessZones"
            label="Access Zones"
            rules={[{ required: true, message: '至少选择一个 Access Zone' }]}
          >
            <Select
              mode="multiple"
              options={['INTERNAL', 'PUBLIC'].map((value) => ({ value }))}
            />
          </Form.Item>
          <Form.Item
            name="httpMethod"
            label="HTTP Method"
            rules={[{ required: true, message: 'HTTP Method 不能为空' }]}
          >
            <Select
              options={['GET', 'POST', 'PUT', 'DELETE', 'PATCH', 'HEAD', 'OPTIONS']
                .map((value) => ({ value }))}
            />
          </Form.Item>
          <Form.Item
            name="pathPattern"
            label="Path Pattern"
            rules={[
              { required: true, message: 'Path Pattern 不能为空' },
              { pattern: /^\//, message: 'Path Pattern 必须以 / 开头' },
            ]}
          >
            <Input placeholder="/v1/**" />
          </Form.Item>
          <Alert
            type="warning"
            showIcon
            message="Provider 只能通过注册中心发现，本表单不提供静态 URL。"
          />

          {currentTransportState.transportEditable && (
            <Card size="small" title="Transport Policy">
              <Form.Item
                name={['transportPolicy', 'profile']}
                label="Route Profile"
                extra="未显式覆盖的 Profile 默认值仅用于展示，不会自动固化到 Route JSON。"
              >
                <Select
                  allowClear
                  placeholder="DEFAULT（不写入 Profile）"
                  options={['DEFAULT', 'OPENAI_HTTP'].map((value) => ({ value }))}
                />
              </Form.Item>
              {currentTransportPolicy?.profile === 'OPENAI_HTTP' && (
                <Alert
                  type="info"
                  showIcon
                  message="OPENAI_HTTP 只提供透明流式传输默认值"
                  description="Route Override 优先；未覆盖值来自 Profile，保存时不会展开为完整默认配置。"
                />
              )}
              <Form.Item
                name={['transportPolicy', 'transportProtocol']}
                label="Transport Protocol"
                extra={transportFieldExtra(currentTransportPolicy, 'transportProtocol')}
              >
                <Select
                  allowClear
                  placeholder={displayTransportValue(
                    transportFieldPresentation(
                      currentTransportPolicy,
                      'transportProtocol',
                    ).value,
                  )}
                  options={['HTTP', 'WEBSOCKET'].map((value) => ({ value }))}
                />
              </Form.Item>

              {currentTransportState.bodyModesVisible && (
                <>
                  <Form.Item
                    name={['transportPolicy', 'requestBodyMode']}
                    label="Request Body Mode"
                    extra={transportFieldExtra(currentTransportPolicy, 'requestBodyMode')}
                  >
                    <Select
                      allowClear
                      placeholder={displayTransportValue(
                        transportFieldPresentation(
                          currentTransportPolicy,
                          'requestBodyMode',
                        ).value,
                      )}
                      options={['AGGREGATED', 'STREAMING'].map((value) => ({ value }))}
                    />
                  </Form.Item>
                  <Form.Item
                    name={['transportPolicy', 'responseMode']}
                    label="Response Mode"
                    extra={transportFieldExtra(currentTransportPolicy, 'responseMode')}
                  >
                    <Select
                      allowClear
                      placeholder={displayTransportValue(
                        transportFieldPresentation(currentTransportPolicy, 'responseMode').value,
                      )}
                      options={['STANDARD', 'AUTO_STREAM', 'SSE', 'BINARY_STREAM']
                        .map((value) => ({ value }))}
                    />
                  </Form.Item>
                </>
              )}

              {currentTransportState.transparentResponseNotice && (
                <Alert
                  type="warning"
                  showIcon
                  message={currentTransportState.transparentResponseNotice}
                />
              )}

              <Form.Item
                name={['transportPolicy', 'maxRequestBodyBytes']}
                label="最大请求体（bytes）"
                extra={transportFieldExtra(currentTransportPolicy, 'maxRequestBodyBytes')}
              >
                <InputNumber min={1} max={1_073_741_824} style={{ width: '100%' }} />
              </Form.Item>
              <Form.Item
                name={['transportPolicy', 'connectTimeoutMs']}
                label="Connect Timeout（ms）"
                extra={transportFieldExtra(currentTransportPolicy, 'connectTimeoutMs')}
              >
                <InputNumber min={100} max={60_000} style={{ width: '100%' }} />
              </Form.Item>
              <Form.Item
                name={['transportPolicy', 'responseHeaderTimeoutMs']}
                label="Response Header Timeout（ms）"
                extra={transportFieldExtra(currentTransportPolicy, 'responseHeaderTimeoutMs')}
              >
                <InputNumber min={1_000} max={600_000} style={{ width: '100%' }} />
              </Form.Item>
              <Form.Item
                name={['transportPolicy', 'streamIdleTimeoutMs']}
                label="Stream Idle Timeout（ms）"
                extra={transportFieldExtra(currentTransportPolicy, 'streamIdleTimeoutMs')}
              >
                <InputNumber min={1_000} max={1_800_000} style={{ width: '100%' }} />
              </Form.Item>
              <Form.Item
                name={['transportPolicy', 'totalTimeoutMs']}
                label="Total Timeout（ms）"
                extra={transportFieldExtra(currentTransportPolicy, 'totalTimeoutMs')}
              >
                <InputNumber min={1_000} max={7_200_000} style={{ width: '100%' }} />
              </Form.Item>
              <Form.Item
                name={['transportPolicy', 'websocketIdleTimeoutMs']}
                label="WebSocket Idle Timeout（ms）"
                extra={transportFieldExtra(currentTransportPolicy, 'websocketIdleTimeoutMs')}
              >
                <InputNumber min={1_000} max={7_200_000} style={{ width: '100%' }} />
              </Form.Item>
              <Form.Item
                name={['transportPolicy', 'websocketMaxFrameBytes']}
                label="WebSocket 最大 Frame（bytes）"
                extra={transportFieldExtra(currentTransportPolicy, 'websocketMaxFrameBytes')}
              >
                <InputNumber min={1_024} max={67_108_864} style={{ width: '100%' }} />
              </Form.Item>
              <Form.Item
                name={['transportPolicy', 'bodyLogEnabled']}
                label="Body 日志 Override"
                extra={transportFieldExtra(currentTransportPolicy, 'bodyLogEnabled')}
                getValueProps={(value: boolean | undefined) => ({
                  value: value === undefined ? undefined : String(value),
                })}
                normalize={(value: string | undefined) =>
                  value === undefined ? undefined : value === 'true'}
              >
                <Select
                  allowClear
                  placeholder="继承 Profile"
                  options={[
                    { value: 'false', label: '关闭' },
                    { value: 'true', label: '开启' },
                  ]}
                />
              </Form.Item>
              <Form.Item
                name={['transportPolicy', 'retryEnabled']}
                label="重试 Override"
                extra={transportFieldExtra(currentTransportPolicy, 'retryEnabled')}
                getValueProps={(value: boolean | undefined) => ({
                  value: value === undefined ? undefined : String(value),
                })}
                normalize={(value: string | undefined) =>
                  value === undefined ? undefined : value === 'true'}
              >
                <Select
                  allowClear
                  placeholder="继承 Profile / Traffic Policy"
                  options={[
                    { value: 'false', label: '关闭' },
                    { value: 'true', label: '允许' },
                  ]}
                />
              </Form.Item>
              {currentTransportState.retryNotice && (
                <Alert type="warning" showIcon message={currentTransportState.retryNotice} />
              )}
            </Card>
          )}

          <Form.Item name="priority" label="优先级"><InputNumber style={{ width: '100%' }} /></Form.Item>
          <Form.Item
            name="advancedContent"
            label="高级扩展 JSON"
            rules={[{
              validator: async (_, value: string) => {
                try {
                  json(value || '{}')
                } catch {
                  throw new Error('高级扩展内容必须是 JSON Object')
                }
              },
            }]}
          >
            <Input.TextArea rows={5} />
          </Form.Item>
          <Form.Item name="enabled" label="启用" valuePropName="checked"><Switch /></Form.Item>
          <Form.Item name="changeReason" label="变更原因" rules={[{ required: true }]}><Input /></Form.Item>
        </Form>
      </Modal>
      <Modal
        title="Policy"
        open={policyOpen}
        onCancel={() => setPolicyOpen(false)}
        onOk={() => policyForm.submit()}
        confirmLoading={savePolicy.isPending}
        destroyOnHidden
      >
        <Form
          form={policyForm}
          layout="vertical"
          initialValues={{
            enabled: true,
            policyScope: 'ROUTE',
            advancedContent: '{}',
            failureMode: 'FAIL_CLOSED',
          }}
          onFinish={(values) => savePolicy.mutate(values)}
        >
          <Form.Item name="policyId" label="Policy ID" rules={[{ required: true }]}><Input /></Form.Item>
          <Form.Item name="policyType" label="能力" rules={[{ required: true }]}>
            <Select options={['TIMEOUT', 'RATE_LIMIT', 'BULKHEAD', 'CIRCUIT_BREAKER', 'RETRY', 'LOAD_BALANCE', 'SECURITY', 'CORS'].map((value) => ({ value }))} />
          </Form.Item>
          <Form.Item name="policyScope" label="作用域" rules={[{ required: true }]}><Select options={['GLOBAL', 'GROUP', 'ROUTE', 'OPERATION'].map((value) => ({ value }))} /></Form.Item>
          <Form.Item name="timeoutMs" label="超时（ms）"><Input type="number" min={1} /></Form.Item>
          <Form.Item name="permitsPerSecond" label="每秒许可数"><Input type="number" min={1} /></Form.Item>
          <Form.Item name="maxConcurrent" label="最大并发"><Input type="number" min={1} /></Form.Item>
          <Form.Item name="maxAttempts" label="最大尝试次数"><Input type="number" min={1} /></Form.Item>
          <Form.Item name="strategy" label="负载均衡策略">
            <Select allowClear options={['ROUND_ROBIN', 'WEIGHTED_RANDOM', 'LEAST_ACTIVE', 'CONSISTENT_HASH'].map((value) => ({ value }))} />
          </Form.Item>
          <Form.Item name="failureMode" label="失败模式">
            <Select options={['FAIL_CLOSED', 'FAIL_OPEN'].map((value) => ({ value }))} />
          </Form.Item>
          <Form.Item name="allowedOrigins" label="CORS Origins（逗号分隔）"><Input /></Form.Item>
          {warnings.map((warning) => <Alert key={warning} type="warning" showIcon message={warning} />)}
          <Form.Item name="advancedContent" label="高级扩展 JSON（单位必须显式）"><Input.TextArea rows={4} /></Form.Item>
          <Form.Item name="enabled" label="启用" valuePropName="checked"><Switch /></Form.Item>
          <Form.Item name="changeReason" label="变更原因" rules={[{ required: true }]}><Input /></Form.Item>
        </Form>
      </Modal>
    </section>
  )
}
