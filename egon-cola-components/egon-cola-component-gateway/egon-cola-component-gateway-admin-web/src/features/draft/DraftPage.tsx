import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  Alert,
  Button,
  Card,
  Form,
  Input,
  Modal,
  Select,
  Space,
  Switch,
  Table,
  Tabs,
  Typography,
  message,
} from 'antd'
import { useMemo, useState } from 'react'
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

export const DraftPage = () => {
  const { groupId = '' } = useParams()
  const location = useLocation()
  const queryClient = useQueryClient()
  const canWrite = useCapability('gateway.draft.write')
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
            ...json(values.content),
            listener: values.accessZone,
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
          content: json(values.content),
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
  const active = location.pathname.endsWith('/policies') ? 'policies' : 'routes'
  const policyContent = Form.useWatch('content', policyForm) ?? '{}'
  const policyType = Form.useWatch('policyType', policyForm) ?? ''
  const warnings = useMemo(() => {
    try {
      return policyWarnings(policyType, json(policyContent))
    } catch {
      return []
    }
  }, [policyContent, policyType])

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
            content: '{"protocol":"HTTP","path":"/example","method":"GET"}',
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
          <Form.Item name="content" label="协议 Route JSON" rules={[{ required: true }]}><Input.TextArea rows={8} /></Form.Item>
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
          initialValues={{ enabled: true, policyScope: 'ROUTE', content: '{}' }}
          onFinish={(values) => savePolicy.mutate(values)}
        >
          <Form.Item name="policyId" label="Policy ID" rules={[{ required: true }]}><Input /></Form.Item>
          <Form.Item name="policyType" label="能力" rules={[{ required: true }]}>
            <Select options={['TIMEOUT', 'RATE_LIMIT', 'BULKHEAD', 'CIRCUIT_BREAKER', 'RETRY', 'LOAD_BALANCE', 'SECURITY', 'CORS'].map((value) => ({ value }))} />
          </Form.Item>
          <Form.Item name="policyScope" label="作用域" rules={[{ required: true }]}><Select options={['GLOBAL', 'GROUP', 'ROUTE', 'OPERATION'].map((value) => ({ value }))} /></Form.Item>
          {warnings.map((warning) => <Alert key={warning} type="warning" showIcon message={warning} />)}
          <Form.Item name="content" label="策略 JSON（单位必须显式）" rules={[{ required: true }]}><Input.TextArea rows={10} /></Form.Item>
          <Form.Item name="enabled" label="启用" valuePropName="checked"><Switch /></Form.Item>
          <Form.Item name="changeReason" label="变更原因" rules={[{ required: true }]}><Input /></Form.Item>
        </Form>
      </Modal>
    </section>
  )
}
