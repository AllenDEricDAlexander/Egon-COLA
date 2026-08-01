import type { LoginRequest, LoginResult } from '@egon-cola/rbac3-react-sdk'

export interface AuthApi {
  login(request: LoginRequest): Promise<LoginResult>
}
