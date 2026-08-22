import {useRbac3Authorization} from '@egon-cola/rbac3-react-sdk'
import {createContext, type PropsWithChildren, useContext, useMemo,} from 'react'

export interface FeatureApiRequest {
  readonly method?: 'GET' | 'POST' | 'PUT' | 'DELETE'
  readonly query?: Readonly<Record<string, string | number | boolean | null | undefined>>
  readonly body?: unknown
  readonly headers?: Readonly<Record<string, string>>
}

export interface FeatureApiClient {
  request<T>(path: string, request?: FeatureApiRequest): Promise<T>
}

interface FeatureApiContextValue {
  readonly client: FeatureApiClient
  readonly effectiveTenantId: string | null
}

const FeatureApiContext = createContext<FeatureApiContextValue | null>(null)

export interface FeatureApiProviderProps extends PropsWithChildren {
  readonly client: FeatureApiClient
}

export const FeatureApiProvider = ({ client, children }: FeatureApiProviderProps) => {
    const {about} = useRbac3Authorization()
  const effectiveTenantId = about?.user.tenantId ?? null
  const tenantClient = useMemo<FeatureApiClient>(() => ({
    request: async <T,>(path: string, request: FeatureApiRequest = {}) => {
        return client.request<T>(path, request)
    },
  }), [client])
  const value = useMemo<FeatureApiContextValue>(() => ({
    client: tenantClient,
    effectiveTenantId,
  }), [effectiveTenantId, tenantClient])
  return <FeatureApiContext.Provider value={value}>{children}</FeatureApiContext.Provider>
}

export const useFeatureApi = (): FeatureApiClient => {
  const value = useContext(FeatureApiContext)
  if (value === null) {
    throw new Error('RBAC3 FeatureApiProvider is required')
  }
  return value.client
}

export const useFeatureTenantContext = () => {
  const value = useContext(FeatureApiContext)
  if (value === null) {
    throw new Error('RBAC3 FeatureApiProvider is required')
  }
  return {
    effectiveTenantId: value.effectiveTenantId,
  }
}
