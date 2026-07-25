import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  Alert,
  Button,
  Card,
  Form,
  Input,
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
import { useState } from 'react'
import { useLocation, useParams } from 'react-router-dom'
import { GatewayApiError } from '../../api/client'
import { gatewayApi } from '../../api/gatewayApi'
import type { DraftPolicy, DraftRoute } from '../../api/types'
import { JsonPanel } from '../../components/JsonPanel'
import { LoadingBlock, QueryFailure } from '../../components/QueryState'
import { useCapability } from '../../app/capabilities'
import { policyWarnings, validatePublicRoute } from './routeValidation'

const json = (value: string): Record<string, unknown> => {
  const parsed = JSON.parse(value) as unknown
  if (!parsed || Array.isArray(parsed) || typeof parsed !== 'object') {
    throw new Error('内容必须是 JSON Object')
  }
  return parsed as Record<string, unknown>
}

const routeContent = (values: any): Record<string, unknown> => ({
  ...json(values.advancedContent || '{}'),
  listener: values.accessZone,
  protocol: values.protocol,
  method: values.protocol === 'HTTP' ? values.method : undefined,
  path: values.protocol === 'HTTP' ? values.path : undefined,
  fullMethodName: values.protocol === 'RPC' ? values.fullMethodName : undefined,
  providerServiceName: values.providerServiceName,
  priority: Number(values.priority ?? 0),
})

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

export const DraftPage = () => {
  const { groupId = '' } = useParams()
  const location = useLocation()
  const queryClient = useQueryClient()
  const canWrite = useCapability('gateway:drafts:write')
  const [routeForm] = Form.useForm()
  const [policyForm] = Form.useForm()
  const [routeOpen, setRouteOpen] = useState(false)
  const [policyOpen, setPolicyOpen] = useState(false)
  const [localConflict, setLocalConflict] = useState<GatewayApiError>()
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
    mutationFn: (values: any) => {
      const constraint = validatePublicRoute(
        values.accessZone,
        values.operationExternalAccessible,
      )
      if (constraint) throw new Error(constraint)
      return gatewayApi.saveRoute(
        groupId,
        values.routeId,
        {
          operationId: values.operationId,
          content: {
            ...routeContent(values),
          },
          enabled: values.enabled,
          changeReason: values.changeReason,
        },
        draft.data!.revision,
      )
    },
    onSuccess: async () => {
      setRouteOpen(false)
      setLocalConflict(undefined)
      await queryClient.invalidateQueries({ queryKey: ['draft', groupId] })
      await queryClient.invalidateQueries({ queryKey: ['draft-diff', groupId] })
      void message.success('Route 已保存')
    },
    onError: (error) => {
      if (error instanceof GatewayApiError && error.status === 409) {
        setLocalConflict(error)
      }
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
  const active = location.pathname.endsWith('/policies') ? 'policies' : 'routes'
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
                  extra={<Button type="primary" disabled={!canWrite} onClick={() => setRouteOpen(true)}>新增 Route</Button>}
                >
                  <Table<DraftRoute>
                    rowKey="routeId"
                    dataSource={draft.data.routes}
                    scroll={{ x: 900 }}
                    columns={[
                      { title: 'Route ID', dataIndex: 'routeId' },
                      { title: 'Operation ID', dataIndex: 'operationId' },
                      { title: 'Listener', render: (_, row) => String(row.routeContent.listener ?? '-') },
                      { title: '协议', render: (_, row) => String(row.routeContent.protocol ?? '-') },
                      { title: '状态', render: (_, row) => row.enabled ? '启用' : '禁用' },
                      {
                        title: '操作',
                        render: (_, row) => (
                          <Space>
                            <Button
                              disabled={!canWrite}
                              onClick={() => {
                                routeForm.setFieldsValue({
                                  routeId: row.routeId,
                                  operationId: row.operationId,
                                  accessZone: row.routeContent.listener ?? 'INTERNAL',
                                  operationExternalAccessible:
                                    row.routeContent.operationExternalAccessible ?? false,
                                  protocol: row.routeContent.protocol ?? 'HTTP',
                                  method: row.routeContent.method,
                                  path: row.routeContent.path,
                                  fullMethodName: row.routeContent.fullMethodName,
                                  providerServiceName: row.routeContent.providerServiceName,
                                  priority: row.routeContent.priority ?? 0,
                                  advancedContent: '{}',
                                  enabled: row.enabled,
                                  changeReason: 'Edit route from Gateway Admin Web',
                                })
                                setRouteOpen(true)
                              }}
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
        title="Route"
        open={routeOpen}
        onCancel={() => setRouteOpen(false)}
        onOk={() => routeForm.submit()}
        confirmLoading={saveRoute.isPending}
        destroyOnHidden
      >
        <Form
          form={routeForm}
          layout="vertical"
          initialValues={{
            accessZone: 'INTERNAL',
            enabled: true,
            operationExternalAccessible: false,
            protocol: 'HTTP',
            method: 'GET',
            priority: 0,
            advancedContent: '{}',
          }}
          onFinish={(values) => saveRoute.mutate(values)}
        >
          <Form.Item name="routeId" label="Route ID" rules={[{ required: true }]}><Input /></Form.Item>
          <Form.Item name="operationId" label="Operation ID" rules={[{ required: true }]}><Input /></Form.Item>
          <Form.Item name="accessZone" label="Listener / Access Zone" rules={[{ required: true }]}>
            <Select options={['INTERNAL', 'PUBLIC'].map((value) => ({ value }))} />
          </Form.Item>
          <Form.Item name="operationExternalAccessible" label="Operation 允许外部访问" valuePropName="checked">
            <Switch />
          </Form.Item>
          <Alert
            type="warning"
            showIcon
            message="Provider 只能通过注册中心发现，本表单不提供静态 URL。"
          />
          <Form.Item name="protocol" label="协议" rules={[{ required: true }]}>
            <Select options={['HTTP', 'RPC'].map((value) => ({ value }))} />
          </Form.Item>
          <Form.Item noStyle shouldUpdate={(previous, current) => previous.protocol !== current.protocol}>
            {({ getFieldValue }) => getFieldValue('protocol') === 'HTTP' ? (
              <>
                <Form.Item name="method" label="HTTP Method" rules={[{ required: true }]}>
                  <Select options={['GET', 'POST', 'PUT', 'DELETE', 'PATCH'].map((value) => ({ value }))} />
                </Form.Item>
                <Form.Item name="path" label="Path" rules={[{ required: true }]}><Input /></Form.Item>
              </>
            ) : (
              <Form.Item name="fullMethodName" label="RPC Full Method" rules={[{ required: true }]}>
                <Input />
              </Form.Item>
            )}
          </Form.Item>
          <Form.Item name="providerServiceName" label="Provider Service" rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item name="priority" label="优先级"><Input type="number" /></Form.Item>
          <Form.Item name="advancedContent" label="高级扩展 JSON"><Input.TextArea rows={4} /></Form.Item>
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
