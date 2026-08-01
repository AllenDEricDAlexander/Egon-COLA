import { QueryClient } from '@tanstack/react-query'

export const createAdminQueryClient = () => new QueryClient({
  defaultOptions: {
    queries: { refetchOnWindowFocus: false, retry: false, staleTime: 10_000 },
    mutations: { retry: false },
  },
})
