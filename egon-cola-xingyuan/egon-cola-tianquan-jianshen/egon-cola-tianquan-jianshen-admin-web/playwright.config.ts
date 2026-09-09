import { defineConfig } from '@playwright/test'

export default defineConfig({
  testDir: './e2e',
  fullyParallel: false,
  retries: 0,
  reporter: 'list',
  use: {
    baseURL: process.env.TIANQUAN_JIANSHEN_E2E_BASE_URL ?? 'http://127.0.0.1:4173',
    storageState: process.env.TIANQUAN_JIANSHEN_E2E_AUTH_STATE,
    trace: 'retain-on-failure',
  },
})
