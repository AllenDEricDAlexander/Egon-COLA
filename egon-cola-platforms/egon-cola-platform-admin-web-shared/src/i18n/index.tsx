import i18next from 'i18next'
import { initReactI18next, useTranslation } from 'react-i18next'
import { I18nextProvider } from 'react-i18next'
import type { PropsWithChildren } from 'react'
import { zhCN } from './zh-CN'

let initialized = false

export interface I18nInitOptions {
  readonly defaultNS?: string
  readonly resources: Record<string, Record<string, Record<string, string>>>
  readonly lng?: string
}

export const initI18n = (options: I18nInitOptions): void => {
  if (initialized) return
  void i18next
    .use(initReactI18next)
    .init({
      lng: options.lng ?? (typeof navigator !== 'undefined' ? navigator.language : 'zh-CN'),
      fallbackLng: 'zh-CN',
      defaultNS: options.defaultNS ?? 'common',
      resources: {
        'zh-CN': { common: zhCN.common, ...options.resources['zh-CN'] },
        ...options.resources,
      },
      interpolation: { escapeValue: false },
      returnNull: false,
      returnEmptyString: false,
    })
  initialized = true
}

export const I18nProvider = ({ children }: PropsWithChildren) => (
  <I18nextProvider i18n={i18next}>{children}</I18nextProvider>
)

export const useT = (ns?: string) => useTranslation(ns).t

export const changeLanguage = async (lng: string): Promise<void> => {
  await i18next.changeLanguage(lng)
}

export const currentLanguage = (): string => i18next.language
