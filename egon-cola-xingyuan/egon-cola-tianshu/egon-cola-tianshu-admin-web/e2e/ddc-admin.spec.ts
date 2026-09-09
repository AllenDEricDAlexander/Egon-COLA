import { expect, test } from '@playwright/test'

const token = process.env.DDC_E2E_TOKEN ?? ''

test.skip(!token, 'DDC_E2E_TOKEN is required')

test('admin console smoke: login, registry, configs', async ({ page }) => {
  await page.goto('/')
  await page.getByPlaceholder('粘贴 admin.token 内容').fill(token)
  await page.getByRole('button', { name: '登录并加载' }).click()

  await expect(page.getByText('DDC 已连接')).toBeVisible()
  await expect(page.getByText('服务注册目录')).toBeVisible()
  await page.getByRole('menuitem', { name: '配置管理' }).click()
  await expect(page.getByRole('button', { name: '新建配置' })).toBeVisible()
})
