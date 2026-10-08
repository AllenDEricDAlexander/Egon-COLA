import {type Rbac3Client, Rbac3Provider, Rbac3RequestError} from '@egon-cola/tianquan-jianshen-react-sdk'
import {fireEvent, render, screen} from '@testing-library/react'
import {MemoryRouter} from 'react-router-dom'
import {afterEach, describe, expect, it, vi} from 'vitest'
import {AuthenticationShell} from './AuthenticationShell'
import {gatewayAuth} from './gatewayAuth'

describe('authentication shell', () => {
  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('offers unified SSO without local username, password, or token fields', async () => {
    const client = {
        getAbout: async () => {
        throw new Rbac3RequestError({
          status: 401,
          code: 'AUTHENTICATION_REQUIRED',
          message: 'login',
          retryable: false,
        })
      },
    } as unknown as Rbac3Client
    render(
      <MemoryRouter>
          <Rbac3Provider client={client}>
          <AuthenticationShell><div>READY CONTENT</div></AuthenticationShell>
        </Rbac3Provider>
      </MemoryRouter>,
    )

    expect(await screen.findByRole('button', { name: '使用统一身份登录' }))
      .toBeInTheDocument()
    expect(screen.queryByLabelText('Username')).not.toBeInTheDocument()
    expect(screen.queryByLabelText('Password')).not.toBeInTheDocument()
    expect(screen.queryByLabelText('用户名')).not.toBeInTheDocument()
    expect(screen.queryByLabelText('密码')).not.toBeInTheDocument()
    expect(screen.queryByLabelText(/Token/)).not.toBeInTheDocument()
  })

  it('starts the configured OIDC flow from the sign-in action', async () => {
    const client = {
      getAbout: async () => {
        throw new Rbac3RequestError({
          status: 401,
          code: 'AUTHENTICATION_REQUIRED',
          message: 'login',
          retryable: false,
        })
      },
    } as unknown as Rbac3Client
    const login = vi.spyOn(gatewayAuth, 'login').mockImplementation(() => {})
    render(
      <MemoryRouter>
        <Rbac3Provider client={client}>
          <AuthenticationShell><div>READY CONTENT</div></AuthenticationShell>
        </Rbac3Provider>
      </MemoryRouter>,
    )

    fireEvent.click(await screen.findByRole('button', {name: '使用统一身份登录'}))

    expect(login).toHaveBeenCalledWith()
  })
})
