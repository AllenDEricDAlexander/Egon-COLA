import {act, cleanup, render, screen, waitFor} from '@testing-library/react'
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest'

const clients = vi.hoisted(() => ({
  yuheng: {bootstrap: vi.fn(), login: vi.fn(), logout: vi.fn()},
  yaoguang: {bootstrap: vi.fn(), login: vi.fn(), logout: vi.fn()},
}))

vi.mock('@egon-cola/xingyuan-admin-web-shared', () => ({
  createGatewayAuthClient: ({registrationId}: {registrationId: string}) =>
    registrationId === 'edge-yaoguang' ? clients.yaoguang : clients.yuheng,
  createHttpClient: () => vi.fn(),
}))

import {AuthProvider, useAuth} from './AuthContext'

const Identity = () => {
  const auth = useAuth()
  return <p>{auth.workspace}:{auth.authorization?.user?.id ?? 'none'}</p>
}

beforeEach(() => {
  vi.clearAllMocks()
})
afterEach(cleanup)

describe('workspace authorization isolation', () => {
  it('bootstraps knowledge with its own client and path', async () => {
    clients.yaoguang.bootstrap.mockResolvedValue({user: {id: 'knowledge-user'}})
    render(<AuthProvider resource="yaoguang"><Identity /></AuthProvider>)
    await screen.findByText('yaoguang:knowledge-user')
    expect(clients.yaoguang.bootstrap).toHaveBeenCalledWith('/api/v1/yaoguang/auth/bootstrap')
    expect(clients.yuheng.bootstrap).not.toHaveBeenCalled()
  })

  it('ignores an old bootstrap response after moving to the other workspace', async () => {
    let completeOld: (value: unknown) => void = () => {}
    clients.yuheng.bootstrap.mockImplementation(() => new Promise((resolve) => { completeOld = resolve }))
    clients.yaoguang.bootstrap.mockResolvedValue({user: {id: 'knowledge-user'}})
    const view = render(<AuthProvider key="yuheng"><Identity /></AuthProvider>)
    await waitFor(() => expect(clients.yuheng.bootstrap).toHaveBeenCalledOnce())
    view.rerender(<AuthProvider key="yaoguang" resource="yaoguang"><Identity /></AuthProvider>)
    await screen.findByText('yaoguang:knowledge-user')
    await act(async () => { completeOld({user: {id: 'old-gateway-user'}}) })
    expect(screen.getByText('yaoguang:knowledge-user')).toBeTruthy()
    expect(screen.queryByText(/old-gateway-user/)).toBeNull()
  })
})
