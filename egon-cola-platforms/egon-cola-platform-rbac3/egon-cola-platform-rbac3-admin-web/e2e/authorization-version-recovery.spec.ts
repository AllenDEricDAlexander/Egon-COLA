import { expect, test } from '@playwright/test'

test('contract fixture: a version mismatch clears transient state and rebuilds once', async ({ page }) => {
  let refreshCount = 0
  await page.route('**/api/rbac3/v1/auth/refresh', async (route) => {
    refreshCount += 1
    await route.fulfill({ status: 401, json: { error: { code: 'SESSION_VERSION_MISMATCH', message: 'refresh required', retryable: false, details: [] }, meta: { requestId: 'e2e', traceId: 'e2e', timestamp: new Date().toISOString() } } })
  })
  await page.goto('/')
  await expect(page.getByLabel('Tenant Code')).toBeVisible()
  expect(refreshCount).toBe(1)
})
