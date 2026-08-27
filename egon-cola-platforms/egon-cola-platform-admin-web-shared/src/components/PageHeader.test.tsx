import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { PageHeader } from './PageHeader'

describe('PageHeader', () => {
  it('renders title breadcrumb and main action', () => {
    render(
      <PageHeader
        title="配置资源"
        breadcrumbs={[{ title: '配置中心' }, { title: '配置资源' }]}
        extra={<button>新建配置</button>}
      />,
    )

    expect(screen.getByRole('heading', { name: '配置资源' })).toBeInTheDocument()
    expect(screen.getByText('配置中心')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: '新建配置' })).toBeInTheDocument()
  })

  it('omits empty subtitle and accepts an accessible action', () => {
    render(<PageHeader title="审计" extra={<button aria-label="刷新审计">刷新</button>} />)

    expect(screen.queryByText('undefined')).not.toBeInTheDocument()
    expect(screen.getByRole('button', { name: '刷新审计' })).toBeInTheDocument()
  })
})
