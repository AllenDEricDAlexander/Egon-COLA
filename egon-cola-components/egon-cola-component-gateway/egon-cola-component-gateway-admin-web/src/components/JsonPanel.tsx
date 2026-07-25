import { Collapse, Typography } from 'antd'

export const JsonPanel = ({
  title,
  value,
}: {
  title: string
  value: unknown
}) => (
  <Collapse
    size="small"
    items={[
      {
        key: title,
        label: title,
        children: (
          <Typography.Paragraph className="json-panel" copyable>
            {JSON.stringify(value, null, 2)}
          </Typography.Paragraph>
        ),
      },
    ]}
  />
)
