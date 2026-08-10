import { QueryClientProvider } from '@tanstack/react-query'
import { render } from '@testing-library/react'
import { App as AntdApp } from 'antd'
import type { ReactElement } from 'react'
import { createDdcQueryClient } from '../query/queryClient'

export function renderWithQueryClient(ui: ReactElement) {
  const client = createDdcQueryClient()
  return {
    queryClient: client,
    ...render(
      <QueryClientProvider client={client}>
        <AntdApp>{ui}</AntdApp>
      </QueryClientProvider>,
    ),
  }
}
