import { useQuery } from '@tanstack/react-query'
import { Button, Card, Descriptions, Input, Space, Tag } from 'antd'
import { useState } from 'react'
import { useRbac3Session } from '@egon-cola/rbac3-react-sdk'
import { useFeatureApi, useFeatureTenantContext } from '../shared/FeatureApi'
import { PageState } from '@egon-cola/admin-web-shared'
import { directoryApi } from './directory.api'

export interface UserDirectoryPageProps {
  readonly initialUserId?: string
}

export const UserDirectoryPage = ({ initialUserId = '' }: UserDirectoryPageProps) => {
  const [draftUserId, setDraftUserId] = useState(initialUserId)
  const [userId, setUserId] = useState(initialUserId)
  const { status } = useRbac3Session()
  const { effectiveTenantId } = useFeatureTenantContext()
  const api = directoryApi(useFeatureApi())
  const query = useQuery({
    queryKey: ['rbac3', 'directory-user', effectiveTenantId ?? 'none', userId.trim()],
    queryFn: () => api.user(userId.trim()),
    enabled: status === 'READY' && userId.trim().length > 0,
  })
  return (
    <Card title="用户目录">
      <Space.Compact block>
        <Input
          aria-label="用户 ID"
          value={draftUserId}
          onChange={(event) => setDraftUserId(event.target.value)}
        />
        <Button type="primary" onClick={() => setUserId(draftUserId.trim())}>查询</Button>
      </Space.Compact>
      <PageState
        loading={query.isPending && userId.length > 0}
        error={query.error}
        empty={!query.data}
        emptyDescription="输入字符串形式的用户 ID 查询"
      >
        {query.data && (
          <Descriptions bordered column={2} style={{ marginTop: 16 }}>
            <Descriptions.Item label="User ID">{query.data.userId}</Descriptions.Item>
            <Descriptions.Item label="用户名">{query.data.username}</Descriptions.Item>
            <Descriptions.Item label="显示名">{query.data.displayName}</Descriptions.Item>
            <Descriptions.Item label="状态"><Tag>{query.data.status}</Tag></Descriptions.Item>
            <Descriptions.Item label="Auth Version">{query.data.authVersion}</Descriptions.Item>
            <Descriptions.Item label="目录快照版本">{query.data.directorySnapshotVersion}</Descriptions.Item>
          </Descriptions>
        )}
      </PageState>
    </Card>
  )
}
