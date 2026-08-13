import { Flex, Layout, Space, theme, Typography } from 'antd'
import { useT } from '../i18n'
import type { EnterpriseFooterConfig } from './types'

export interface EnterpriseFooterProps extends EnterpriseFooterConfig {
  readonly platformName: string
}

/**
 * 统一轻量 Footer：只展示 Copyright、平台名称与版本等仓库中真实存在的信息，
 * 不包含任何虚构的联系方式、备案号、社交媒体或法律链接。
 */
export const EnterpriseFooter = ({
  platformName,
  copyrightName = 'Egon COLA',
  version,
  extra,
}: EnterpriseFooterProps) => {
  const { token } = theme.useToken()
  const t = useT()
  const year = new Date().getFullYear()

  return (
    <Layout.Footer
      style={{
        background: token.colorBgContainer,
        borderTop: `1px solid ${token.colorBorderSecondary}`,
        paddingBlock: 14,
        paddingInline: 24,
      }}
    >
      <Flex justify="space-between" align="center" wrap gap={8}>
        <Typography.Text type="secondary" style={{ fontSize: 13 }}>
          © {year} {copyrightName} · {platformName}
        </Typography.Text>
        <Space size={16}>
          {extra}
          {version && (
            <Typography.Text type="secondary" style={{ fontSize: 13 }}>
              {t('layout.version', '版本')} v{version}
            </Typography.Text>
          )}
        </Space>
      </Flex>
    </Layout.Footer>
  )
}
