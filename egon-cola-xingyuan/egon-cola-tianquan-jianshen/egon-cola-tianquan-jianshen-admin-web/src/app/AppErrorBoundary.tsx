import { Button, Result } from 'antd'
import { Component, type PropsWithChildren, type ReactNode } from 'react'

interface State { readonly failed: boolean }

export class AppErrorBoundary extends Component<PropsWithChildren, State> {
  state: State = { failed: false }

  static getDerivedStateFromError(): State { return { failed: true } }

  componentDidCatch(): void {
    // The boundary intentionally avoids rendering or logging response bodies.
  }

  render(): ReactNode {
    if (this.state.failed) return <Result status="error" title="页面加载失败" subTitle="敏感错误详情已隐藏，请刷新后重试。" extra={<Button onClick={() => this.setState({ failed: false })}>重试渲染</Button>} />
    return this.props.children
  }
}
