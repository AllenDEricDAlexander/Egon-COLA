import { Flex, Typography } from 'antd'
import type { ReactNode } from 'react'

type Props = {
  title: string
  description: string
  extra?: ReactNode
}

export default function AdminPageHeader({ title, description, extra }: Props) {
  return (
    <Flex justify="space-between" align="flex-start" gap={16} wrap>
      <div>
        <Typography.Title level={3}>{title}</Typography.Title>
        <Typography.Paragraph type="secondary">
          {description}
        </Typography.Paragraph>
      </div>
      {extra}
    </Flex>
  )
}
