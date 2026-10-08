import { expect, test } from '@playwright/test'

test('admin console offers passwordless OIDC login', async ({ page }) => {
  await page.route('**/api/v1/tianshu/auth/bootstrap', (route) => route.fulfill({
    status: 401,
    contentType: 'application/json',
    body: JSON.stringify({code: 'AUTHENTICATION_REQUIRED'}),
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
