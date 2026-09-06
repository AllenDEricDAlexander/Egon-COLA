import { Alert, Button, Form, Input, InputNumber, Modal, Select } from 'antd'
import type { CreateAssignmentCommand } from './assignment.api'

interface AssignmentForm {
  readonly roleId: string
  readonly validFrom: string
  readonly validTo?: string
  readonly assignmentType: string
  readonly reason?: string
  readonly ticketNo?: string
}

export interface AssignmentEditorProps {
  readonly open: boolean
  readonly saving: boolean
  readonly expectedUserAuthVersion: number
  readonly error?: unknown
  readonly onCancel: () => void
  readonly onSave: (command: CreateAssignmentCommand) => void
}

export const AssignmentEditor = ({ open, saving, error, expectedUserAuthVersion, onCancel, onSave }: AssignmentEditorProps) => (
  <Modal title="新增任职资格" open={open} footer={null} onCancel={onCancel} destroyOnHidden>
    {error != null && <Alert type="error" showIcon message={error instanceof Error ? error.message : '任职资格保存失败'} />}
    <Form<AssignmentForm>
      layout="vertical"
      initialValues={{ assignmentType: 'DIRECT' }}
      onFinish={(values) => onSave({
        roleId: values.roleId,
        validFrom: new Date(values.validFrom).toISOString(),
        validTo: values.validTo ? new Date(values.validTo).toISOString() : null,
        assignmentType: values.assignmentType,
        reason: values.reason?.trim() || null,
        ticketNo: values.ticketNo?.trim() || null,
        expectedUserAuthVersion,
      })}
    >
      <Form.Item name="roleId" label="Role ID" rules={[{ required: true }]}><Input /></Form.Item>
      <Form.Item name="assignmentType" label="资格类型" rules={[{ required: true }]}>
        <Select options={[
          {value: 'DIRECT', label: 'DIRECT'},
          {value: 'TEMPORARY', label: 'TEMPORARY'},
          {value: 'EMERGENCY', label: 'EMERGENCY'},
        ]} />
      </Form.Item>
      <Form.Item name="validFrom" label="生效时间" rules={[{ required: true }]}><Input type="datetime-local" /></Form.Item>
      <Form.Item name="validTo" label="失效时间"><Input type="datetime-local" /></Form.Item>
      <Form.Item name="reason" label="原因"><Input.TextArea rows={2} /></Form.Item>
      <Form.Item name="ticketNo" label="外部工单号"><Input /></Form.Item>
      <Form.Item label="目标用户授权版本">
        <InputNumber aria-label="目标用户授权版本" value={expectedUserAuthVersion} readOnly style={{ width: '100%' }} />
      </Form.Item>
      <Button type="primary" htmlType="submit" loading={saving}>保存资格</Button>
    </Form>
  </Modal>
)
