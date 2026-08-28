import { startApp } from 'wujie'
import type { PropsWithChildren, ReactNode } from 'react'
import { Component } from 'react'
import { useEffect, useRef } from 'react'
import { Alert } from 'antd'
import type { ChildContext } from '../bridge/context'
import { buildChildProps } from '../bridge/context'
import { PORTAL_HOST_VERSION } from '../manifest/loader'
import type { ChildManifest } from '../manifest/types'
import type { LifecycleEvent } from './lifecycleState'

export interface WujieChildProps {
  readonly manifest: ChildManifest
  readonly context: ChildContext
  readonly onState: (event: LifecycleEvent) => void
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

    onState({ type: 'LOAD' })
    void startApp({
      name: manifest.key,
      url: manifest.url,
      el: host,
      sync: true,
      fiber: false,
      // Vite development HTML contains HMR modules that are safer in Wujie's
      // supported degrade iframe; production keeps the normal sandbox path.
      degrade: import.meta.env.DEV,
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
    })
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
  }, [manifest.key, manifest.url, manifest.version])

  return (
    <ChildErrorBoundary onCrash={() => onState({ type: 'CHILD_CRASHED', message: 'Child render failed' })}>
      <div ref={hostRef} data-testid={`wujie-host-${manifest.key}`} style={{ width: '100%', minHeight: 420 }} />
    </ChildErrorBoundary>
  )
}
