export const designTokens = {
  color: {
    primary: '#2447b8',
    primaryHover: '#1a3696',
    text: '#172033',
    textSecondary: '#64748b',
    background: '#f4f7fb',
    backgroundAlt: '#ffffff',
    border: '#e7eaf0',
    error: '#dc2626',
    warning: '#f59e0b',
    success: '#16a34a',
  },
  radius: { sm: 4, md: 8, lg: 12 },
  spacing: { xs: 8, sm: 12, md: 16, lg: 24, xl: 32 },
  font: {
    family: 'Inter, "PingFang SC", "Microsoft YaHei", ui-sans-serif, system-ui, -apple-system, sans-serif',
  },
} as const

export type DesignTokens = typeof designTokens

const CSS_VAR_PREFIX = '--egon-'

export const injectTokens = (): void => {
  const root = document.documentElement
  root.style.setProperty(`${CSS_VAR_PREFIX}color-primary`, designTokens.color.primary)
  root.style.setProperty(`${CSS_VAR_PREFIX}color-text`, designTokens.color.text)
  root.style.setProperty(`${CSS_VAR_PREFIX}color-text-secondary`, designTokens.color.textSecondary)
  root.style.setProperty(`${CSS_VAR_PREFIX}color-background`, designTokens.color.background)
  root.style.setProperty(`${CSS_VAR_PREFIX}color-background-alt`, designTokens.color.backgroundAlt)
  root.style.setProperty(`${CSS_VAR_PREFIX}color-border`, designTokens.color.border)
  root.style.setProperty(`${CSS_VAR_PREFIX}color-error`, designTokens.color.error)
  root.style.setProperty(`${CSS_VAR_PREFIX}color-warning`, designTokens.color.warning)
  root.style.setProperty(`${CSS_VAR_PREFIX}color-success`, designTokens.color.success)
  root.style.setProperty(`${CSS_VAR_PREFIX}font-family`, designTokens.font.family)
  root.style.fontFamily = designTokens.font.family
  root.style.color = designTokens.color.text
  root.style.background = designTokens.color.background

  const style = document.createElement('style')
  style.textContent = `
    body { margin: 0; min-height: 100vh; background: ${designTokens.color.background}; }
  `
  document.head.appendChild(style)
}
