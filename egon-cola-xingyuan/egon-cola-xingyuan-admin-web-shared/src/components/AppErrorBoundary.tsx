import { Button, Result } from 'antd'
import { Component, type ErrorInfo, type ReactNode } from 'react'

export interface AppErrorBoundaryProps {
  readonly onError?: (error: Error, info: ErrorInfo) => void
  readonly fallback?: ReactNode
  readonly children: ReactNode
}

interface State {
  readonly error: Error | null
}

export class AppErrorBoundary extends Component<AppErrorBoundaryProps, State> {
  constructor(props: AppErrorBoundaryProps) {
    super(props)
    this.state = { error: null }
  }

  static getDerivedStateFromError(error: Error): State {
    return { error }
  }

  override componentDidCatch(error: Error, info: ErrorInfo): void {
    console.error('[AppErrorBoundary]', error, info.componentStack)
    this.props.onError?.(error, info)
  }

  override render(): ReactNode {
    if (this.state.error) {
      if (this.props.fallback) return this.props.fallback
      return (
        <Result
          status="error"
          title="页面出现错误"
          subTitle={this.state.error.message}
          extra={(
            <Button type="primary" onClick={() => window.location.reload()}>
              刷新页面
            </Button>
          )}
        />
      )
    }
    return this.props.children
  }
}
