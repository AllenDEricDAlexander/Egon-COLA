export type RegistryScope = {
  bizCode: string
  appCode: string
  env: string
  namespace: string
}

const valueOr = (value: string | undefined, fallback: string) => {
  const normalized = value?.trim()
  return normalized ? normalized : fallback
}

export const resolveInitialScope = (
  configuredBizCode?: string,
  configuredAppCode?: string,
  configuredEnv?: string,
  configuredNamespace?: string,
): RegistryScope => ({
  bizCode: valueOr(configuredBizCode, 'default'),
  appCode: valueOr(configuredAppCode, 'default-app'),
  env: valueOr(configuredEnv, 'dev'),
  namespace: valueOr(configuredNamespace, 'default'),
})

export const configuredInitialScope = resolveInitialScope(
  import.meta.env.VITE_DDC_ADMIN_DEFAULT_BIZ_CODE,
  import.meta.env.VITE_DDC_ADMIN_DEFAULT_APP_CODE,
  import.meta.env.VITE_DDC_ADMIN_DEFAULT_ENV,
  import.meta.env.VITE_DDC_ADMIN_DEFAULT_NAMESPACE,
)
