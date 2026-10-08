import {cleanup, fireEvent, render, screen, waitFor} from '@testing-library/react'
import {afterEach, describe, expect, it, vi} from 'vitest'
import {CentralLoginPage} from './CentralLoginPage'

const auth = vi.hoisted(() => ({
    loading: false,
    error: undefined,
    login: vi.fn(),
}))

vi.mock('./AuthContext', () => ({useAuth: () => auth}))

afterEach(() => {
    cleanup()
    vi.clearAllMocks()
})

describe('Shoubing OIDC login', () => {
    it('starts login without tenant, credential or token inputs', async () => {
        render(<CentralLoginPage/>)

        expect(screen.queryByRole('textbox')).not.toBeInTheDocument()
        expect(screen.queryByLabelText(/租户|用户名|密码|Access Token|Refresh Token/)).not.toBeInTheDocument()
        fireEvent.click(screen.getByRole('button', {name: '使用统一身份登录'}))

        await waitFor(() => expect(auth.login).toHaveBeenCalledOnce())
    })
})
