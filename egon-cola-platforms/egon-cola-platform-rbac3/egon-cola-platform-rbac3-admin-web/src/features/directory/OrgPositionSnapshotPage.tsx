import { PermissionGuard } from '@egon-cola/rbac3-react-sdk'
import { useMutation } from '@tanstack/react-query'
import { Alert, Button, Card, Form, Input, InputNumber, Space, Typography } from 'antd'
import { useState } from 'react'
import { useFeatureApi } from '../shared/FeatureApi'
import { directoryApi } from './directory.api'

interface SnapshotForm {
  readonly providerCode: string
  readonly snapshotVersion: number
  readonly checksum: string
  readonly payloadJson: string
}

export const OrgPositionSnapshotPage = () => {
  const api = directoryApi(useFeatureApi())
  const [result, setResult] = useState<string | null>(null)
  const mutation = useMutation({
    mutationFn: ({ payloadJson, ...values }: SnapshotForm) => api.submitSnapshot({
      ...values,
      generatedAt: new Date().toISOString(),
      payload: JSON.parse(payloadJson) as Readonly<Record<string, unknown>>,
    }),
    onSuccess: (view) => setResult(`${view.outcome} · ${view.snapshotId}`),
  })
  return (
    <Card title="组织与岗位目录快照">
      <Typography.Paragraph type="secondary">
        每次同步创建不可变快照版本；平台只激活完整且校验通过的新版本，不在原版本上修改事实。
      </Typography.Paragraph>
      {result && <Alert type="success" message={result} showIcon />}
      <Form<SnapshotForm>
        layout="vertical"
        initialValues={{ providerCode: 'directory-provider', snapshotVersion: 1, payloadJson: '{\n  "organizations": [],\n  "positions": [],\n  "users": []\n}' }}
        onFinish={(values) => mutation.mutate(values)}
      >
        <Form.Item label="Provider Code" name="providerCode" rules={[{ required: true }]}>
          <Input />
        </Form.Item>
        <Form.Item label="快照版本" name="snapshotVersion" rules={[{ required: true }]}>
          <InputNumber min={0} precision={0} style={{ width: '100%' }} />
        </Form.Item>
        <Form.Item label="Checksum" name="checksum" rules={[{ required: true }]}>
          <Input />
        </Form.Item>
        <Form.Item
          label="完整快照 JSON"
          name="payloadJson"
          rules={[
            { required: true },
            {
              validator: async (_rule, value: string) => {
                try {
                  JSON.parse(value)
                } catch {
                  throw new Error('请输入合法 JSON')
                }
              },
            },
          ]}
        >
          <Input.TextArea rows={10} />
        </Form.Item>
        {mutation.error && <Alert type="error" message="快照提交失败" description={String(mutation.error)} showIcon />}
        <Space>
          <PermissionGuard permission="system:directory:sync">
            <Button type="primary" htmlType="submit" loading={mutation.isPending}>提交并激活快照</Button>
          </PermissionGuard>
        </Space>
      </Form>
    </Card>
  )
}
