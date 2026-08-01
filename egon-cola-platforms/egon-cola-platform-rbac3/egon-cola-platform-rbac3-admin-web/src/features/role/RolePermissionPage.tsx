import { PermissionGuard, useRbac3Session } from '@egon-cola/rbac3-react-sdk'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Button, Card, Descriptions, Form, Input, Modal, Tag } from 'antd'
import { useState } from 'react'
import { useFeatureApi, useFeatureTenantContext } from '../shared/FeatureApi'
import { PageState } from '../shared/PageState'
import { roleApi } from './role.api'

export interface RolePermissionPageProps {
  readonly roleId: string
}

interface PermissionForm {
  readonly applicationId: string
  readonly permissionIds: string
  readonly expectedRoleVersion: number
}

export const RolePermissionPage = ({ roleId }: RolePermissionPageProps) => {
  const { status } = useRbac3Session()
  const { effectiveTenantId } = useFeatureTenantContext()
  const api = roleApi(useFeatureApi())
  const queryClient = useQueryClient()
  const [open, setOpen] = useState(false)
  const queryKey = ['rbac3', 'role-impact', effectiveTenantId ?? 'none', roleId]
  const impact = useQuery({
    queryKey,
    queryFn: () => api.impact(roleId),
    enabled: status === 'READY',
  })
  const mutation = useMutation({
    mutationFn: (form: PermissionForm) => api.bindPermissions(roleId, {
      applicationId: form.applicationId,
      permissionIds: form.permissionIds.split(',').map((value) => value.trim()).filter(Boolean),
      validFrom: new Date().toISOString(),
      validTo: null,
      expectedRoleVersion: form.expectedRoleVersion,
    }),
    onSuccess: async () => {
      setOpen(false)
      await queryClient.invalidateQueries({ queryKey })
    },
  })
  return (
    <Card
      title="角色权限与影响分析"
      extra={(
        <PermissionGuard permission="system:role-permission:manage">
          <Button type="primary" onClick={() => setOpen(true)}>原子替换权限</Button>
        </PermissionGuard>
      )}
    >
      <PageState loading={impact.isPending} error={impact.error ?? mutation.error} empty={!impact.data}>
        {impact.data && (
          <Descriptions bordered column={2}>
            <Descriptions.Item label="Role ID">{impact.data.roleId}</Descriptions.Item>
            <Descriptions.Item label="有效风险"><Tag>{impact.data.effectiveFamilyRisk}</Tag></Descriptions.Item>
            <Descriptions.Item label="权限数">{impact.data.permissionCount}</Descriptions.Item>
            <Descriptions.Item label="角色族规模">{impact.data.roleFamily.length}</Descriptions.Item>
          </Descriptions>
        )}
      </PageState>
      <Modal open={open} title="原子替换权限" footer={null} onCancel={() => setOpen(false)} destroyOnHidden>
        <Form<PermissionForm> layout="vertical" onFinish={(values) => mutation.mutate(values)}>
          <Form.Item name="applicationId" label="Application ID" rules={[{ required: true }]}><Input /></Form.Item>
          <Form.Item name="permissionIds" label="Permission IDs（逗号分隔）" rules={[{ required: true }]}><Input /></Form.Item>
          <Form.Item name="expectedRoleVersion" label="Expected Role Version" rules={[{ required: true }]}><Input type="number" /></Form.Item>
          <Button type="primary" htmlType="submit" loading={mutation.isPending}>保存</Button>
        </Form>
      </Modal>
    </Card>
  )
}
