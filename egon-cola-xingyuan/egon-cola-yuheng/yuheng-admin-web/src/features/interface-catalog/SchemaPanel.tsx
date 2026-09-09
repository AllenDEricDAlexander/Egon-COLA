import { Card, Collapse, Empty, Space, Table, Tag, Typography } from 'antd'
import type { TableColumnsType } from 'antd'
import { buildSchemaRows } from './schemaRows'
import type { SchemaRow } from './schemaRows'

const columns: TableColumnsType<SchemaRow> = [
  {
    title: '字段 / 路径',
    dataIndex: 'name',
    width: 260,
    render: (name: string, row) => (
      <div className="schema-field">
        <Typography.Text strong>{name}</Typography.Text>
        {row.path !== name && (
          <Typography.Text type="secondary" className="schema-path">
            {row.path}
          </Typography.Text>
        )}
      </div>
    ),
  },
  {
    title: '类型',
    dataIndex: 'type',
    width: 150,
    render: (value: string) => <Tag color="blue">{value}</Tag>,
  },
  {
    title: '技术类型',
    dataIndex: 'technicalType',
    width: 280,
    render: (value: string) => value ? (
      <Space size={[4, 4]} wrap>
        {value.split(' · ').map((item) => <Tag key={item}>{item}</Tag>)}
      </Space>
    ) : <Typography.Text type="secondary">—</Typography.Text>,
  },
  {
    title: '必填',
    dataIndex: 'required',
    width: 80,
    render: (value: boolean) => value
      ? <Tag color="red">是</Tag>
      : <Typography.Text type="secondary">否</Typography.Text>,
  },
  {
    title: '字段说明',
    dataIndex: 'description',
    width: 260,
    render: (value?: string) => value ?? (
      <Typography.Text type="secondary">暂无字段说明</Typography.Text>
    ),
  },
  {
    title: '约束',
    dataIndex: 'constraints',
    width: 260,
    render: (values: string[]) => values.length > 0 ? (
      <Space direction="vertical" size={2}>
        {values.map((value) => <Typography.Text key={value}>{value}</Typography.Text>)}
      </Space>
    ) : <Typography.Text type="secondary">—</Typography.Text>,
  },
]

export const SchemaPanel = ({
  title,
  schema,
}: {
  title: string
  schema: Record<string, unknown>
}) => {
  const rows = buildSchemaRows(schema)
  return (
    <Card title={title}>
      {rows.length > 0 ? (
        <Table<SchemaRow>
          rowKey="key"
          columns={columns}
          dataSource={rows}
          pagination={false}
          size="small"
          scroll={{ x: 1_290 }}
          expandable={{ defaultExpandAllRows: true }}
        />
      ) : (
        <Empty
          image={Empty.PRESENTED_IMAGE_SIMPLE}
          description="暂无 Schema 字段"
        />
      )}
      <Collapse
        className="schema-raw"
        size="small"
        items={[{
          key: 'raw-schema',
          label: '原始 Schema JSON',
          children: (
            <Typography.Paragraph className="json-panel" copyable>
              {JSON.stringify(schema, null, 2)}
            </Typography.Paragraph>
          ),
        }]}
      />
    </Card>
  )
}
