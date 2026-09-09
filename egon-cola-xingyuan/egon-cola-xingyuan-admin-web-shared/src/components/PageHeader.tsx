import { Breadcrumb, Space, Typography } from 'antd'
import type { ReactNode } from 'react'
import type { BreadcrumbItem } from './PageTemplate'

export interface PageHeaderProps {
  readonly title: string
  readonly subtitle?: string
  readonly breadcrumbs?: readonly BreadcrumbItem[]
  readonly extra?: ReactNode
}

export const PageHeader = ({
  title,
  subtitle,
  breadcrumbs,
  extra,
}: PageHeaderProps) => (
  <section className="egon-page-header">
    {breadcrumbs && breadcrumbs.length > 0 && (
      <Breadcrumb
        style={{ marginBottom: 16 }}
        items={breadcrumbs.map((item) => ({
          title: item.path ? <a href={item.path}>{item.title}</a> : item.title,
        }))}
      />
    )}
    <Space align="start" style={{ display: 'flex', justifyContent: 'space-between' }}>
      <div>
        <Typography.Title level={4} style={{ margin: 0 }}>{title}</Typography.Title>
        {subtitle && <Typography.Text type="secondary">{subtitle}</Typography.Text>}
      </div>
      {extra}
    </Space>
  </section>
)
