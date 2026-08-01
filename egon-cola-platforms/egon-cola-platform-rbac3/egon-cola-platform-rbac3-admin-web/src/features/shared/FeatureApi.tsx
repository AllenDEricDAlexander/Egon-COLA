import { useRbac3Session } from '@egon-cola/rbac3-react-sdk'
import {
  createContext,
  useContext,
  useMemo,
  useState,
  type PropsWithChildren,
} from 'react'

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
  readonly targetTenantId: string | null
  readonly setTargetTenantId: (tenantId: string | null) => void
}

const FeatureApiContext = createContext<FeatureApiContextValue | null>(null)

export interface FeatureApiProviderProps extends PropsWithChildren {
  readonly client: FeatureApiClient
}

export const FeatureApiProvider = ({ client, children }: FeatureApiProviderProps) => {
  const { bootstrap } = useRbac3Session()
  const [targetTenantId, setTargetTenant] = useState<string | null>(null)
  const effectiveTenantId = targetTenantId ?? bootstrap?.user.tenantId ?? null
  const tenantClient = useMemo<FeatureApiClient>(() => ({
    request: <T,>(path: string, request: FeatureApiRequest = {}) => client.request<T>(
      path,
      targetTenantId === null
        ? request
        : {
            ...request,
            headers: {
              ...request.headers,
              'X-Target-Tenant-Id': targetTenantId,
            },
          },
    ),
  }), [client, targetTenantId])
  const setTargetTenantId = (tenantId: string | null) => {
    const normalized = tenantId?.trim() || null
    setTargetTenant(normalized)
  }
  const value = useMemo<FeatureApiContextValue>(() => ({
    client: tenantClient,
    effectiveTenantId,
    targetTenantId,
    setTargetTenantId,
  }), [effectiveTenantId, targetTenantId, tenantClient])
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
    targetTenantId: value.targetTenantId,
    setTargetTenantId: value.setTargetTenantId,
  }
}
