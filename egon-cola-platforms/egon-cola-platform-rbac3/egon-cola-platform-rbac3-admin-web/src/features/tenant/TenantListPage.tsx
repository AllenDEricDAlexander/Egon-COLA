import { useRbac3Session } from '@egon-cola/rbac3-react-sdk'
import { PermissionGuard } from '@egon-cola/rbac3-react-sdk'
import { Button, Card, Descriptions, Input, Space, Typography } from 'antd'
import { useState } from 'react'
import { useFeatureTenantContext } from '../shared/FeatureApi'

export const TenantListPage = () => {
  const { status } = useRbac3Session()
  const { effectiveTenantId, targetTenantId, setTargetTenantId } = useFeatureTenantContext()
  const [draftTenantId, setDraftTenantId] = useState(targetTenantId ?? '')
  return (
    <Card title="租户上下文">
      <Typography.Paragraph type="secondary">
        当前登录租户
      </Typography.Paragraph>
      <Descriptions column={1} bordered>
        <Descriptions.Item label="Tenant ID">
          {status === 'READY' ? effectiveTenantId : '加载中'}
        </Descriptions.Item>
      </Descriptions>
      <PermissionGuard permission="system:tenant:switch">
        <Space.Compact block style={{ marginTop: 16 }}>
          <Input
            aria-label="目标租户 ID"
            placeholder="输入目标租户 ID"
            value={draftTenantId}
            onChange={(event) => setDraftTenantId(event.target.value)}
          />
          <Button type="primary" onClick={() => setTargetTenantId(draftTenantId)}>切换上下文</Button>
          <Button onClick={() => { setDraftTenantId(''); setTargetTenantId(null) }}>恢复登录租户</Button>
        </Space.Compact>
      </PermissionGuard>
      {targetTenantId && (
        <Typography.Paragraph type="warning" style={{ marginTop: 12 }}>
          当前正在管理目标租户 {effectiveTenantId}；所有查询键和请求头均包含该租户。
        </Typography.Paragraph>
      )}
    </Card>
  )
}
