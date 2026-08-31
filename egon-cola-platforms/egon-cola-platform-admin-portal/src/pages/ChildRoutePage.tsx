import { useQuery } from '@tanstack/react-query'
import { Alert, Button, Result, Space, Spin, Typography } from 'antd'
import { useEffect, useReducer, useState } from 'react'
import { useLocation, useParams } from 'react-router-dom'
import {
  EnterpriseLayout,
  PageHeader,
} from '@egon-cola/admin-web-shared'
import { buildChildProps } from '../bridge/context'
import { ManifestLoadError, PORTAL_HOST_VERSION, loadManifest } from '../manifest/loader'
import type { PlatformKey } from '../manifest/types'
import { PORTAL_PLATFORM_ITEMS, navigationFor } from './PortalHomePage'
import { WujieChild } from '../lifecycle/WujieChild'
import {
  initialLifecycleState,
  reduceLifecycle,
} from '../lifecycle/lifecycleState'

const isPlatformKey = (value: string | undefined): value is PlatformKey => (
  value === 'idp' || value === 'rbac3' || value === 'gateway' || value === 'ddc'
)

const errorCopy = (error: unknown): string => {
  if (error instanceof ManifestLoadError) {
    switch (error.code) {
      case 'MANIFEST_VERSION_UNSUPPORTED':
        return '子应用版本不兼容，暂未挂载'
      case 'MANIFEST_URL_NOT_ALLOWED':
        return '子应用地址不安全，暂未挂载'
      case 'MANIFEST_FETCH_FAILED':
        return '子应用清单加载失败'
      default:
        return '子应用清单无效'
    }
  }
  return '子应用加载失败'
}

interface MountFailureProps {
  readonly message: string
  readonly standaloneUrl: string
  readonly retryLabel: string
  readonly onRetry: () => void
}

const MountFailure = ({ message, standaloneUrl, retryLabel, onRetry }: MountFailureProps) => (
  <Alert
    type="error"
    showIcon
    message={message}
    description={(
      <Space wrap>
        <Button type="primary" size="small" onClick={onRetry}>{retryLabel}</Button>
        <a href={standaloneUrl}>独立打开</a>
      </Space>
    )}
  />
)

export const ChildRoutePage = () => {
  const { platformKey: rawPlatformKey } = useParams()
  const platformKey = isPlatformKey(rawPlatformKey) ? rawPlatformKey : undefined
  const location = useLocation()
  const environment = import.meta.env.VITE_PORTAL_ENV ?? 'local'
  const [lifecycle, dispatch] = useReducer(
    reduceLifecycle,
    platformKey ?? 'idp',
    initialLifecycleState,
  )
  const [mountKey, setMountKey] = useState(0)
  const [remounting, setRemounting] = useState(false)

  const manifestQuery = useQuery({
    queryKey: ['portal-manifest', platformKey, environment],
    enabled: platformKey !== undefined,
    queryFn: ({ signal }) => loadManifest(platformKey as PlatformKey, environment, signal),
  })

  useEffect(() => {
    if (!remounting || lifecycle.status !== 'IDLE') return
    // The child has completed afterUnmount; only now is a new Wujie instance allowed.
    // eslint-disable-next-line react-hooks/set-state-in-effect
    setMountKey((current) => current + 1)
    setRemounting(false)
  }, [lifecycle.status, remounting])

  const item = PORTAL_PLATFORM_ITEMS.find((candidate) => candidate.key === platformKey)
  const config = {
    platformName: 'Egon COLA Platform',
    navigation: navigationFor(),
    footer: { version: PORTAL_HOST_VERSION },
  }

  let content: React.ReactNode
  if (!platformKey || !item) {
    content = <Result status="404" title="平台不存在" subTitle="请从左侧平台菜单进入有效的子应用。" />
  } else if (manifestQuery.isPending) {
    content = <Spin description="加载子应用清单" />
  } else if (manifestQuery.error) {
    content = (
      <MountFailure
        message={errorCopy(manifestQuery.error)}
        standaloneUrl={item.standaloneUrl}
        retryLabel="重试加载"
        onRetry={() => { void manifestQuery.refetch() }}
      />
    )
  } else if (!manifestQuery.data) {
    content = <Typography.Text type="secondary">子应用清单为空</Typography.Text>
  } else {
    const isFailure = lifecycle.status === 'MOUNT_FAILED' || lifecycle.status === 'CRASHED'
    const handleRemount = (): void => {
      setRemounting(true)
      dispatch({ type: 'UNMOUNT' })
    }
    const safeContext = buildChildProps({
      platformKey,
      routeIntent: location.pathname,
      scopeDisplay: 'default',
      hostVersion: PORTAL_HOST_VERSION,
    })
    content = (
      <Space orientation="vertical" size="middle" style={{ width: '100%' }}>
        {isFailure && (
          <MountFailure
            message={lifecycle.status === 'CRASHED' ? '子应用运行异常' : '子应用加载失败'}
            standaloneUrl={manifestQuery.data.standaloneUrl}
            retryLabel="重新挂载"
            onRetry={handleRemount}
          />
        )}
        {!remounting && (
          <div style={isFailure ? { display: 'none' } : undefined}>
            <WujieChild
              key={`${manifestQuery.data.key}:${mountKey}`}
              manifest={manifestQuery.data}
              context={safeContext}
              onState={dispatch}
            />
          </div>
        )}
      </Space>
    )
  }

  return (
    <EnterpriseLayout config={config}>
      <PageHeader
        title={item?.label ?? '平台'}
        subtitle="子应用运行区；业务权限和数据仍由对应平台负责"
      />
      {content}
    </EnterpriseLayout>
  )
}
