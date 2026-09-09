import { ConfigProvider } from 'antd'
import zhCN from 'antd/locale/zh_CN'
import type { PropsWithChildren } from 'react'
import { designTokens } from './tokens'

export const AdminThemeProvider = ({ children }: PropsWithChildren) => (
  <ConfigProvider
    locale={zhCN}
    theme={{
      token: {
        colorPrimary: designTokens.color.primary,
        colorText: designTokens.color.text,
        colorTextSecondary: designTokens.color.textSecondary,
        colorBgContainer: designTokens.color.backgroundAlt,
        colorBorder: designTokens.color.border,
        colorError: designTokens.color.error,
        colorWarning: designTokens.color.warning,
        colorSuccess: designTokens.color.success,
        borderRadius: designTokens.radius.md,
        fontFamily: designTokens.font.family,
      },
    }}
  >
    {children}
  </ConfigProvider>
)
