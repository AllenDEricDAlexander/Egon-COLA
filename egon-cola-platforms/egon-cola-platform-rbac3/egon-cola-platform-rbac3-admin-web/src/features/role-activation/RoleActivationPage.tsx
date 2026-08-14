import {useActiveRoles, usePermission, useRbac3Authorization} from '@egon-cola/rbac3-react-sdk'
import {useQuery, useQueryClient} from '@tanstack/react-query'
import {Alert, Button, Card, Descriptions, Input, Modal, Space, Tag, Typography} from 'antd'
import {useMemo, useState} from 'react'
import {useFeatureApi, useFeatureTenantContext} from '../shared/FeatureApi'
import {PageState} from '@egon-cola/admin-web-shared'
import {findDsdConflict, RoleActivationSelector} from './RoleActivationSelector'
import {roleActivationApi} from './roleActivation.api'
import {gatewayAuth} from '../auth/gatewayAuth'

export const RoleActivationPage = () => {
    const authorization = useRbac3Authorization()
  const canActivateAfterBootstrap = usePermission('system:role-activation:use')
  const { replaceActiveRoles } = useActiveRoles()
  const { effectiveTenantId } = useFeatureTenantContext()
  const api = roleActivationApi(useFeatureApi())
  const queryClient = useQueryClient()
    const enabled = ['READY', 'ACTIVATION_REQUIRED'].includes(authorization.status)
  const candidates = useQuery({
      queryKey: ['rbac3', 'activation-candidates', effectiveTenantId ?? 'none', authorization.bootstrap?.authVersion ?? 'none'],
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
  const [stepUpRequest, setStepUpRequest] = useState<{
    readonly roleIds: readonly string[]
      readonly expectedAuthVersion: number
  } | null>(null)
  const [stepUpCredential, setStepUpCredential] = useState('')
  const [stepUpError, setStepUpError] = useState<unknown>(null)
  const [stepUpSubmitting, setStepUpSubmitting] = useState(false)
  const selectedRoleIds = selectionOverride ?? currentIds
  const selectionReady = current.data !== undefined
  const conflict = candidates.data ? findDsdConflict(candidates.data, selectedRoleIds) : null
  const submit = async () => {
    if (!current.data || conflict) return
    setSubmitError(null)
    const request = {
      roleIds: selectedRoleIds,
        expectedAuthVersion: current.data.authVersion,
    }
    try {
      await replaceActiveRoles(request)
      setSelectionOverride(null)
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ['rbac3', 'active-roles'] }),
        queryClient.invalidateQueries({ queryKey: ['rbac3', 'activation-candidates'] }),
      ])
    } catch (error) {
      if (isStepUpRequired(error)) {
        setStepUpRequest(request)
        setStepUpCredential('')
        setStepUpError(null)
        return
      }
      setSelectionOverride(null)
      setSubmitError(error)
    }
  }
  const confirmStepUp = async () => {
    if (stepUpRequest === null || stepUpCredential.length === 0) return
    setStepUpSubmitting(true)
    setStepUpError(null)
    try {
        await gatewayAuth.stepUp(stepUpCredential)
      await replaceActiveRoles(stepUpRequest)
      setStepUpRequest(null)
      setStepUpCredential('')
      setSelectionOverride(null)
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ['rbac3', 'active-roles'] }),
        queryClient.invalidateQueries({ queryKey: ['rbac3', 'activation-candidates'] }),
      ])
    } catch (error) {
      setStepUpCredential('')
      setStepUpError(error)
    } finally {
      setStepUpSubmitting(false)
    }
  }
  const cancelStepUp = () => {
    if (stepUpSubmitting) return
    setStepUpRequest(null)
    setStepUpCredential('')
    setStepUpError(null)
  }
  return (
    <>
        <Card title="激活当前用户角色">
        <Typography.Paragraph type="secondary">
            选择当前用户需要的一个或多个激活根角色。提交的是完整 Root Set；服务端按用户授权版本原子替换并返回权威集合。
        </Typography.Paragraph>
        {current.data && (
          <Descriptions bordered size="small" column={3} style={{ marginBottom: 16 }}>
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
              disabled={authorization.status === 'REPLACING_ACTIVE_ROLES'}
              onChange={setSelectionOverride}
            />
          )}
        </PageState>
        <Space style={{ marginTop: 16 }}>
            {(authorization.status === 'ACTIVATION_REQUIRED' || canActivateAfterBootstrap) && (
            <Button
              type="primary"
              disabled={!current.data || Boolean(conflict) || authorization.status === 'REPLACING_ACTIVE_ROLES'}
              loading={authorization.status === 'REPLACING_ACTIVE_ROLES'}
              onClick={() => void submit()}
            >
              激活所选角色
            </Button>
          )}
          {current.data?.activeRoles.map((application) => <Tag key={application.applicationCode}>{application.applicationCode}: {application.rootRoleIds.join(', ')}</Tag>)}
        </Space>
      </Card>
      <Modal
        open={stepUpRequest !== null}
        title="关键角色需要强认证"
        onCancel={cancelStepUp}
        footer={[
          <Button key="cancel" disabled={stepUpSubmitting} onClick={cancelStepUp}>取消</Button>,
          <Button
            key="confirm"
            type="primary"
            loading={stepUpSubmitting}
            disabled={stepUpCredential.length === 0}
            onClick={() => void confirmStepUp()}
          >
            确认强认证并激活
          </Button>,
        ]}
      >
        <Typography.Paragraph type="secondary">
            所选角色包含高风险或关键权限。请重新输入当前密码，Gateway 会更新当前用户的短期 Access Token，然后继续激活原角色集合。
        </Typography.Paragraph>
        {stepUpError !== null && (
          <Alert
            type="error"
            showIcon
            message="强认证失败"
            description={stepUpError instanceof Error ? stepUpError.message : 'AUTHENTICATION_FAILED'}
            style={{ marginBottom: 16 }}
          />
        )}
        <Input.Password
          aria-label="Current Password"
          autoComplete="current-password"
          value={stepUpCredential}
          disabled={stepUpSubmitting}
          onChange={(event) => setStepUpCredential(event.target.value)}
          onPressEnter={() => void confirmStepUp()}
        />
      </Modal>
    </>
  )
}

const isStepUpRequired = (error: unknown): boolean => typeof error === 'object'
  && error !== null
  && 'code' in error
  && error.code === 'STEP_UP_REQUIRED'
