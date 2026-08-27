import { useQuery } from '@tanstack/react-query'
import { Button, Card, Col, Row, Space, Tag, Typography } from 'antd'
import { useState } from 'react'
import { Link } from 'react-router-dom'
import {
  EnterpriseLayout,
  PageHeader,
  PageState,
  type EnterpriseNavigationItem,
} from '@egon-cola/admin-web-shared'
import type { PlatformKey } from '../manifest/types'
import {
  loadSummary,
  PLATFORM_KEYS,
  retrySummary,
  type PlatformSummaryCard,
  type SummaryAdapters,
} from '../summary/platformSummaryFacade'

export interface PortalPlatformItem {
  readonly key: PlatformKey
  readonly label: string
  readonly path: string
  readonly standaloneUrl: string
}

const configuredStandaloneUrl = (
  environmentKey: string,
  path: string,
  fallback: string,
): string => {
  const configuredOrigin = import.meta.env[environmentKey]
  return configuredOrigin
    ? `${configuredOrigin.replace(/\/$/, '')}${path}`
    : fallback
}

export const PORTAL_PLATFORM_ITEMS: readonly PortalPlatformItem[] = [
  {
    key: 'idp',
    label: '身份与安全',
    path: '/platform/idp/overview',
    standaloneUrl: configuredStandaloneUrl('VITE_IDP_ADMIN_WEB_URL', '/overview', '/idp/overview'),
  },
  {
    key: 'rbac3',
    label: '权限治理',
    path: '/platform/rbac3/roles',
    standaloneUrl: configuredStandaloneUrl('VITE_RBAC3_ADMIN_WEB_URL', '/roles', '/rbac3/roles'),
  },
  {
    key: 'gateway',
    label: 'API 网关',
    path: '/platform/gateway/dashboard',
    standaloneUrl: configuredStandaloneUrl('VITE_GATEWAY_ADMIN_WEB_URL', '/dashboard', '/gateway/dashboard'),
  },
  {
    key: 'ddc',
    label: '配置中心',
    path: '/platform/ddc/registry',
    standaloneUrl: configuredStandaloneUrl('VITE_DDC_ADMIN_WEB_URL', '/registry', '/ddc/registry'),
  },
]

export interface PortalHomePageProps {
  readonly adapters?: SummaryAdapters
  readonly allowedPlatformKeys?: readonly PlatformKey[]
  readonly scope?: string
}

const statusLabels: Record<PlatformSummaryCard['status'], string> = {
  READY: '正常',
  DEGRADED: '部分可用',
  ERROR: '加载失败',
  TIMEOUT: '请求超时',
  NOT_CONFIGURED: '未配置',
  DENIED: '无权访问',
}

const statusColors: Record<PlatformSummaryCard['status'], string> = {
  READY: 'success',
  DEGRADED: 'warning',
  ERROR: 'error',
  TIMEOUT: 'warning',
  NOT_CONFIGURED: 'default',
  DENIED: 'error',
}

export const navigationFor = (allowedPlatformKeys?: readonly PlatformKey[]): EnterpriseNavigationItem[] => {
  const allowed = new Set(allowedPlatformKeys ?? PLATFORM_KEYS)
  return PORTAL_PLATFORM_ITEMS
    .filter((item) => allowed.has(item.key))
    .map((item) => ({
      key: item.key,
      label: item.label,
      path: item.path,
      activePathPrefixes: [`/platform/${item.key}`],
    }))
}

export const PortalHomePage = ({
  adapters = {},
  allowedPlatformKeys,
  scope = 'default',
}: PortalHomePageProps) => {
  const [retryingKey, setRetryingKey] = useState<PlatformKey | undefined>()
  const [retryCards, setRetryCards] = useState<Partial<Record<PlatformKey, PlatformSummaryCard>>>({})
  const summaryQuery = useQuery({
    queryKey: ['portal-summary', scope],
    queryFn: ({ signal }) => loadSummary({ scope }, signal, adapters),
  })

  const visibleKeys = new Set(allowedPlatformKeys ?? PLATFORM_KEYS)
  const visibleItems = PORTAL_PLATFORM_ITEMS.filter((item) => visibleKeys.has(item.key))
  const cards = visibleItems.map((item) => retryCards[item.key] ?? summaryQuery.data?.cards[item.key]).filter(
    (card): card is PlatformSummaryCard => card !== undefined,
  )

  const handleRetry = async (platformKey: PlatformKey): Promise<void> => {
    setRetryingKey(platformKey)
    try {
      const card = await retrySummary(platformKey, { scope }, new AbortController().signal, adapters)
      setRetryCards((current) => ({ ...current, [platformKey]: card }))
    } finally {
      setRetryingKey(undefined)
    }
  }

  const config = {
    platformName: 'Egon COLA Platform',
    navigation: navigationFor(allowedPlatformKeys),
    footer: { version: '5.3.2' },
  }

  return (
    <EnterpriseLayout config={config}>
      <PageHeader
        title="平台工作台"
        subtitle="统一查看身份、权限、网关和配置平台状态"
      />
      <PageState
        loading={summaryQuery.isPending}
        error={summaryQuery.error}
        empty={false}
        onRetry={() => { void summaryQuery.refetch() }}
      >
        <Row gutter={[16, 16]}>
          {cards.map((card) => {
            const item = PORTAL_PLATFORM_ITEMS.find((candidate) => candidate.key === card.platformKey)
            if (!item) return null
            return (
              <Col key={card.platformKey} xs={24} sm={12} xl={6}>
                <Card
                  title={card.label}
                  extra={<Tag color={statusColors[card.status]}>{statusLabels[card.status]}</Tag>}
                  styles={{ body: { minHeight: 132 } }}
                >
                  <Space direction="vertical" size="middle" style={{ width: '100%' }}>
                    <Typography.Text type="secondary">
                      {card.summary ?? card.message ?? statusLabels[card.status]}
                    </Typography.Text>
                    <Space wrap>
                      <Link to={item.path}>进入平台</Link>
                      <a href={item.standaloneUrl}>独立打开 {item.label}</a>
                      {card.status !== 'READY' && card.status !== 'NOT_CONFIGURED' && (
                        <Button
                          type="link"
                          size="small"
                          loading={retryingKey === card.platformKey}
                          onClick={() => { void handleRetry(card.platformKey) }}
                        >
                          重试
                        </Button>
                      )}
                    </Space>
                  </Space>
                </Card>
              </Col>
            )
          })}
        </Row>
      </PageState>
    </EnterpriseLayout>
  )
}
