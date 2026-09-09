import { expect, test } from '@playwright/test'

test('contract fixture: definition lease and release remain independent', async ({ page }) => {
  test.skip(!process.env.TIANQUAN_JIANSHEN_E2E_AUTH_STATE, 'requires an externally prepared authenticated browser state')
  await page.goto('/diagnostics/runtime')
  await expect(page.getByText('Yuheng Definition')).toBeVisible()
  await expect(page.getByText('Tianshu HTTP Provider Lease')).toBeVisible()
  await expect(page.getByText('Yuheng Release')).toBeVisible()
})
