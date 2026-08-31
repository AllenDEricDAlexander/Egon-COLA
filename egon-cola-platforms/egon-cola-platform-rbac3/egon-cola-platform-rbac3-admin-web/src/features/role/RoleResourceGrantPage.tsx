import {PermissionGuard, useRbac3Authorization} from '@egon-cola/rbac3-react-sdk'
import {useMutation, useQuery, useQueryClient} from '@tanstack/react-query'
import {Alert, Button, Card, Space, Tag, Tree, Typography} from 'antd'
import type {DataNode, TreeProps} from 'antd/es/tree'
import {useMemo, useState} from 'react'
import {PageState} from '@egon-cola/admin-web-shared'
import {useFeatureApi, useFeatureTenantContext} from '../shared/FeatureApi'
import {roleApi, type RoleResourceGrantNode} from './role.api'

export interface RoleResourceGrantPageProps {
  readonly roleId: string
}

export const RoleResourceGrantPage = ({roleId}: RoleResourceGrantPageProps) => {
  const {status} = useRbac3Authorization()
  const {effectiveTenantId} = useFeatureTenantContext()
  const api = roleApi(useFeatureApi())
  const queryClient = useQueryClient()
  const queryKey = ['rbac3', 'role-resources', effectiveTenantId ?? 'none', roleId]
  const resources = useQuery({
    queryKey,
    queryFn: () => api.resources(roleId),
    enabled: status === 'READY',
  })
  const [selected, setSelected] = useState<Set<string>>(new Set())
  const [dirty, setDirty] = useState(false)

  const mutation = useMutation({
    mutationFn: () => api.replaceResources(roleId, {
      resourceIds: [...selected].sort(),
      validFrom: new Date().toISOString(),
      validTo: null,
      expectedRoleVersion: resources.data?.roleVersion ?? 0,
    }),
    onSuccess: async () => {
      setDirty(false)
      await queryClient.invalidateQueries({queryKey})
      await queryClient.invalidateQueries({queryKey: ['rbac3', 'role-impact', effectiveTenantId ?? 'none', roleId]})
      await queryClient.invalidateQueries({queryKey: ['rbac3', 'about']})
    },
  })

  const dataNodes = useMemo(
    () => (resources.data?.nodes ?? []).map(toTreeNode),
    [resources.data?.nodes],
  )

  const onCheck: TreeProps['onCheck'] = (checkedKeys) => {
    const keys = Array.isArray(checkedKeys) ? checkedKeys : checkedKeys.checked
    setSelected(new Set(keys.map(String)))
    setDirty(true)
  }

  return (
    <Card
      title="角色资源授权"
      extra={(
        <PermissionGuard permission="system:role-resource:manage">
          <Button
            type="primary"
            disabled={!resources.data || !dirty || mutation.isPending}
            loading={mutation.isPending}
            onClick={() => mutation.mutate()}
          >
            保存资源授权
          </Button>
        </PermissionGuard>
      )}
    >
      <PageState loading={resources.isPending} error={resources.error} empty={!resources.data}>
        {resources.data && (
          <Space direction="vertical" size="middle" style={{width: '100%'}}>
            <Typography.Paragraph type="secondary">
              角色直接绑定菜单/页面、按钮或独立接口；页面和按钮关联的接口由服务端按并集自动获得。
            </Typography.Paragraph>
            {mutation.error && <Alert type="error" showIcon message={mutation.error instanceof Error ? mutation.error.message : '保存资源授权失败'} />}
            <Space wrap>
              <Tag>页面 {resources.data.summary.menuPageCount}</Tag>
              <Tag>按钮 {resources.data.summary.actionCount}</Tag>
              <Tag>接口 {resources.data.summary.apiCount}</Tag>
              <Tag>已派生接口 {resources.data.derivedApiResourceIds.length}</Tag>
            </Space>
            <Tree
              checkable
              selectable={false}
              defaultExpandAll
              checkedKeys={dirty ? [...selected] : [...(resources.data?.directResourceIds ?? [])]}
              treeData={dataNodes}
              onCheck={onCheck}
            />
          </Space>
        )}
      </PageState>
    </Card>
  )
}

const toTreeNode = (node: RoleResourceGrantNode): DataNode => ({
  key: node.resourceId,
  title: (
    <Space size="small">
      <span>{node.name}</span>
      <Typography.Text type="secondary">{node.resourceCode}</Typography.Text>
      {node.grantState === 'DERIVED' && <Tag>页面/按钮派生</Tag>}
      {node.grantState === 'INHERITED' && <Tag>继承</Tag>}
      {!node.grantable && <Tag color="orange">{node.disabledReason ?? '不可直接授权'}</Tag>}
      {node.linkedApis.map((api) => <Typography.Text key={api.resourceId} type="secondary">API: {api.name}</Typography.Text>)}
    </Space>
  ),
  disabled: !node.grantable || node.grantState === 'DERIVED' || node.grantState === 'INHERITED',
  children: node.children.map(toTreeNode),
})
