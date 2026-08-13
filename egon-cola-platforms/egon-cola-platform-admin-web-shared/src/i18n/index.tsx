import i18next from 'i18next'
import { initReactI18next, useTranslation } from 'react-i18next'
import { I18nextProvider } from 'react-i18next'
import type { PropsWithChildren } from 'react'
import { enUS } from './en-US'
import { zhCN } from './zh-CN'

let initialized = false

export interface I18nInitOptions {
  readonly defaultNS?: string
  readonly resources: Record<string, Record<string, Record<string, string>>>
  readonly lng?: string
}

export const initI18n = (options: I18nInitOptions): Promise<void> => {
  if (initialized) return Promise.resolve()
  const promise = i18next
    .use(initReactI18next)
    .init({
      lng: options.lng ?? (typeof navigator !== 'undefined' ? navigator.language : 'zh-CN'),
      fallbackLng: 'zh-CN',
      defaultNS: options.defaultNS ?? 'common',
      resources: {
        ...options.resources,
        'zh-CN': { common: zhCN.common, ...options.resources['zh-CN'] },
        'en-US': { common: enUS.common, ...options.resources['en-US'] },
      },
      interpolation: { escapeValue: false },
      returnNull: false,
      returnEmptyString: false,
    })
  initialized = true
  return promise.then(() => undefined)
}

export const I18nProvider = ({ children }: PropsWithChildren) => (
  <I18nextProvider i18n={i18next}>{children}</I18nextProvider>
)

export const useT = (ns?: string) => useTranslation(ns).t

export const changeLanguage = async (lng: string): Promise<void> => {
  await i18next.changeLanguage(lng)
}

export const currentLanguage = (): string => i18next.language
