import {expect, test} from '@playwright/test'

test('contract fixture: Gateway cookie login carries no RBAC role or token payload', async ({page}) => {
    await page.route('**/api/v1/auth/bootstrap', (route) => route.fulfill({
        status: 401,
        json: {
            error: {code: 'AUTHENTICATION_REQUIRED', message: 'login', retryable: false, details: []},
            meta: {requestId: 'e2e', traceId: 'e2e', timestamp: new Date().toISOString()}
        },
    }))
  let loginBody: Record<string, unknown> | null = null
    await page.route('**/oauth2/login/csrf', (route) => route.fulfill({json: {token: 'csrf-fixture'}}))
    await page.route('**/oauth2/login', async (route) => {
    loginBody = route.request().postDataJSON() as Record<string, unknown>
        await route.fulfill({json: {identitySub: 'alice-sub', displayName: 'Alice', mustChangePassword: false}})
  })
  await page.goto('/')
    await page.getByLabel('租户 ID').fill('fixture')
    await page.getByLabel('用户名').fill('alice')
    await page.getByLabel('密码').fill('secret')
  await page.getByRole('button', { name: /登\s*录/ }).click()
  expect(loginBody).not.toHaveProperty('roleId')
    expect(loginBody).not.toHaveProperty('accessToken')
    expect(loginBody).not.toHaveProperty('refreshToken')
})
