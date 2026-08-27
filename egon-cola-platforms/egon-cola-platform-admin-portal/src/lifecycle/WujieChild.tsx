import WujieReact from 'wujie-react'
import type { ComponentType, PropsWithChildren, ReactNode } from 'react'
import { Component } from 'react'
import { Alert } from 'antd'
import type { ChildContext } from '../bridge/context'
import { buildChildProps } from '../bridge/context'
import { PORTAL_HOST_VERSION } from '../manifest/loader'
import type { ChildManifest } from '../manifest/types'
import type { LifecycleEvent } from './lifecycleState'

interface WujieRuntimeProps {
  readonly name: string
  readonly url: string
  readonly width: string
  readonly height: string
  readonly sync: boolean
  readonly props: ChildContext & { readonly embedded: true }
  readonly beforeLoad: () => void
  readonly afterMount: () => void
  readonly beforeUnmount: () => void
  readonly afterUnmount: () => void
  readonly loadError: () => void
}

const WujieRuntime = WujieReact as unknown as ComponentType<WujieRuntimeProps>

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
  const safeProps = {
    ...buildChildProps({
      ...context,
      platformKey: manifest.key,
      hostVersion: context.hostVersion ?? PORTAL_HOST_VERSION,
    }),
    // This is a presentation-only lifecycle flag; credentials stay child-owned.
    embedded: true as const,
  }

  return (
    <ChildErrorBoundary onCrash={() => onState({ type: 'CHILD_CRASHED', message: 'Child render failed' })}>
      <WujieRuntime
        name={manifest.key}
        url={manifest.url}
        width="100%"
        height="100%"
        sync
        props={safeProps}
        beforeLoad={() => onState({ type: 'LOAD' })}
        afterMount={() => onState({ type: 'MOUNTED', cleanupToken: `${manifest.key}:${manifest.version}` })}
        beforeUnmount={() => onState({ type: 'UNMOUNT' })}
        afterUnmount={() => onState({ type: 'CLEANED' })}
        loadError={() => onState({
          type: 'MOUNT_FAILED',
          code: 'CHILD_LOAD_ERROR',
          message: 'Child asset failed to load',
        })}
      />
    </ChildErrorBoundary>
  )
}
