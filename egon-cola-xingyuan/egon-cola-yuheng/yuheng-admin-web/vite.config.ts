import { defineConfig } from 'vitest/config'
import react from '@vitejs/plugin-react'
import { egonFaviconPlugin } from '@egon-cola/xingyuan-admin-web-shared/vite-plugin'

export default defineConfig({
  plugins: [react(), egonFaviconPlugin()],
  resolve: {
    dedupe: ["react", "react-dom", "react-i18next", "i18next", "antd", "@tanstack/react-query", "react-router-dom", "@ant-design/icons"],
  },
  server: {
    host: '127.0.0.1',
    port: 18141,
    strictPort: true,
    proxy: {
      '/oauth2': {
        target: process.env.YUHENG_AUTH_PROXY ?? 'http://127.0.0.1:18180',
        changeOrigin: true,
      },
      '/api': {
        target: process.env.YUHENG_ADMIN_PROXY ?? 'http://127.0.0.1:18180',
        changeOrigin: true,
      },
    },
  },
  build: {
    sourcemap: false,
    chunkSizeWarningLimit: 900,
  },
  test: {
    environment: 'jsdom',
    fileParallelism: false,
    setupFiles: './src/test/setup.ts',
    exclude: ['e2e/**', 'node_modules/**', 'dist/**'],
    coverage: {
      reporter: ['text', 'html'],
    },
  },
})
