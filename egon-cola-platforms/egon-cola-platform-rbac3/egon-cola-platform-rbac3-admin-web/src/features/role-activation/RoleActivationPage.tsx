import { useActiveRoles, usePermission, useRbac3Session } from '@egon-cola/rbac3-react-sdk'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { Alert, Button, Card, Descriptions, Space, Tag, Typography } from 'antd'
import { useMemo, useState } from 'react'
import { useFeatureApi, useFeatureTenantContext } from '../shared/FeatureApi'
import { PageState } from '../shared/PageState'
import { RoleActivationSelector, findDsdConflict } from './RoleActivationSelector'
import { roleActivationApi } from './roleActivation.api'

export const RoleActivationPage = () => {
  const session = useRbac3Session()
  const canActivateAfterBootstrap = usePermission('system:role-activation:use')
  const { replaceActiveRoles } = useActiveRoles()
  const { effectiveTenantId } = useFeatureTenantContext()
  const api = roleActivationApi(useFeatureApi())
  const queryClient = useQueryClient()
  const enabled = ['READY', 'ACTIVATION_REQUIRED'].includes(session.status)
  const candidates = useQuery({
    queryKey: ['rbac3', 'activation-candidates', effectiveTenantId ?? 'none', session.bootstrap?.authVersion ?? 'none'],
    queryFn: api.candidates,
    enabled,
  })
  const current = useQuery({
    queryKey: ['rbac3', 'active-roles', effectiveTenantId ?? 'none'],
    queryFn: api.current,
    enabled,
  })
  const currentIds = useMemo(
    () => current.data?.activeRoles.flatMap((application) => application.rootRoleIds) ?? [],
    [current.data],
  )
  const [selectionOverride, setSelectionOverride] = useState<readonly string[] | null>(null)
  const [submitError, setSubmitError] = useState<unknown>(null)
  const selectedRoleIds = selectionOverride ?? currentIds
  const selectionReady = current.data !== undefined
  const conflict = candidates.data ? findDsdConflict(candidates.data, selectedRoleIds) : null
  const submit = async () => {
    if (!current.data || conflict) return
    setSubmitError(null)
    try {
      await replaceActiveRoles({ roleIds: selectedRoleIds, expectedSessionVersion: current.data.sessionVersion })
      setSelectionOverride(null)
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ['rbac3', 'active-roles'] }),
        queryClient.invalidateQueries({ queryKey: ['rbac3', 'activation-candidates'] }),
      ])
    } catch (error) {
      setSelectionOverride(null)
      setSubmitError(error)
    }
  }
  return (
    <Card title="激活当前会话角色">
      <Typography.Paragraph type="secondary">
        选择会话需要的一个或多个激活根角色。提交的是完整 Root Set；服务端按 Session Version 原子替换并返回权威集合。
      </Typography.Paragraph>
      {current.data && (
        <Descriptions bordered size="small" column={3} style={{ marginBottom: 16 }}>
          <Descriptions.Item label="Session Version">{current.data.sessionVersion}</Descriptions.Item>
          <Descriptions.Item label="Auth Version">{current.data.authVersion}</Descriptions.Item>
          <Descriptions.Item label="Policy Version">{current.data.policyVersion}</Descriptions.Item>
        </Descriptions>
      )}
      {(candidates.data?.configurationErrors.length ?? 0) > 0 && (
        <Alert
          type="error"
          showIcon
          message="角色配置无法安全归一"
          description={candidates.data?.configurationErrors.map((error) => `${error.reasonCode}: ${error.evidenceIds.join(', ')}`).join('；')}
        />
      )}
      <PageState
        loading={candidates.isPending || current.isPending || !selectionReady}
        error={candidates.error ?? current.error ?? submitError}
        empty={candidates.data?.applications.length === 0}
        emptyDescription="当前没有可安全激活的候选根角色"
      >
        {candidates.data && selectionReady && (
          <RoleActivationSelector
            candidates={candidates.data}
            selectedRoleIds={selectedRoleIds}
            disabled={session.status === 'REPLACING_ACTIVE_ROLES'}
            onChange={setSelectionOverride}
          />
        )}
      </PageState>
      <Space style={{ marginTop: 16 }}>
        {(session.status === 'ACTIVATION_REQUIRED' || canActivateAfterBootstrap) && (
          <Button
            type="primary"
            disabled={!current.data || Boolean(conflict) || session.status === 'REPLACING_ACTIVE_ROLES'}
            loading={session.status === 'REPLACING_ACTIVE_ROLES'}
            onClick={() => void submit()}
          >
            激活所选角色
          </Button>
        )}
        {current.data?.activeRoles.map((application) => <Tag key={application.applicationCode}>{application.applicationCode}: {application.rootRoleIds.join(', ')}</Tag>)}
      </Space>
    </Card>
  )
}
