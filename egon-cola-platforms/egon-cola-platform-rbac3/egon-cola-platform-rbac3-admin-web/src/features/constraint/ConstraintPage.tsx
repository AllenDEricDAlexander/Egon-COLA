import { useRbac3Session } from '@egon-cola/rbac3-react-sdk'
import { useQuery } from '@tanstack/react-query'
import { Card, Table, Tabs, Tag, Typography } from 'antd'
import { useFeatureApi, useFeatureTenantContext } from '../shared/FeatureApi'
import { PageState } from '@egon-cola/admin-web-shared'
import { constraintApi, type SodSetView } from './constraint.api'

export interface DsdRoleSelection {
  readonly roleId: string
  readonly applicationId: string
  readonly roleType: string
}

export const validateDsdRoleSelection = (
  roles: readonly DsdRoleSelection[],
): string | null => {
  if (roles.some((role) => role.roleType !== 'ACTIVATION_ROOT')) {
    return 'DSD_ROLE_MUST_BE_ACTIVATION_ROOT'
  }
  if (new Set(roles.map((role) => role.applicationId)).size > 1) {
    return 'DSD_ROLES_MUST_SHARE_APPLICATION'
  }
  return null
}

export const ConstraintPage = () => {
  const { status } = useRbac3Session()
  const { effectiveTenantId } = useFeatureTenantContext()
  const api = constraintApi(useFeatureApi())
  const tenant = effectiveTenantId ?? 'none'
  const enabled = status === 'READY'
  const sod = useQuery({ queryKey: ['rbac3', 'sod', tenant], queryFn: api.sodSets, enabled })
  const data = useQuery({ queryKey: ['rbac3', 'data-rules', tenant], queryFn: api.dataRules, enabled })
  const fields = useQuery({ queryKey: ['rbac3', 'field-rules', tenant], queryFn: api.fieldRules, enabled })
  const operations = useQuery({ queryKey: ['rbac3', 'operation-sod', tenant], queryFn: api.operationSodRules, enabled })
  return (
    <Card
      title="授权约束"
      extra={null}
    >
      <Typography.Paragraph type="secondary">
        DSD 只引用同一 APP 的激活根角色，并在会话激活角色集合原子替换时校验；SSD、Prerequisite 与 Cardinality 在分配时校验。
      </Typography.Paragraph>
      <Tabs items={[
        {
          key: 'sod',
          label: 'SSD / DSD',
          children: (
            <PageState loading={sod.isPending} error={sod.error} empty={sod.data?.length === 0}>
              <Table<SodSetView>
                rowKey="setId"
                dataSource={sod.data ?? []}
                pagination={false}
                columns={[
                  { title: '集合编码', dataIndex: 'setCode' },
                  { title: '类型', dataIndex: 'constraintType', render: (value: string) => <Tag>{value}</Tag> },
                  { title: 'APP', dataIndex: 'applicationId' },
                  { title: '最大同时激活', dataIndex: 'maximumActiveRoles' },
                  { title: '激活根角色', dataIndex: 'roleIds', render: (ids: readonly string[]) => ids.join(', ') },
                  { title: '状态', dataIndex: 'status' },
                ]}
              />
            </PageState>
          ),
        },
        {
          key: 'data', label: `数据规则 (${data.data?.length ?? 0})`,
          children: <PageState loading={data.isPending} error={data.error} empty={data.data?.length === 0}><Typography.Text>类型化数据范围由服务端执行。</Typography.Text></PageState>,
        },
        {
          key: 'field', label: `字段规则 (${fields.data?.length ?? 0})`,
          children: <PageState loading={fields.isPending} error={fields.error} empty={fields.data?.length === 0}><Typography.Text>字段访问默认拒绝，遮罩值由服务端产生。</Typography.Text></PageState>,
        },
        {
          key: 'operation', label: `Operation SOD (${operations.data?.length ?? 0})`,
          children: <PageState loading={operations.isPending} error={operations.error} empty={operations.data?.length === 0}><Typography.Text>同一业务对象参与记录由服务端串行化。</Typography.Text></PageState>,
        },
      ]} />
    </Card>
  )
}
