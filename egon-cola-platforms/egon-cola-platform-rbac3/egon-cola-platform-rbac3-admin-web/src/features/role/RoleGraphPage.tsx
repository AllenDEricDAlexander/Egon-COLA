import { useQueries, useQuery } from '@tanstack/react-query'
import { Card, List, Space, Tag, Typography } from 'antd'
import { useRbac3Session } from '@egon-cola/rbac3-react-sdk'
import { useFeatureApi, useFeatureTenantContext } from '../shared/FeatureApi'
import { PageState } from '../shared/PageState'
import { roleApi, type RoleImpactView } from './role.api'

export interface RoleGraphPageProps {
  readonly applicationId?: string
}

const MAX_RENDERED_ROLES = 200

export const RoleGraphPage = ({ applicationId }: RoleGraphPageProps) => {
  const { status } = useRbac3Session()
  const { effectiveTenantId } = useFeatureTenantContext()
  const api = roleApi(useFeatureApi())
  const roles = useQuery({
    queryKey: ['rbac3', 'roles', effectiveTenantId ?? 'none', applicationId ?? 'all'],
    queryFn: () => api.roles(applicationId),
    enabled: status === 'READY',
  })
  const visibleRoles = (roles.data ?? []).slice(0, MAX_RENDERED_ROLES)
  const impacts = useQueries({
    queries: visibleRoles.map((role) => ({
      queryKey: ['rbac3', 'role-impact', effectiveTenantId ?? 'none', role.roleId],
      queryFn: () => api.impact(role.roleId),
      enabled: status === 'READY',
    })),
  })
  const impactByRole = new Map<string, RoleImpactView>()
  impacts.forEach((impact) => {
    if (impact.data) impactByRole.set(impact.data.roleId, impact.data)
  })
  return (
    <Card title="角色图谱">
      <Typography.Paragraph type="secondary">
        图谱按 APP 隔离；Root 是可激活角色，Child 只随根角色继承生效。
      </Typography.Paragraph>
      {(roles.data?.length ?? 0) > MAX_RENDERED_ROLES && (
        <Typography.Paragraph type="warning">
          节点超过渲染上限，仅显示前 {MAX_RENDERED_ROLES} 个角色摘要。
        </Typography.Paragraph>
      )}
      <PageState loading={roles.isPending} error={roles.error} empty={roles.data?.length === 0}>
        <List
          dataSource={visibleRoles}
          renderItem={(role) => {
            const impact = impactByRole.get(role.roleId)
            const ambiguous = (impact?.activationRoots.length ?? 0) > 1
              || (impact?.conflicts.length ?? 0) > 0
            return (
              <List.Item>
                <List.Item.Meta title={role.roleName} description={`${role.roleCode} · APP ${role.applicationId}`} />
                <Space wrap>
                  <Tag color={role.roleType === 'ACTIVATION_ROOT' ? 'blue' : 'default'}>
                    {role.roleType === 'ACTIVATION_ROOT' ? 'Root' : 'Child'}
                  </Tag>
                  {role.status !== 'ACTIVE' && <Tag color="red">Disabled</Tag>}
                  {ambiguous && <Tag color="orange">Ambiguous</Tag>}
                  <Tag>{role.riskLevel}</Tag>
                </Space>
              </List.Item>
            )
          }}
        />
      </PageState>
    </Card>
  )
}
