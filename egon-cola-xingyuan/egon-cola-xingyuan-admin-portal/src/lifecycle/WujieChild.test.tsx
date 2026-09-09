import { render, screen, waitFor } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { startApp } from 'wujie'
import type { ChildContext } from '../bridge/context'
import type { ChildManifest } from '../manifest/types'
import { WujieChild } from './WujieChild'

const manifest: ChildManifest = {
  key: 'tianquan-shoubing',
  displayName: '身份与安全',
  url: '/children/tianquan-shoubing/',
  standaloneUrl: '/overview',
  version: '5.3.2',
  contractVersion: 'xingyuan-1',
  compatibleHostRange: '>=5.3.2 <6.0.0',
  requiredCapabilities: [],
}

const context: ChildContext = {
  platformKey: 'tianquan-shoubing',
  routeIntent: '/xingyuan/tianquan-shoubing/overview',
  scopeDisplay: 'default',
  hostVersion: '5.3.2',
}

afterEach(() => {
  vi.clearAllMocks()
})

beforeEach(() => {
  const testWindow = window as unknown as Record<string, unknown>
  delete testWindow.__WUJIE_TEST_EVENTS
  delete testWindow.__WUJIE_TEST_TRIGGER_LOAD_ERROR
})

describe('WujieChild', () => {
  it('mounts the child in a concrete host and reports MOUNTED', async () => {
    const onState = vi.fn()

    render(<WujieChild manifest={manifest} context={context} onState={onState} />)

    await waitFor(() => expect(onState).toHaveBeenCalledWith({
      type: 'MOUNTED',
      cleanupToken: 'tianquan-shoubing:5.3.2',
    }))
    expect(screen.getByTestId('wujie-host-tianquan-shoubing')).toBeInTheDocument()
    expect(vi.mocked(startApp)).toHaveBeenCalledWith(expect.objectContaining({
      name: 'tianquan-shoubing',
      url: expect.stringMatching(/\/overview$/),
      el: screen.getByTestId('wujie-host-tianquan-shoubing'),
      attrs: {src: 'about:blank'},
      degradeAttrs: {title: '身份与安全', style: 'display:block;width:100%;height:100%;border:0;box-shadow:none'},
      fiber: false,
      sync: false,
    }))
    expect(screen.getByTestId('wujie-host-tianquan-shoubing')).toHaveStyle({
      height: '100%',
      minHeight: '0',
      overflow: 'auto',
    })
  })

  it('turns a Core mount rejection into a recoverable mount failure', async () => {
    vi.mocked(startApp).mockRejectedValueOnce(new Error('sandbox failed'))
    const onState = vi.fn()

    render(<WujieChild manifest={manifest} context={context} onState={onState} />)

    await waitFor(() => expect(onState).toHaveBeenCalledWith({
      type: 'MOUNT_FAILED',
      code: 'CHILD_MOUNT_FAILED',
      message: 'Child mount rejected',
    }))
    expect(screen.getByTestId('wujie-host-tianquan-shoubing')).toBeInTheDocument()
  })

  it('destroys the Core instance when the host unmounts', async () => {
    const onState = vi.fn()
    const rendered = render(<WujieChild manifest={manifest} context={context} onState={onState} />)

    await waitFor(() => expect(onState).toHaveBeenCalledWith({
      type: 'MOUNTED',
      cleanupToken: 'tianquan-shoubing:5.3.2',
    }))
    rendered.unmount()

    await waitFor(() => expect(onState).toHaveBeenCalledWith({ type: 'CLEANED' }))
    expect((window as unknown as { __WUJIE_TEST_EVENTS?: string[] }).__WUJIE_TEST_EVENTS)
      .toContain('afterUnmount')
  })
})
