import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { Result } from 'antd'
import { createBrowserRouter, RouterProvider } from 'react-router-dom'

const routerError = (
  <Result
    status="error"
    title="页面加载失败"
    subTitle="请返回上一页或刷新后重试。"
  />
)

const notFound = (
  <Result status="404" title="404" subTitle="页面不存在" />
)

const router = createBrowserRouter([
  {
    path: '*',
    element: notFound,
    errorElement: routerError,
  },
])

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      refetchOnWindowFocus: false,
      retry: false,
    },
    mutations: {
      retry: false,
    },
  },
})

export const App = () => (
  <QueryClientProvider client={queryClient}>
    <RouterProvider router={router} />
  </QueryClientProvider>
)
