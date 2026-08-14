import {expect, test} from '@playwright/test'

test('contract fixture: an unauthenticated bootstrap shows the Gateway login form', async ({page}) => {
    let bootstrapCount = 0
    await page.route('**/api/v1/auth/bootstrap', async (route) => {
        bootstrapCount += 1
        await route.fulfill({status: 401,
            json: {
                error: {code: 'AUTHENTICATION_REQUIRED', message: 'login required', retryable: false, details: []},
                meta: {requestId: 'e2e', traceId: 'e2e', timestamp: new Date().toISOString()}
            }
        })
  })
  await page.goto('/')
    await expect(page.getByLabel('租户 ID')).toBeVisible()
    expect(bootstrapCount).toBe(1)
})
