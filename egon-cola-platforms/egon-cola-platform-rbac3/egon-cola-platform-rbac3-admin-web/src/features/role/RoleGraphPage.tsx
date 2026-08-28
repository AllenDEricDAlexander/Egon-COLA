import {useMutation, useQueries, useQuery, useQueryClient} from '@tanstack/react-query'
import {Alert, Button, Card, Descriptions, Drawer, Form, Input, InputNumber, Modal, Select, Space, Tag, Typography} from 'antd'
import {useState} from 'react'
import {PermissionGuard, useRbac3Authorization} from '@egon-cola/rbac3-react-sdk'
import {PageState} from '@egon-cola/admin-web-shared'
import {useFeatureApi, useFeatureTenantContext} from '../shared/FeatureApi'
import {roleApi, type RoleImpactView, type RoleView} from './role.api'

export interface RoleGraphPageProps {
  readonly applicationId?: string
}

const MAX_RENDERED_ROLES = 200

type RoleFormValues = {
  applicationId?: string
  roleCode?: string
  roleName: string
  roleType: string
  riskLevel: string
  privileged: boolean
  status: string
  landingRouteId?: string | null
  landingPriority: number
  maximumAssignmentDays?: number | null
}

type InheritanceFormValues = {
  applicationId: string
  juniorRoleId: string
  expectedRoleVersion: number
}

const roleTypes = [
  {value: 'ACTIVATION_ROOT', label: '激活根角色'},
  {value: 'BUSINESS', label: '业务角色'},
]

const riskLevels = [
  {value: 'LOW', label: '低风险'},
  {value: 'MEDIUM', label: '中风险'},
  {value: 'HIGH', label: '高风险'},
]

const statusOptions = [
  {value: 'ACTIVE', label: '启用'},
  {value: 'DISABLED', label: '停用'},
]

