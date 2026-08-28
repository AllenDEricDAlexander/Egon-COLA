import { render, screen, waitFor } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { startApp } from 'wujie'
import type { ChildContext } from '../bridge/context'
import type { ChildManifest } from '../manifest/types'
import { WujieChild } from './WujieChild'

const manifest: ChildManifest = {
  key: 'idp',
  displayName: '身份与安全',
  url: '/children/idp/',
  standaloneUrl: '/overview',
  version: '5.3.2',
  contractVersion: 'platform-1',
  compatibleHostRange: '>=5.3.2 <6.0.0',
  requiredCapabilities: [],
}

const context: ChildContext = {
  platformKey: 'idp',
  routeIntent: '/platform/idp/overview',
  scopeDisplay: 'default',
  hostVersion: '5.3.2',
}

afterEach(() => {
  vi.clearAllMocks()
})

describe('WujieChild', () => {
  it('mounts the child in a concrete host and reports MOUNTED', async () => {
    const onState = vi.fn()

    render(<WujieChild manifest={manifest} context={context} onState={onState} />)

    await waitFor(() => expect(onState).toHaveBeenCalledWith({
      type: 'MOUNTED',
      cleanupToken: 'idp:5.3.2',
    }))
    expect(screen.getByTestId('wujie-host-idp')).toBeInTheDocument()
    expect(vi.mocked(startApp)).toHaveBeenCalledWith(expect.objectContaining({
      name: 'idp',
      url: '/children/idp/',
      el: screen.getByTestId('wujie-host-idp'),
      fiber: false,
      sync: true,
    }))
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
    expect(screen.getByTestId('wujie-host-idp')).toBeInTheDocument()
  })

  it('destroys the Core instance when the host unmounts', async () => {
    const onState = vi.fn()
    const rendered = render(<WujieChild manifest={manifest} context={context} onState={onState} />)

    await waitFor(() => expect(onState).toHaveBeenCalledWith({
      type: 'MOUNTED',
      cleanupToken: 'idp:5.3.2',
    }))
    rendered.unmount()

    await waitFor(() => expect(onState).toHaveBeenCalledWith({ type: 'CLEANED' }))
    expect((window as unknown as { __WUJIE_TEST_EVENTS?: string[] }).__WUJIE_TEST_EVENTS)
      .toContain('afterUnmount')
  })
})
