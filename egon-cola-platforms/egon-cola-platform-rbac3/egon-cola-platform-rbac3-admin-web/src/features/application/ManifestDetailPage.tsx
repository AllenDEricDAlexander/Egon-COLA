import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Alert, Button, Card, Descriptions, Form, Input, InputNumber, Modal, Space, Tag, Typography } from 'antd'
import { PermissionGuard, useRbac3Session } from '@egon-cola/rbac3-react-sdk'
import { useState } from 'react'
import { useFeatureApi, useFeatureTenantContext } from '../shared/FeatureApi'
import { PageState } from '@egon-cola/admin-web-shared'
import { applicationApi } from './application.api'

export interface ManifestDetailPageProps {
  readonly manifestId: string
}

interface ActivationForm {
  readonly applicationId: string
  readonly expectedApplicationVersion: number
  readonly expectedCurrentManifestVersion: number
  readonly expectedDefinitionSetId: string
  readonly reason: string
}

export const ManifestDetailPage = ({ manifestId }: ManifestDetailPageProps) => {
  const { status } = useRbac3Session()
  const { effectiveTenantId } = useFeatureTenantContext()
  const api = applicationApi(useFeatureApi())
  const queryClient = useQueryClient()
  const [open, setOpen] = useState(false)
  const query = useQuery({
    queryKey: ['rbac3', 'manifest', effectiveTenantId ?? 'none', manifestId],
    queryFn: () => api.manifest(manifestId),
    enabled: status === 'READY',
  })
  const validation = useQuery({
    queryKey: ['rbac3', 'manifest-validation', effectiveTenantId ?? 'none', manifestId],
    queryFn: () => api.validation(manifestId),
    enabled: status === 'READY',
  })
  const impact = useQuery({
    queryKey: ['rbac3', 'manifest-impact', effectiveTenantId ?? 'none', manifestId],
    queryFn: () => api.impact(manifestId),
    enabled: status === 'READY',
  })
  const activation = useMutation({
    mutationFn: (values: ActivationForm) => api.activate(manifestId, {
      ...values,
      idempotencyKey: crypto.randomUUID(),
    }),
    onSuccess: async () => {
      setOpen(false)
      await queryClient.invalidateQueries({ queryKey: ['rbac3', 'manifest'] })
    },
  })
  return (
    <Card
      title="Manifest 详情"
      extra={(
        <PermissionGuard permission="system:resource-manifest:activate">
          <Button type="primary" onClick={() => setOpen(true)}>激活 Manifest</Button>
        </PermissionGuard>
      )}
    >
      <Typography.Paragraph type="secondary">
        Checksum、构建版本与 Manifest 版本共同构成不可变提交身份；冲突按稳定错误码处理。
      </Typography.Paragraph>
      <PageState
        loading={query.isPending || validation.isPending || impact.isPending}
        error={query.error ?? validation.error ?? impact.error ?? activation.error}
        empty={!query.data}
      >
        {query.data && (
          <Space orientation="vertical" size="middle" style={{ width: '100%' }}>
            <Descriptions bordered column={2}>
              <Descriptions.Item label="Manifest ID">{query.data.manifestId}</Descriptions.Item>
              <Descriptions.Item label="状态"><Tag>{query.data.status}</Tag></Descriptions.Item>
              <Descriptions.Item label="Checksum">{query.data.checksum}</Descriptions.Item>
              <Descriptions.Item label="Manifest Version">{query.data.manifestVersion}</Descriptions.Item>
              <Descriptions.Item label="校验结果">
                <Tag color={validation.data?.valid ? 'green' : 'red'}>
                  {validation.data?.valid ? 'VALID' : 'INVALID'}
                </Tag>
              </Descriptions.Item>
              <Descriptions.Item label="受影响角色">{impact.data?.affectedRoleCount ?? 0}</Descriptions.Item>
              <Descriptions.Item label="新增 / 变更 / 失效" span={2}>
                {impact.data ? `${impact.data.resourcesAdded} / ${impact.data.resourcesChanged} / ${impact.data.resourcesStale}` : '-'}
              </Descriptions.Item>
            </Descriptions>
            {(validation.data?.errors.length ?? 0) > 0 && (
              <Alert type="error" message="校验错误" description={validation.data?.errors.join('；')} showIcon />
            )}
            {(impact.data?.conflicts.length ?? 0) > 0 && (
              <Alert type="warning" message="影响冲突" description={impact.data?.conflicts.join('；')} showIcon />
            )}
          </Space>
        )}
      </PageState>
      <Modal title="激活不可变 Manifest" open={open} footer={null} onCancel={() => setOpen(false)} destroyOnHidden>
        <Form<ActivationForm>
          layout="vertical"
          initialValues={{
            applicationId: query.data?.applicationId,
            expectedCurrentManifestVersion: query.data?.manifestVersion ?? 0,
          }}
          onFinish={(values) => activation.mutate(values)}
        >
          <Form.Item name="applicationId" label="Application ID" rules={[{ required: true }]}><Input /></Form.Item>
          <Form.Item name="expectedApplicationVersion" label="Expected Application Version" rules={[{ required: true }]}><InputNumber min={0} precision={0} style={{ width: '100%' }} /></Form.Item>
          <Form.Item name="expectedCurrentManifestVersion" label="Expected Current Manifest Version" rules={[{ required: true }]}><InputNumber min={0} precision={0} style={{ width: '100%' }} /></Form.Item>
          <Form.Item name="expectedDefinitionSetId" label="Expected Definition Set ID" rules={[{ required: true }]}><Input /></Form.Item>
          <Form.Item name="reason" label="变更原因" rules={[{ required: true }]}><Input.TextArea rows={3} /></Form.Item>
          <Button type="primary" htmlType="submit" loading={activation.isPending}>确认激活</Button>
        </Form>
      </Modal>
    </Card>
  )
}