export const RoleGraphPage = ({applicationId}: RoleGraphPageProps) => {
  const {status} = useRbac3Authorization()
  const {effectiveTenantId} = useFeatureTenantContext()
  const api = roleApi(useFeatureApi())
  const queryClient = useQueryClient()
  const [editingRole, setEditingRole] = useState<RoleView | null>(null)
  const [roleModalOpen, setRoleModalOpen] = useState(false)
  const [inheritanceRole, setInheritanceRole] = useState<RoleView | null>(null)
  const [impactRole, setImpactRole] = useState<RoleView | null>(null)
  const [roleForm] = Form.useForm<RoleFormValues>()
  const [inheritanceForm] = Form.useForm<InheritanceFormValues>()
  const tenant = effectiveTenantId ?? 'none'
  const rolesKey = ['rbac3', 'roles', tenant, applicationId ?? 'all']

  const roles = useQuery({
    queryKey: rolesKey,
    queryFn: () => api.roles(applicationId),
    enabled: status === 'READY',
  })
  const visibleRoles = (roles.data ?? []).slice(0, MAX_RENDERED_ROLES)
  const impacts = useQueries({
    queries: visibleRoles.map((role) => ({
      queryKey: ['rbac3', 'role-impact', tenant, role.roleId],
      queryFn: () => api.impact(role.roleId),
      enabled: status === 'READY',
    })),
  })
  const impactByRole = new Map<string, RoleImpactView>()
  impacts.forEach((impact) => {
    if (impact.data) impactByRole.set(impact.data.roleId, impact.data)
  })

  const refreshRoles = async () => {
    await Promise.all([
      queryClient.invalidateQueries({queryKey: ['rbac3', 'roles']}),
      queryClient.invalidateQueries({queryKey: ['rbac3', 'role-impact']}),
    ])
  }

  const saveRole = useMutation({
    mutationFn: (values: RoleFormValues) => editingRole
      ? api.update(editingRole.roleId, {
        roleName: values.roleName.trim(),
        status: values.status,
        landingRouteId: values.landingRouteId?.trim() || null,
        landingPriority: values.landingPriority,
        maximumAssignmentDays: values.maximumAssignmentDays ?? null,
        expectedRoleVersion: editingRole.version,
      })
      : api.create({
        applicationId: values.applicationId!.trim(),
        roleCode: values.roleCode!.trim(),
        roleName: values.roleName.trim(),
        roleType: values.roleType,
        riskLevel: values.riskLevel,
        privileged: values.privileged,
        landingRouteId: values.landingRouteId?.trim() || null,
        landingPriority: values.landingPriority,
        maximumAssignmentDays: values.maximumAssignmentDays ?? null,
      }),
    onSuccess: async () => {
      setRoleModalOpen(false)
      setEditingRole(null)
      await refreshRoles()
    },
  })

  const addInheritance = useMutation({
    mutationFn: (values: InheritanceFormValues) => {
      if (!inheritanceRole) throw new Error('ROLE_REQUIRED')
      return api.addInheritance(inheritanceRole.roleId, {
        applicationId: values.applicationId.trim(),
        juniorRoleId: values.juniorRoleId.trim(),
        expectedRoleVersion: values.expectedRoleVersion,
      })
    },
    onSuccess: async () => {
      setInheritanceRole(null)
      await refreshRoles()
    },
  })

  const removeInheritance = useMutation({
    mutationFn: (values: InheritanceFormValues) => {
      if (!inheritanceRole) throw new Error('ROLE_REQUIRED')
      return api.removeInheritance(inheritanceRole.roleId, values.juniorRoleId.trim(), {
        applicationId: values.applicationId.trim(),
        expectedRoleVersion: values.expectedRoleVersion,
      })
    },
    onSuccess: async () => {
      setInheritanceRole(null)
      await refreshRoles()
    },
  })

  const openCreate = () => {
    setEditingRole(null)
    roleForm.resetFields()
    roleForm.setFieldsValue({
      applicationId: applicationId ?? '',
      roleType: 'BUSINESS',
      riskLevel: 'LOW',
      privileged: false,
      status: 'ACTIVE',
      landingPriority: 0,
      maximumAssignmentDays: null,
    })
    setRoleModalOpen(true)
  }

  const openEdit = (role: RoleView) => {
    setEditingRole(role)
    roleForm.setFieldsValue({
      roleName: role.roleName,
      roleType: role.roleType,
      riskLevel: role.riskLevel,
      privileged: role.privileged,
      status: role.status,
      landingPriority: 0,
      maximumAssignmentDays: null,
    })
    setRoleModalOpen(true)
  }

  const openInheritance = (role: RoleView) => {
    setInheritanceRole(role)
    inheritanceForm.setFieldsValue({
      applicationId: role.applicationId,
      juniorRoleId: '',
      expectedRoleVersion: role.version,
    })
  }

  const submitRole = async () => {
    try {
      saveRole.mutate(await roleForm.validateFields())
    } catch {
      return
    }
  }

  const submitInheritance = async () => {
    try {
      addInheritance.mutate(await inheritanceForm.validateFields())
    } catch {
      return
    }
  }

  const submitRemoveInheritance = async () => {
    try {
      removeInheritance.mutate(await inheritanceForm.validateFields())
    } catch {
      return
    }
  }

  return (
    <Card
      title="角色图谱"
      extra={(
        <PermissionGuard permission="system:role:create">
          <Button type="primary" onClick={openCreate}>新建角色</Button>
        </PermissionGuard>
      )}
    >
      <Typography.Paragraph type="secondary">
        图谱按 APP 隔离；Root 是可激活角色，Child 只随根角色继承生效。所有变更均携带角色版本，避免覆盖他人的并发修改。
      </Typography.Paragraph>
      {(saveRole.error || addInheritance.error || removeInheritance.error) && (
        <Alert
          type="error"
          showIcon
          closable
          message={[
            saveRole.error,
            addInheritance.error,
            removeInheritance.error,
          ].find(Boolean) instanceof Error
            ? ([saveRole.error, addInheritance.error, removeInheritance.error].find(Boolean) as Error).message
            : '角色变更失败，请重试或刷新版本'}
          style={{marginBottom: 16}}
        />
      )}
      {(roles.data?.length ?? 0) > MAX_RENDERED_ROLES && (
        <Typography.Paragraph type="warning">
          节点超过渲染上限，仅显示前 {MAX_RENDERED_ROLES} 个角色摘要。
        </Typography.Paragraph>
      )}
      <PageState loading={roles.isPending} error={roles.error} empty={roles.data?.length === 0}>
        {visibleRoles.map((role) => {
          const impact = impactByRole.get(role.roleId)
          const ambiguous = (impact?.activationRoots.length ?? 0) > 1
            || (impact?.conflicts.length ?? 0) > 0
          return (
            <Card key={role.roleId} size="small" style={{marginBottom: 12}}>
              <Space direction="vertical" size="small" style={{width: '100%'}}>
                <Space wrap>
                  <Typography.Text strong>{role.roleName}</Typography.Text>
                  <Typography.Text code>{role.roleCode}</Typography.Text>
                  <Typography.Text type="secondary">APP {role.applicationId}</Typography.Text>
                  <Tag color={role.roleType === 'ACTIVATION_ROOT' ? 'blue' : 'default'}>
                    {role.roleType === 'ACTIVATION_ROOT' ? 'Root' : 'Child'}
                  </Tag>
                  {role.status !== 'ACTIVE' && <Tag color="red">Disabled</Tag>}
                  {ambiguous && <Tag color="orange">Ambiguous</Tag>}
                  <Tag>{role.riskLevel}</Tag>
                  <Tag>版本 {role.version}</Tag>
                </Space>
                <Space wrap>
                  <Button size="small" onClick={() => setImpactRole(role)}>影响分析</Button>
                  <PermissionGuard permission="system:role:update">
                    <Button size="small" onClick={() => openEdit(role)}>编辑</Button>
                  </PermissionGuard>
                  <PermissionGuard permission="system:role-inheritance:manage">
                    <Button size="small" onClick={() => openInheritance(role)}>继承管理</Button>
                  </PermissionGuard>
                </Space>
              </Space>
            </Card>
          )
        })}
      </PageState>

      <Modal
        open={roleModalOpen}
        title={editingRole ? '编辑角色' : '新建角色'}
        onCancel={() => { setRoleModalOpen(false); setEditingRole(null) }}
        onOk={() => { void submitRole() }}
        okText="保存"
        confirmLoading={saveRole.isPending}
        destroyOnHidden
      >
        <Form form={roleForm} layout="vertical">
          {!editingRole && (
            <>
              <Form.Item name="applicationId" label="应用 ID" rules={[{required: true}]}>
                <Input />
              </Form.Item>
              <Form.Item name="roleCode" label="角色编码" rules={[{required: true}]}>
                <Input />
              </Form.Item>
              <Form.Item name="roleType" label="角色类型" rules={[{required: true}]}>
                <Select options={roleTypes} />
              </Form.Item>
              <Form.Item name="riskLevel" label="风险等级" rules={[{required: true}]}>
                <Select options={riskLevels} />
              </Form.Item>
              <Form.Item name="privileged" label="特权角色" rules={[{required: true}]}>
                <Select options={[{value: false, label: '普通'}, {value: true, label: '特权'}]} />
              </Form.Item>
            </>
          )}
          <Form.Item name="roleName" label="角色名称" rules={[{required: true}]}>
            <Input />
          </Form.Item>
          {editingRole && (
            <Form.Item name="status" label="状态" rules={[{required: true}]}>
              <Select options={statusOptions} />
            </Form.Item>
          )}
          <Form.Item name="landingRouteId" label="落地路由 ID">
            <Input allowClear />
          </Form.Item>
          <Form.Item name="landingPriority" label="落地优先级" rules={[{required: true, type: 'number', min: 0}]}>
            <InputNumber min={0} precision={0} style={{width: '100%'}} />
          </Form.Item>
          <Form.Item name="maximumAssignmentDays" label="最长任职天数">
            <InputNumber min={0} precision={0} style={{width: '100%'}} />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        open={inheritanceRole !== null}
        title={inheritanceRole ? `继承管理：${inheritanceRole.roleName}` : ''}
        onCancel={() => setInheritanceRole(null)}
        onOk={() => { void submitInheritance() }}
        okText="添加继承"
        confirmLoading={addInheritance.isPending}
        destroyOnHidden
        footer={(_, {OkBtn, CancelBtn}) => (
          <Space>
            <Button danger loading={removeInheritance.isPending} onClick={() => { void submitRemoveInheritance() }}>
              删除继承
            </Button>
            <CancelBtn />
            <OkBtn />
          </Space>
        )}
      >
        <Form form={inheritanceForm} layout="vertical">
          <Form.Item name="applicationId" label="应用 ID" rules={[{required: true}]}>
            <Input />
          </Form.Item>
          <Form.Item name="juniorRoleId" label="下级角色 ID" rules={[{required: true}]}>
            <Input />
          </Form.Item>
          <Form.Item name="expectedRoleVersion" label="期望角色版本" rules={[{required: true, type: 'number', min: 0}]}>
            <InputNumber min={0} precision={0} style={{width: '100%'}} />
          </Form.Item>
        </Form>
      </Modal>

      <Drawer
        open={impactRole !== null}
        title={impactRole ? `影响分析：${impactRole.roleName}` : ''}
        onClose={() => setImpactRole(null)}
        width={560}
      >
        {impactRole && (() => {
          const impact = impactByRole.get(impactRole.roleId)
          if (!impact) return <Typography.Text type="secondary">影响分析加载中...</Typography.Text>
          return (
            <Descriptions bordered column={1} size="small">
              <Descriptions.Item label="角色族">{impact.roleFamily.join(', ') || '-'}</Descriptions.Item>
              <Descriptions.Item label="激活根">{impact.activationRoots.join(', ') || '-'}</Descriptions.Item>
              <Descriptions.Item label="有效风险">{impact.effectiveFamilyRisk}</Descriptions.Item>
              <Descriptions.Item label="权限数量">{impact.permissionCount}</Descriptions.Item>
              <Descriptions.Item label="冲突">{impact.conflicts.join(', ') || '无'}</Descriptions.Item>
            </Descriptions>
          )
        })()}
      </Drawer>
    </Card>
  )
}
