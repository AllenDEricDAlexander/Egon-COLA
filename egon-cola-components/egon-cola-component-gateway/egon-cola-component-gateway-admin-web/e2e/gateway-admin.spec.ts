import { expect, test } from '@playwright/test'

test.describe('Gateway Admin contract smoke', () => {
  test('does not expose excluded infrastructure navigation', async ({ page }) => {
    await page.goto('/')
    await expect(page.getByText('Nginx')).toHaveCount(0)
    await expect(page.getByText('Nacos')).toHaveCount(0)
    await expect(page.getByText('Dubbo')).toHaveCount(0)
  })
})
