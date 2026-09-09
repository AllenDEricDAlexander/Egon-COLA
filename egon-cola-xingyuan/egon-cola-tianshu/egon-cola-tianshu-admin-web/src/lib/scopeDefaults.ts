export type RegistryScope = {
  bizCode: string
  namespaceCode: string
  env: string
  appCode: string
}

const normalized = (value?: string) => {
  const trimmed = value?.trim()
  return trimmed ?? ''
}

export const resolveInitialScope = (
  configuredBizCode?: string,
  configuredAppCode?: string,
  configuredEnv?: string,
  configuredNamespace?: string,
): RegistryScope => ({
  bizCode: normalized(configuredBizCode),
  namespaceCode: normalized(configuredNamespace),
  env: normalized(configuredEnv),
  appCode: normalized(configuredAppCode),
})

export const emptyScope: RegistryScope = {
  bizCode: '',
  namespaceCode: '',
  env: '',
  appCode: '',
}

export const configuredInitialScope = resolveInitialScope(
  import.meta.env.VITE_DDC_ADMIN_DEFAULT_BIZ_CODE,
  import.meta.env.VITE_DDC_ADMIN_DEFAULT_APP_CODE,
  import.meta.env.VITE_DDC_ADMIN_DEFAULT_ENV,
  import.meta.env.VITE_DDC_ADMIN_DEFAULT_NAMESPACE,
)
