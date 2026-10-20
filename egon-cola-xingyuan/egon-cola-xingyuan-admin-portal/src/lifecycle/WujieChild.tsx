import { startApp } from 'wujie'
import type { PropsWithChildren, ReactNode } from 'react'
import { Component } from 'react'
import { useEffect, useRef } from 'react'
import { Alert } from 'antd'
import type { ChildContext } from '../bridge/context'
import { buildChildProps } from '../bridge/context'
import { PORTAL_HOST_VERSION, validateChildUrl } from '../manifest/loader'
import type { ChildManifest } from '../manifest/types'
import type { LifecycleEvent } from './lifecycleState'

export interface WujieChildProps {
  readonly manifest: ChildManifest
  readonly context: ChildContext
  readonly onState: (event: LifecycleEvent) => void
}

const childRouteSuffix = (platformKey: ChildManifest['key'], routeIntent?: string): string => {
  routeIntent = routeIntent?.split(/[?#]/, 1)[0]
  const prefix = `/xingyuan/${platformKey}`
  if (!routeIntent || (routeIntent !== prefix && !routeIntent.startsWith(`${prefix}/`))) {
    return ''
  }

  try {
    const decoded = decodeURIComponent(routeIntent.slice(prefix.length)).split(/[?#]/, 1)[0]
    const segments = decoded.split('/')
    if (/[\\]/.test(decoded) || Array.from(decoded).some((character) => character.charCodeAt(0) < 32 || character.charCodeAt(0) === 127) || /%[0-9a-f]{2}/i.test(decoded)
      || segments.some((segment) => segment === '..')) {
      return ''
    }
    return segments.filter((segment) => segment && segment !== '.').join('/')
  } catch {
    return ''
  }
}

export const resolveChildUrl = (
  manifestUrl: string,
  platformKey: ChildManifest['key'],
  routeIntent?: string,
): string => {
  const baseUrl = validateChildUrl(manifestUrl, platformKey)
  const routeSuffix = childRouteSuffix(platformKey, routeIntent)
  if (!routeSuffix) {
    baseUrl.search = ''
    baseUrl.hash = ''
    return baseUrl.toString()
  }

  const basePath = baseUrl.pathname.endsWith('/')
    ? baseUrl.pathname
    : baseUrl.pathname.slice(0, baseUrl.pathname.lastIndexOf('/') + 1)
  baseUrl.pathname = `${basePath}${routeSuffix}`
  baseUrl.search = ''
  baseUrl.hash = ''
  return baseUrl.toString()
}

interface ChildErrorBoundaryProps extends PropsWithChildren {
  readonly onCrash: () => void
}

interface ChildErrorBoundaryState {
  readonly error: Error | null
}

class ChildErrorBoundary extends Component<ChildErrorBoundaryProps, ChildErrorBoundaryState> {
  state: ChildErrorBoundaryState = { error: null }

  static getDerivedStateFromError(error: Error): ChildErrorBoundaryState {
    return { error }
  }

  override componentDidCatch(): void {
    this.props.onCrash()
  }

  override render(): ReactNode {
    if (this.state.error) {
      return (
        <Alert
          type="error"
          showIcon
          message="子应用运行异常"
          description="当前子应用已隔离，可重新挂载或独立打开。"
        />
      )
    }
    return this.props.children
  }
}

export const WujieChild = ({ manifest, context, onState }: WujieChildProps) => {
  const hostRef = useRef<HTMLDivElement>(null)
  const destroyRef = useRef<(() => unknown) | undefined>(undefined)
  const safeProps = {
    ...buildChildProps({
      ...context,
      platformKey: manifest.key,
      hostVersion: context.hostVersion ?? PORTAL_HOST_VERSION,
    }),
    // This is a presentation-only lifecycle flag; credentials stay child-owned.
    embedded: true as const,
  }

  useEffect(() => {
    const host = hostRef.current
    if (!host) {
      onState({
        type: 'MOUNT_FAILED',
        code: 'CHILD_HOST_UNAVAILABLE',
        message: 'Child mount host is unavailable',
      })
      return
    }

    let disposed = false
    let cleanupReported = false
    const reportCleaned = (): void => {
      if (cleanupReported) return
      cleanupReported = true
      onState({ type: 'CLEANED' })
    }
    const reportUnmount = (): void => onState({ type: 'UNMOUNT' })

    let childUrl: string
    try { childUrl = resolveChildUrl(manifest.url, manifest.key, context.routeIntent) } catch {
      onState({type: 'MOUNT_FAILED', code: 'CHILD_URL_NOT_ALLOWED', message: 'Child mount URL is not allowed'})
      return
    }
    onState({ type: 'LOAD' })
    // A cancelled StrictMode effect must not create a sandbox that the next mount replaces while loading.
    void Promise.resolve().then(() => disposed ? undefined : startApp({
      name: manifest.key,
      url: childUrl,
      el: host,
      attrs: {src: new URL(`${import.meta.env.BASE_URL}wujie-empty.html`, window.location.origin).toString()},
      degradeAttrs: {title: manifest.displayName, style: 'display:block;width:100%;height:100%;border:0;box-shadow:none'},
      sync: false,
      fiber: false,
      // Vite development HTML contains HMR modules that are safer in Wujie's
      // supported degrade iframe; production keeps the normal sandbox path.
      degrade: import.meta.env.DEV,
      // These styles exist only inside the embedded runtime, never in standalone applications.
      plugins: [{cssAfterLoaders: [{content: `
        html, body, #root { height: 100%; min-width: 0; margin: 0; }
        #root > .ant-layout, #root > .ant-app, #root > .ant-app > .ant-layout {
          height: 100%; min-height: 0 !important;
        }
        .ant-layout-content { min-height: 0; }
      `}]}],
      props: safeProps,
      afterMount: () => onState({
        type: 'MOUNTED',
        cleanupToken: `${manifest.key}:${manifest.version}`,
      }),
      beforeUnmount: reportUnmount,
      afterUnmount: reportCleaned,
      loadError: () => onState({
        type: 'MOUNT_FAILED',
        code: 'CHILD_LOAD_ERROR',
        message: 'Child asset failed to load',
      }),
    }))
      .then((destroy) => {
        const destroyFunction = typeof destroy === 'function'
          ? destroy as () => unknown
          : undefined
        if (disposed) {
          if (destroyFunction) {
            void Promise.resolve(destroyFunction()).finally(reportCleaned)
          } else {
            reportCleaned()
          }
          return
        }
        destroyRef.current = destroyFunction
      })
      .catch(() => {
        if (!disposed) {
          onState({
            type: 'MOUNT_FAILED',
            code: 'CHILD_MOUNT_FAILED',
            message: 'Child mount rejected',
          })
        }
      })

    return () => {
      disposed = true
      const destroy = destroyRef.current
      destroyRef.current = undefined
      if (destroy) {
        void Promise.resolve(destroy()).finally(reportCleaned)
      } else {
        reportUnmount()
        reportCleaned()
      }
    }
    // The host must not remount when the parent reducer records LOAD/MOUNTED.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [context.routeIntent, manifest.key, manifest.url, manifest.version])

  return (
    <ChildErrorBoundary onCrash={() => onState({ type: 'CHILD_CRASHED', message: 'Child render failed' })}>
      <div
        ref={hostRef}
        className="portal-child-host"
        data-testid={`wujie-host-${manifest.key}`}
        style={{width: '100%', height: '100%', minHeight: 0, overflow: 'auto'}}
      />
    </ChildErrorBoundary>
  )
}
