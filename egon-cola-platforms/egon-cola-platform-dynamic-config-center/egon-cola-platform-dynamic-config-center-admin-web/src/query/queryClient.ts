import { QueryClient } from '@tanstack/react-query'

export const createDdcQueryClient = (): QueryClient => new QueryClient({
  defaultOptions: {
    queries: { retry: false, staleTime: 30_000 },
    mutations: { retry: false },
  },
})

export const queryClient = createDdcQueryClient()
