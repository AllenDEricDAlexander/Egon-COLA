// Theme
export { designTokens, injectTokens, type DesignTokens } from './theme/tokens'
export { AdminThemeProvider } from './theme/ThemeProvider'

// API
export { createHttpClient, type HttpClient, type HttpClientConfig } from './api/httpClient'
export { ApiError, classifyApiError, type ErrorClassification } from './api/errors'
export { decodeTokenPayload, computeExpiresAt, isTokenExpired } from './api/jwt'

// Auth
export { createTokenStore, type TokenStore, type AuthTokens } from './auth/tokenStore'
export { createOAuthClient, type OAuthClient, type OAuthClientConfiguration, type OAuthRuntime } from './auth/oauthClient'

// i18n
export { initI18n, I18nProvider, useT, changeLanguage, currentLanguage, type I18nInitOptions } from './i18n'

// Components
export { PageState, type PageStateProps } from './components/PageState'
export { AppErrorBoundary, type AppErrorBoundaryProps } from './components/AppErrorBoundary'
export { PageTemplate, type PageTemplateProps, type BreadcrumbItem } from './components/PageTemplate'

// Layout
export { EnterpriseHeader, type EnterpriseHeaderProps } from './layout/EnterpriseHeader'
export { EnterpriseFooter, type EnterpriseFooterProps } from './layout/EnterpriseFooter'
export { EnterpriseLayout, type EnterpriseLayoutProps } from './layout/EnterpriseLayout'
export type {
  EnterpriseNavigationItem,
  EnterpriseUser,
  EnterpriseHeaderConfig,
  EnterpriseFooterConfig,
  EnterpriseLayoutConfig,
} from './layout/types'

// Hooks
export { usePermission } from './hooks/usePermission'
export { useFeatureQuery, type FeatureQueryDeps } from './hooks/useFeatureQuery'
