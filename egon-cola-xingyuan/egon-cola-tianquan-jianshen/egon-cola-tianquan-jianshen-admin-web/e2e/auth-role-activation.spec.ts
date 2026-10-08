import {expect, test} from '@playwright/test'

test('contract fixture: Jianshen login redirects to the configured OIDC registration without credentials', async ({page}) => {
    await page.route('**/api/v1/auth/about', (route) => route.fulfill({
        status: 401,
        json: {
            error: {code: 'AUTHENTICATION_REQUIRED', message: 'login', retryable: false, details: []},
            meta: {requestId: 'e2e', traceId: 'e2e', timestamp: new Date().toISOString()}
        },
    }))
    await page.route('**/oauth2/authorization/edge-ui', (route) => route.fulfill({
        status: 200,
        contentType: 'text/html',
        body: '<html>OIDC fixture</html>',
    }))
  await page.goto('/')
    const login = page.getByRole('button', {name: '使用统一身份登录'})
    await expect(login).toBeVisible()
    await expect(page.getByRole('textbox')).toHaveCount(0)
    await login.click()
    await expect(page).toHaveURL(/\/oauth2\/authorization\/edge-ui$/)
})
