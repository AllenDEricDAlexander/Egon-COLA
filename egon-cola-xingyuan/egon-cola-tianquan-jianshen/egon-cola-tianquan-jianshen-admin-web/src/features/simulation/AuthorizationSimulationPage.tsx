import {PermissionGuard, useRbac3Authorization} from '@egon-cola/tianquan-jianshen-react-sdk'
import {useMutation} from '@tanstack/react-query'
import {Alert, Button, Card, Col, Descriptions, Form, Input, Row, Space, Tag, Typography} from 'antd'
import {useFeatureApi} from '../shared/FeatureApi'
import {PageState} from '@egon-cola/xingyuan-admin-web-shared'
import {type AuthorizationSimulationCommand, simulationApi, type SimulationDecisionBundle} from './simulation.api'

interface SimulationForm {
  readonly permissionCode: string
  readonly applicationCode: string
  readonly resourceCode: string
  readonly addedPermissions?: string
  readonly removedPermissions?: string
}

interface RoleImpactForm {
  readonly roleId: string
}

const permissionList = (value?: string) => value?.split(',').map((item) => item.trim()).filter(Boolean) ?? []

export const AuthorizationSimulationPage = () => {
    const {about} = useRbac3Authorization()
  const api = simulationApi(useFeatureApi())
  const simulation = useMutation({
    mutationFn: (form: SimulationForm) => {
      if (!about) throw new Error('ABOUT_REQUIRED')
      const command: AuthorizationSimulationCommand = {
        decisionRequest: {
            subject: {
                tenantId: about.user.tenantId,
                userId: about.user.subject,
                identitySub: about.user.subject
            },
          permissionCode: form.permissionCode,
          resource: { applicationCode: form.applicationCode, resourceCode: form.resourceCode },
          requestedDecisions: ['FUNCTION', 'DATA_SCOPE', 'FIELD', 'PARTICIPATION', 'FENCE'],
            tokenVersions: {authVersion: about.authVersion, policyVersion: about.policyVersion},
        },
        hypothesis: { addedPermissions: permissionList(form.addedPermissions), removedPermissions: permissionList(form.removedPermissions) },
        at: new Date().toISOString(),
      }
      return api.simulate(command)
    },
  })
  const roleImpact = useMutation({
    mutationFn: (form: RoleImpactForm) => api.roleChangeImpact(form.roleId),
  })
  return (
    <Card title="授权模拟">
        <Alert type="info" showIcon title="模拟固定在一个一致快照上，仅返回判断和证据，不修改角色、权限或业务数据。"/>
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
              <Descriptions.Item label="Policy Version">{simulation.data.policyVersion}</Descriptions.Item>
              <Descriptions.Item label="结果过期">{simulation.data.expiresAt}</Descriptions.Item>
              <Descriptions.Item label="Snapshot Checksum" span={4}>{simulation.data.snapshotChecksum}</Descriptions.Item>
            </Descriptions>
          </Space>
        )}
      </PageState>
      <Card size="small" title="角色变更影响" style={{ marginTop: 24 }}>
        <Typography.Paragraph type="secondary">
          输入角色 ID，查询该角色当前激活根、角色族风险、权限数量和冲突证据；结果只读且带策略版本与过期时间。
        </Typography.Paragraph>
        <Form<RoleImpactForm> layout="inline" onFinish={roleImpact.mutate}>
          <Form.Item name="roleId" label="Role ID" rules={[{ required: true, whitespace: true }]}>
            <Input aria-label="Role ID" placeholder="例如 1001" />
          </Form.Item>
          <Form.Item>
            <PermissionGuard permission="system:authorization-simulation:execute">
              <Button type="primary" htmlType="submit" loading={roleImpact.isPending}>查询角色变更影响</Button>
            </PermissionGuard>
          </Form.Item>
        </Form>
        <PageState loading={false} error={roleImpact.error} empty={!roleImpact.data} emptyDescription="输入 Role ID 后查询">
          {roleImpact.data && (
            <Descriptions bordered size="small" column={2} style={{ marginTop: 16 }}>
              <Descriptions.Item label="Role ID">{roleImpact.data.impact.roleId}</Descriptions.Item>
              <Descriptions.Item label="Family Risk"><Tag>{roleImpact.data.impact.effectiveFamilyRisk}</Tag></Descriptions.Item>
              <Descriptions.Item label="Permission Count">{roleImpact.data.impact.permissionCount}</Descriptions.Item>
              <Descriptions.Item label="Policy Version">{roleImpact.data.policyVersion}</Descriptions.Item>
              <Descriptions.Item label="Activation Roots" span={2}>{roleImpact.data.impact.activationRoots.join(', ') || '-'}</Descriptions.Item>
              <Descriptions.Item label="Role Family" span={2}>{roleImpact.data.impact.roleFamily.join(', ') || '-'}</Descriptions.Item>
              <Descriptions.Item label="Conflicts" span={2}>{roleImpact.data.impact.conflicts.join(', ') || '无'}</Descriptions.Item>
              <Descriptions.Item label="Evidence Checksum" span={2}>{roleImpact.data.evidenceChecksum}</Descriptions.Item>
              <Descriptions.Item label="Expires At" span={2}>{roleImpact.data.expiresAt}</Descriptions.Item>
            </Descriptions>
          )}
        </PageState>
      </Card>
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
