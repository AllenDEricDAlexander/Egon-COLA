import { PermissionGuard } from '@egon-cola/rbac3-react-sdk'
import { Button, Card, Popconfirm, Select, Table, Tag, Typography } from 'antd'
import type { MutationView } from './runtime.api'

export interface MutationRecoveryPanelProps {
  readonly mutations: readonly MutationView[]
  readonly status?: string
  readonly retrying: boolean
  readonly onStatusChange: (status?: string) => void
  readonly onRetry: (mutationId: string) => void
}

export const MutationRecoveryPanel = ({ mutations, status, retrying, onStatusChange, onRetry }: MutationRecoveryPanelProps) => (
  <Card
    title="Mutation Journal 与受控恢复"
    extra={<Select allowClear placeholder="状态过滤" value={status} onChange={onStatusChange} options={['PENDING', 'APPLYING', 'APPLIED', 'FAILED'].map((value) => ({ value }))} />}
  >
    <Typography.Paragraph type="secondary">恢复操作只接受列表中稳定的 Mutation ID；FAILED 只能重试，页面不能强制改成 APPLIED。</Typography.Paragraph>
    <Table<MutationView>
      rowKey="mutationId"
      dataSource={mutations}
      pagination={false}
      columns={[
        { title: 'Mutation ID', dataIndex: 'mutationId' },
        { title: 'Scope', render: (_value, row) => `${row.scopeType}:${row.scopeId}` },
        { title: '状态', dataIndex: 'status', render: (value: string) => <Tag>{value}</Tag> },
        { title: 'Attempt', dataIndex: 'attempt' },
        { title: 'Last Error', dataIndex: 'lastErrorCode', render: (value: string | null) => value ?? '-' },
        { title: 'Updated At', dataIndex: 'updatedAt' },
        {
          title: '操作', render: (_value, row) => row.status === 'FAILED' && (
            <PermissionGuard permission="system:authorization-runtime:operate">
              <Popconfirm title={`再次读取后重试 Mutation ${row.mutationId}？`} onConfirm={() => onRetry(row.mutationId)}>
                <Button size="small" loading={retrying} aria-label={`重试 ${row.mutationId}`}>重试</Button>
              </Popconfirm>
            </PermissionGuard>
          ),
        },
      ]}
    />
  </Card>
)
