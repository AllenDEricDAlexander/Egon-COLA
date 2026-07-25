import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Alert, Button, Card, Form, Input, Modal, Space, Steps, Table, Typography, message } from 'antd'
import { useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { useCapability } from '../../app/capabilities'
import { createLogicalTrace } from '../../api/trace'
import { gatewayApi } from '../../api/gatewayApi'
import type { GatewayRelease } from '../../api/types'
import { JsonPanel } from '../../components/JsonPanel'
import { LoadingBlock, QueryFailure } from '../../components/QueryState'
import { StatusTag } from '../../components/StatusTag'

export const ReleasesPage = () => {
  const { groupId = '' } = useParams()
  const queryClient = useQueryClient()
  const canPublish = useCapability('gateway.release.publish')
  const [open, setOpen] = useState(false)
  const [reason, setReason] = useState('')
  const [trace] = useState(() => createLogicalTrace())
  const draft = useQuery({
    queryKey: ['draft', groupId],
    queryFn: ({ signal }) => gatewayApi.draft(groupId, signal),
    enabled: Boolean(groupId),
  })
  const releases = useQuery({
    queryKey: ['releases', groupId],
    queryFn: ({ signal }) => gatewayApi.releases(groupId, signal),
    enabled: Boolean(groupId),
  })
  const validation = useQuery({
    queryKey: ['release-validation', groupId, draft.data?.revision],
    queryFn: () => gatewayApi.validateDraft(groupId, trace),
    enabled: open && draft.data !== undefined,
  })
  const diff = useQuery({
    queryKey: ['draft-diff', groupId],
    queryFn: ({ signal }) => gatewayApi.draftDiff(groupId, signal),
    enabled: open,
  })
  const publish = useMutation({
    mutationFn: () =>
      gatewayApi.publish(groupId, draft.data!.revision, reason, trace),
    onSuccess: async () => {
      setOpen(false)
      setReason('')
      await queryClient.invalidateQueries({ queryKey: ['releases', groupId] })
      void message.success('Release 已创建，正在等待逐节点 ACK')
    },
  })
  if (draft.isLoading || releases.isLoading) return <LoadingBlock />
  if (draft.error || releases.error) return <QueryFailure error={draft.error ?? releases.error} />
  const valid = validation.data?.valid === true
  return (
    <section>
      <Space className="page-title" align="center">
        <Typography.Title level={2}>发布工作台</Typography.Title>
        <Button type="primary" disabled={!canPublish} onClick={() => setOpen(true)}>
          校验并发布
        </Button>
      </Space>
      <Table<GatewayRelease>
        rowKey="id"
        dataSource={releases.data ?? []}
        scroll={{ x: 1000 }}
        columns={[
          { title: 'Release ID', render: (_, row) => <Link to={`/gateway-groups/${groupId}/releases/${row.id}`}>{row.id}</Link> },
          { title: 'Draft Revision', dataIndex: 'draftRevision' },
          { title: '状态', render: (_, row) => <StatusTag status={row.status} partialApplied={row.partialApplied} /> },
          { title: 'Change ID', dataIndex: 'changeId' },
          { title: '变更原因', dataIndex: 'changeReason' },
          { title: '创建时间', dataIndex: 'createdAt' },
        ]}
      />
      <Modal
        title="发布确认"
        open={open}
        width={760}
        onCancel={() => setOpen(false)}
        onOk={() => publish.mutate()}
        okButtonProps={{ disabled: !valid || !reason.trim() }}
        confirmLoading={publish.isPending}
      >
        <Steps
          current={validation.isLoading || diff.isLoading ? 0 : valid ? 2 : 1}
          items={[{ title: '完整校验' }, { title: '查看错误/警告' }, { title: '确认 Diff' }]}
        />
        {validation.error && <QueryFailure error={validation.error} />}
        {validation.data && (
          <Card title={validation.data.valid ? '校验通过' : '校验未通过'} className="section-row">
            {validation.data.errors.map((issue) => <Alert key={`${issue.path}:${issue.code}`} type="error" showIcon message={`${issue.path} · ${issue.code}`} description={issue.message} />)}
            {validation.data.warnings.map((issue) => <Alert key={`${issue.path}:${issue.code}`} type="warning" showIcon message={`${issue.path} · ${issue.code}`} description={issue.message} />)}
          </Card>
        )}
        {diff.data && <JsonPanel title="与基线的结构化 Diff" value={diff.data} />}
        <Form.Item label="变更说明" required className="section-row">
          <Input.TextArea value={reason} onChange={(event) => setReason(event.target.value)} rows={3} />
        </Form.Item>
      </Modal>
    </section>
  )
}
