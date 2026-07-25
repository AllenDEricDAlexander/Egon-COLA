import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Alert, Button, Card, Descriptions, Input, Modal, Space, Table, Tabs, Typography, message } from 'antd'
import { useEffect, useState } from 'react'
import { useParams } from 'react-router-dom'
import { gatewayApi } from '../../api/gatewayApi'
import type { ReleaseAttempt, ReleaseTarget } from '../../api/types'
import { createLogicalTrace } from '../../api/trace'
import { useCapability } from '../../app/capabilities'
import { JsonPanel } from '../../components/JsonPanel'
import { LoadingBlock, QueryFailure } from '../../components/QueryState'
import { StatusTag } from '../../components/StatusTag'
import { shouldPollRelease } from './releaseState'

export const ReleaseDetailPage = () => {
  const { groupId = '', releaseId = '' } = useParams()
  const queryClient = useQueryClient()
  const canPublish = useCapability('gateway:releases:write')
  const canRollback = useCapability('gateway:releases:write')
  const [visible, setVisible] = useState(document.visibilityState === 'visible')
  const [rollbackOpen, setRollbackOpen] = useState(false)
  const [reason, setReason] = useState('')
  useEffect(() => {
    const listener = () => setVisible(document.visibilityState === 'visible')
    document.addEventListener('visibilitychange', listener)
    return () => document.removeEventListener('visibilitychange', listener)
  }, [])
  const release = useQuery({
    queryKey: ['release', releaseId],
    queryFn: ({ signal }) => gatewayApi.release(releaseId, signal),
    enabled: Boolean(releaseId),
    refetchInterval: (query) =>
      shouldPollRelease(query.state.data, visible) ? 2_000 : false,
  })
  const draft = useQuery({
    queryKey: ['draft', groupId],
    queryFn: ({ signal }) => gatewayApi.draft(groupId, signal),
    enabled: rollbackOpen,
  })
  const retry = useMutation({
    mutationFn: () => gatewayApi.retryRelease(releaseId, createLogicalTrace()),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['release', releaseId] })
      void message.success('已新增 Retry Attempt；原 Target 证据保持不变')
    },
  })
  const rollback = useMutation({
    mutationFn: () =>
      gatewayApi.rollback(
        groupId,
        releaseId,
        draft.data!.revision,
        reason,
        createLogicalTrace(),
      ),
    onSuccess: (created) => {
      setRollbackOpen(false)
      void message.success(`回滚已创建新 Release：${created.id}`)
    },
  })
  if (release.isLoading) return <LoadingBlock />
  if (release.error || !release.data) return <QueryFailure error={release.error} />
  return (
    <section>
      <Typography.Title level={2}>Release {release.data.id}</Typography.Title>
      {release.data.partialApplied && (
        <Alert type="error" showIcon message="该 Release 存在部分生效，不能视为成功。" />
      )}
      <Card className="section-row">
        <Descriptions column={2}>
          <Descriptions.Item label="状态"><StatusTag status={release.data.status} partialApplied={release.data.partialApplied} /></Descriptions.Item>
          <Descriptions.Item label="Change ID">{release.data.changeId ?? '-'}</Descriptions.Item>
          <Descriptions.Item label="Draft Revision">{release.data.draftRevision}</Descriptions.Item>
          <Descriptions.Item label="Rollback Of">{release.data.rollbackOfReleaseId ?? '-'}</Descriptions.Item>
          <Descriptions.Item label="变更原因">{release.data.changeReason}</Descriptions.Item>
        </Descriptions>
        <Space>
          <Button disabled={!canPublish} loading={retry.isPending} onClick={() => retry.mutate()}>
            使用原 Release 内容和原 Target 重试
          </Button>
          <Button danger disabled={!canRollback} onClick={() => setRollbackOpen(true)}>
            创建回滚 Release
          </Button>
        </Space>
      </Card>
      <Tabs
        items={(release.data.attempts ?? []).map((attempt: ReleaseAttempt) => ({
          key: String(attempt.attemptNo),
          label: `Attempt ${attempt.attemptNo}`,
          children: (
            <Card>
              <Descriptions>
                <Descriptions.Item label="状态"><StatusTag status={attempt.status} /></Descriptions.Item>
                <Descriptions.Item label="开始">{attempt.startedAt}</Descriptions.Item>
                <Descriptions.Item label="结束">{attempt.completedAt ?? '-'}</Descriptions.Item>
                <Descriptions.Item label="错误">{attempt.errorCode ?? '-'}</Descriptions.Item>
              </Descriptions>
              <Table<ReleaseTarget>
                rowKey={(target) => `${target.instanceId}:${target.leaseId}`}
                dataSource={attempt.targets}
                scroll={{ x: 1050 }}
                columns={[
                  { title: 'Engine Instance', dataIndex: 'instanceId' },
                  { title: 'Lease', dataIndex: 'leaseId' },
                  { title: 'ACK', render: (_, target) => <StatusTag status={target.status} /> },
                  { title: 'Applied Version', dataIndex: 'appliedVersion' },
                  { title: 'Artifact SHA', dataIndex: 'appliedArtifactSha256' },
                  { title: '错误码', dataIndex: 'errorCode' },
                  { title: '最后更新', dataIndex: 'observedAt' },
                ]}
              />
            </Card>
          ),
        }))}
      />
      <JsonPanel title="Validation Report" value={release.data.validationReport} />
      <JsonPanel title="Structured Diff" value={release.data.structuredDiff} />
      <Modal
        title="创建回滚 Release"
        open={rollbackOpen}
        onCancel={() => setRollbackOpen(false)}
        onOk={() => rollback.mutate()}
        okButtonProps={{ danger: true, disabled: !reason.trim() || !draft.data }}
        confirmLoading={rollback.isPending}
      >
        <Alert type="warning" showIcon message="回滚不会修改历史 Release，而会基于目标内容创建新的 Release。" />
        <Input.TextArea className="section-row" rows={4} placeholder="必填变更原因" value={reason} onChange={(event) => setReason(event.target.value)} />
      </Modal>
    </section>
  )
}
