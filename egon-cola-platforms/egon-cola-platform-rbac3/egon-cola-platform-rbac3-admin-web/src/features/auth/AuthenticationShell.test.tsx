import {type Rbac3Client, Rbac3Provider, Rbac3RequestError} from '@egon-cola/rbac3-react-sdk'
import {render, screen} from '@testing-library/react'
import {MemoryRouter} from 'react-router-dom'
import {describe, expect, it} from 'vitest'
import {AuthenticationShell} from './AuthenticationShell'

describe('authentication shell', () => {
  it('offers unified SSO without local username, password, or token fields', async () => {
    const client = {
        getBootstrap: async () => {
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
    expect(screen.queryByLabelText(/Token/)).not.toBeInTheDocument()
  })
})
