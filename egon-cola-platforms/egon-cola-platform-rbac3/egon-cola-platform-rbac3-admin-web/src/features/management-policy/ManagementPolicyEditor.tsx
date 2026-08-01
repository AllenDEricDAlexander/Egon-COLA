import { Button, Checkbox, Form, Input, InputNumber, Modal, Select } from 'antd'
import type { ManagementPolicyView, PolicyScope, PolicySubject, SaveManagementPolicyCommand } from './managementPolicy.api'

interface PolicyForm {
  readonly policyCode: string
  readonly name: string
  readonly validFrom: string
  readonly validTo?: string
  readonly subjects: string
  readonly scopes: string
  readonly activationRootRoleIds: string
  readonly operations: string
  readonly maximumAssignmentDays?: number
  readonly maximumRiskLevel: string
  readonly requiredAuthenticationStrength: string
  readonly requireReason: boolean
  readonly requireTicket: boolean
  readonly includeInheritedSubjectRoles: boolean
  readonly requireAllAffiliationsInScope: boolean
}

export interface ManagementPolicyEditorProps {
  readonly open: boolean
  readonly policy: ManagementPolicyView | null
  readonly saving: boolean
  readonly onCancel: () => void
  readonly onSave: (command: SaveManagementPolicyCommand) => void
}

const pairs = <T extends PolicySubject | PolicyScope>(value: string, referenceField: 'id' | 'referenceId'): T[] => value
  .split(',').map((item) => item.trim()).filter(Boolean).map((item) => {
    const [type, reference] = item.split(':', 2)
    return { type, [referenceField]: reference || null } as unknown as T
  })

const values = (policy: ManagementPolicyView | null): Partial<PolicyForm> => ({
  policyCode: policy?.policyCode,
  name: policy?.name,
  validFrom: policy?.validFrom.slice(0, 16),
  validTo: policy?.validTo?.slice(0, 16),
  subjects: policy?.subjects.map((subject) => `${subject.type}:${subject.id}`).join(', '),
  scopes: policy?.scopes.map((scope) => `${scope.type}:${scope.referenceId ?? ''}`).join(', '),
  activationRootRoleIds: policy?.activationRootRoleIds.join(', '),
  operations: policy?.operations.join(', '),
  maximumAssignmentDays: policy?.restrictions.maximumAssignmentDays ?? undefined,
  maximumRiskLevel: policy?.restrictions.maximumRiskLevel ?? 'HIGH',
  requiredAuthenticationStrength: policy?.restrictions.requiredAuthenticationStrength ?? 'PASSWORD',
  requireReason: policy?.restrictions.requireReason ?? true,
  requireTicket: policy?.restrictions.requireTicket ?? false,
  includeInheritedSubjectRoles: policy?.restrictions.includeInheritedSubjectRoles ?? false,
  requireAllAffiliationsInScope: policy?.restrictions.requireAllAffiliationsInScope ?? true,
})

export const ManagementPolicyEditor = ({ open, policy, saving, onCancel, onSave }: ManagementPolicyEditorProps) => (
  <Modal title={policy ? '编辑完整委托策略' : '新增完整委托策略'} open={open} footer={null} onCancel={onCancel} width={720} destroyOnHidden>
    <Form<PolicyForm>
      key={policy?.policyId ?? 'new'}
      layout="vertical"
      initialValues={values(policy)}
      onFinish={(form) => onSave({
        policyCode: form.policyCode,
        name: form.name,
        validFrom: new Date(form.validFrom).toISOString(),
        validTo: form.validTo ? new Date(form.validTo).toISOString() : null,
        subjects: pairs<PolicySubject>(form.subjects, 'id'),
        scopes: pairs<PolicyScope>(form.scopes, 'referenceId'),
        activationRootRoleIds: form.activationRootRoleIds.split(',').map((item) => item.trim()).filter(Boolean),
        operations: form.operations.split(',').map((item) => item.trim()).filter(Boolean),
        restrictions: {
          maximumAssignmentDays: form.maximumAssignmentDays ?? null,
          maximumRiskLevel: form.maximumRiskLevel,
          requiredAuthenticationStrength: form.requiredAuthenticationStrength,
          requireReason: form.requireReason,
          requireTicket: form.requireTicket,
          includeInheritedSubjectRoles: form.includeInheritedSubjectRoles,
          requireAllAffiliationsInScope: form.requireAllAffiliationsInScope,
        },
      })}
    >
      <Form.Item name="policyCode" label="策略编码" rules={[{ required: true }]}><Input disabled={policy !== null} /></Form.Item>
      <Form.Item name="name" label="名称" rules={[{ required: true }]}><Input /></Form.Item>
      <Form.Item name="validFrom" label="生效时间" rules={[{ required: true }]}><Input type="datetime-local" /></Form.Item>
      <Form.Item name="validTo" label="失效时间"><Input type="datetime-local" /></Form.Item>
      <Form.Item name="subjects" label="Subject 集合（TYPE:ID，逗号分隔）" rules={[{ required: true }]}><Input.TextArea /></Form.Item>
      <Form.Item name="scopes" label="Scope 集合（TYPE:REFERENCE，逗号分隔）" rules={[{ required: true }]}><Input.TextArea /></Form.Item>
      <Form.Item name="activationRootRoleIds" label="激活根角色白名单（逗号分隔）" rules={[{ required: true }]}><Input.TextArea /></Form.Item>
      <Form.Item name="operations" label="操作集合（逗号分隔）" rules={[{ required: true }]}><Input.TextArea /></Form.Item>
      <Form.Item name="maximumAssignmentDays" label="最长任职天数"><InputNumber min={1} precision={0} style={{ width: '100%' }} /></Form.Item>
      <Form.Item name="maximumRiskLevel" label="最高风险等级" rules={[{ required: true }]}><Select options={['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'].map((value) => ({ value }))} /></Form.Item>
      <Form.Item name="requiredAuthenticationStrength" label="最低认证强度" rules={[{ required: true }]}><Select options={['PASSWORD', 'MFA', 'HARDWARE_KEY'].map((value) => ({ value }))} /></Form.Item>
      <Form.Item name="requireReason" valuePropName="checked"><Checkbox>必须填写原因</Checkbox></Form.Item>
      <Form.Item name="requireTicket" valuePropName="checked"><Checkbox>必须填写外部工单号</Checkbox></Form.Item>
      <Form.Item name="includeInheritedSubjectRoles" valuePropName="checked"><Checkbox>Subject 角色包含继承角色</Checkbox></Form.Item>
      <Form.Item name="requireAllAffiliationsInScope" valuePropName="checked"><Checkbox>所有归属均需落在 Scope 内</Checkbox></Form.Item>
      <Button type="primary" htmlType="submit" loading={saving}>完整保存策略</Button>
    </Form>
  </Modal>
)
