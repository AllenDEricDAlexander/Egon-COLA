import { QueryClient } from '@tanstack/react-query'

export const createDdcQueryClient = (): QueryClient => new QueryClient({
  defaultOptions: {
    queries: { retry: false, staleTime: 30_000 },
    mutations: { retry: false },
  },
})

export const createDdcTenantQueryKey = <T extends readonly unknown[]>(
  tenantId: string,
  ...parts: T
) => {
  if (!tenantId) throw new Error('A verified tenant is required for a Tianshu query key')
  return ['tianshu', tenantId, ...parts] as const
}

export const cancelAndClearDdcQueryClient = async (client: QueryClient): Promise<void> => {
  const cancellation = client.cancelQueries()
  client.clear()
  await cancellation
}

export const queryClient = createDdcQueryClient()
