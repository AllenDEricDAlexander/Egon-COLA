import { Card } from 'antd'
import type { ReactNode } from 'react'
import { PageHeader } from './PageHeader'
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
    <PageHeader title={title} subtitle={subtitle} breadcrumbs={breadcrumbs} extra={extra} />
    <Card>
      <PageState {...pageState}>{children}</PageState>
    </Card>
  </div>
)
