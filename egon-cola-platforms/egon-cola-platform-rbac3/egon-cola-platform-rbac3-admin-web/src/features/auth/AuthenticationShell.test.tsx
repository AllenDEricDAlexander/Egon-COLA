import { InMemoryAccessTokenStore, Rbac3Provider, Rbac3RequestError, type LoginRequest, type LoginResult, type Rbac3Client } from '@egon-cola/rbac3-react-sdk'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { AuthenticationShell } from './AuthenticationShell'
import type { AuthApi } from './auth.api'

describe('authentication shell', () => {
  it('authenticates without a role field and rebuilds through refresh', async () => {
    const login = vi.fn(async (request: LoginRequest) => { void request; return { sessionId: '11' } as LoginResult })
    const refresh = vi.fn()
      .mockRejectedValueOnce(new Rbac3RequestError({ status: 401, code: 'AUTHENTICATION_REQUIRED', message: 'login', retryable: false }))
      .mockResolvedValue({ accessToken: 'access', roleActivationRequired: false })
    const client = {
      refresh,
      getBootstrap: async () => ({ user: { id: '7', tenantId: '9' }, permissions: [], fieldPolicies: {}, activeRoleContexts: [], apps: [], menus: [], routes: [], actions: [] }),
    } as unknown as Rbac3Client
    render(
      <Rbac3Provider client={client} accessTokenStore={new InMemoryAccessTokenStore()}>
        <AuthenticationShell authApi={{ login } as unknown as AuthApi}><div>READY CONTENT</div></AuthenticationShell>
      </Rbac3Provider>,
    )
    await screen.findByLabelText('Tenant Code')
    fireEvent.change(screen.getByLabelText('Tenant Code'), { target: { value: 'tenant-a' } })
    fireEvent.change(screen.getByLabelText('Username'), { target: { value: 'mario' } })
    fireEvent.change(screen.getByLabelText('Password'), { target: { value: 'secret' } })
    fireEvent.click(screen.getByRole('button', { name: /登\s*录/ }))
    await waitFor(() => expect(login).toHaveBeenCalledTimes(1))
    expect(login.mock.calls[0]?.[0]).not.toHaveProperty('roleId')
    await waitFor(() => expect(screen.getByText('READY CONTENT')).toBeInTheDocument())
  })
})
