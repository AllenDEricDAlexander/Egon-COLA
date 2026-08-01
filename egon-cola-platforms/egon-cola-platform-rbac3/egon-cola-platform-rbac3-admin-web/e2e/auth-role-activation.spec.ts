import { expect, test } from '@playwright/test'

test('contract fixture: login has no role and activation replaces complete roots', async ({ page }) => {
  await page.route('**/api/rbac3/v1/auth/refresh', (route) => route.fulfill({ status: 401, json: { error: { code: 'AUTHENTICATION_REQUIRED', message: 'login', retryable: false, details: [] }, meta: { requestId: 'e2e', traceId: 'e2e', timestamp: new Date().toISOString() } } }))
  let loginBody: Record<string, unknown> | null = null
  await page.route('**/api/rbac3/v1/auth/login', async (route) => {
    loginBody = route.request().postDataJSON() as Record<string, unknown>
    await route.fulfill({ json: { data: { accessToken: 'fixture', roleActivationRequired: true, sessionId: '1' } } })
  })
  await page.goto('/')
  await page.getByLabel('Tenant Code').fill('fixture')
  await page.getByLabel('Username').fill('alice')
  await page.getByLabel('Password').fill('secret')
  await page.getByRole('button', { name: /登\s*录/ }).click()
  expect(loginBody).not.toHaveProperty('roleId')
})
