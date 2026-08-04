import { Breadcrumb, Card, Typography } from 'antd'
import type { ReactNode } from 'react'
import { PageState, type PageStateProps } from './PageState'

export interface BreadcrumbItem {
  readonly title: string
  readonly path?: string
}

export interface PageTemplateProps {
  readonly title: string
  readonly subtitle?: string
  readonly breadcrumbs?: readonly BreadcrumbItem[]
  readonly extra?: ReactNode
  readonly pageState: Omit<PageStateProps, 'children'>
  readonly children: ReactNode
}

export const PageTemplate = ({
  title,
  subtitle,
  breadcrumbs,
  extra,
  pageState,
  children,
}: PageTemplateProps) => (
  <div>
    {breadcrumbs && breadcrumbs.length > 0 && (
      <Breadcrumb
        style={{ marginBottom: 16 }}
        items={breadcrumbs.map((item) => ({
          title: item.path ? <a href={item.path}>{item.title}</a> : item.title,
        }))}
      />
    )}
    <Card
      title={(
        <div>
          <Typography.Title level={4} style={{ margin: 0 }}>{title}</Typography.Title>
          {subtitle && <Typography.Text type="secondary">{subtitle}</Typography.Text>}
        </div>
      )}
      extra={extra}
    >
      <PageState {...pageState}>{children}</PageState>
    </Card>
  </div>
)
