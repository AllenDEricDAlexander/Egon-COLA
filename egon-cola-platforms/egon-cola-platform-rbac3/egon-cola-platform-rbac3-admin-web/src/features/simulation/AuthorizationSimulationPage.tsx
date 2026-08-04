import { PermissionGuard, useRbac3Session } from '@egon-cola/rbac3-react-sdk'
import { useMutation } from '@tanstack/react-query'
import { Alert, Button, Card, Col, Descriptions, Form, Input, Row, Space, Tag, Typography } from 'antd'
import { useFeatureApi } from '../shared/FeatureApi'
import { PageState } from '@egon-cola/admin-web-shared'
import { simulationApi, type AuthorizationSimulationCommand, type SimulationDecisionBundle } from './simulation.api'

interface SimulationForm {
  readonly permissionCode: string
  readonly applicationCode: string
  readonly resourceCode: string
  readonly addedPermissions?: string
  readonly removedPermissions?: string
}

const permissionList = (value?: string) => value?.split(',').map((item) => item.trim()).filter(Boolean) ?? []

export const AuthorizationSimulationPage = () => {
  const { bootstrap } = useRbac3Session()
  const api = simulationApi(useFeatureApi())
  const simulation = useMutation({
    mutationFn: (form: SimulationForm) => {
      if (!bootstrap) throw new Error('BOOTSTRAP_REQUIRED')
      const command: AuthorizationSimulationCommand = {
        decisionRequest: {
          subject: { tenantId: bootstrap.user.tenantId, userId: bootstrap.user.id, sessionId: bootstrap.sessionId },
          permissionCode: form.permissionCode,
          resource: { applicationCode: form.applicationCode, resourceCode: form.resourceCode },
          requestedDecisions: ['FUNCTION', 'DATA_SCOPE', 'FIELD', 'PARTICIPATION', 'FENCE'],
          tokenVersions: { authVersion: bootstrap.authVersion, sessionVersion: bootstrap.sessionVersion, policyVersion: bootstrap.policyVersion },
        },
        hypothesis: { addedPermissions: permissionList(form.addedPermissions), removedPermissions: permissionList(form.removedPermissions) },
        at: new Date().toISOString(),
      }
      return api.simulate(command)
    },
  })
  return (
    <Card title="授权模拟">
      <Alert type="info" showIcon message="模拟固定在一个一致快照上，仅返回判断和证据，不修改角色、权限、会话或业务数据。" />
      <Form<SimulationForm> layout="vertical" onFinish={simulation.mutate} style={{ marginTop: 16 }}>
        <Row gutter={16}>
          <Col span={8}><Form.Item name="permissionCode" label="Permission Code" rules={[{ required: true }]}><Input aria-label="Permission Code" /></Form.Item></Col>
          <Col span={8}><Form.Item name="applicationCode" label="Application Code" rules={[{ required: true }]}><Input aria-label="Application Code" /></Form.Item></Col>
          <Col span={8}><Form.Item name="resourceCode" label="Resource Code" rules={[{ required: true }]}><Input aria-label="Resource Code" /></Form.Item></Col>
        </Row>
        <Form.Item name="addedPermissions" label="假设新增权限（逗号分隔）"><Input /></Form.Item>
        <Form.Item name="removedPermissions" label="假设移除权限（逗号分隔）"><Input /></Form.Item>
        <PermissionGuard permission="system:authorization-simulation:execute">
          <Button type="primary" htmlType="submit" loading={simulation.isPending}>执行无副作用模拟</Button>
        </PermissionGuard>
      </Form>
      <PageState loading={false} error={simulation.error} empty={!simulation.data} emptyDescription="填写条件后执行模拟">
        {simulation.data && (
          <Space orientation="vertical" size="middle" style={{ width: '100%', marginTop: 16 }}>
            <Row gutter={16}>
              <Col span={12}><DecisionCard title="当前判断" bundle={simulation.data.current} /></Col>
              <Col span={12}><DecisionCard title="假设判断" bundle={simulation.data.hypothetical} /></Col>
            </Row>
            <Descriptions bordered column={4}>
              <Descriptions.Item label="Auth Version">{simulation.data.authVersion}</Descriptions.Item>
              <Descriptions.Item label="Session Version">{simulation.data.sessionVersion}</Descriptions.Item>
              <Descriptions.Item label="Policy Version">{simulation.data.policyVersion}</Descriptions.Item>
              <Descriptions.Item label="结果过期">{simulation.data.expiresAt}</Descriptions.Item>
              <Descriptions.Item label="Snapshot Checksum" span={4}>{simulation.data.snapshotChecksum}</Descriptions.Item>
            </Descriptions>
          </Space>
        )}
      </PageState>
    </Card>
  )
}

const DecisionCard = ({ title, bundle }: { readonly title: string; readonly bundle: SimulationDecisionBundle }) => (
  <Card size="small" title={title}>
    <Space wrap>
      <Tag color={bundle.functionDecision.decision === 'ALLOW' ? 'green' : 'red'}>{bundle.functionDecision.decision}</Tag>
      <Typography.Text>{bundle.functionDecision.reasonCode}</Typography.Text>
      <Typography.Text type="secondary">证据 {bundle.functionDecision.evidenceIds.slice(0, 20).join(', ') || '-'}</Typography.Text>
    </Space>
  </Card>
)
