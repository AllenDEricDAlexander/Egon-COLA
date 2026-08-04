import { useQuery } from '@tanstack/react-query'
import { Card, Col, Row, Statistic } from 'antd'
import { useRbac3Session } from '@egon-cola/rbac3-react-sdk'
import { useFeatureApi, useFeatureTenantContext } from '../shared/FeatureApi'
import { overviewApi } from './overview.api'
import { PageState } from '@egon-cola/admin-web-shared'

export const OverviewPage = () => {
  const { status, bootstrap } = useRbac3Session()
  const { effectiveTenantId } = useFeatureTenantContext()
  const api = overviewApi(useFeatureApi())
  const query = useQuery({
    queryKey: ['rbac3', 'overview', effectiveTenantId ?? 'none'],
    queryFn: api.runtime,
    enabled: status === 'READY',
  })
  return (
    <Card title="权限治理概览">
      <PageState loading={query.isPending} error={query.error} empty={!query.data}>
        <Row gutter={16}>
          <Col span={8}><Statistic title="Auth Version" value={bootstrap?.authVersion ?? 0} /></Col>
          <Col span={8}><Statistic title="Session Version" value={bootstrap?.sessionVersion ?? 0} /></Col>
          <Col span={8}><Statistic title="Policy Version" value={bootstrap?.policyVersion ?? 0} /></Col>
        </Row>
      </PageState>
    </Card>
  )
}
