import { defineConfig } from 'vitest/config'
import react from '@vitejs/plugin-react'
import { egonFaviconPlugin } from '@egon-cola/admin-web-shared/vite-plugin'

export default defineConfig({
  plugins: [react(), egonFaviconPlugin()],
  resolve: {
    dedupe: ["react", "react-dom", "react-i18next", "i18next", "antd", "@tanstack/react-query", "react-router-dom", "@ant-design/icons"],
  },
  server: {
    host: '127.0.0.1',
    port: 18152,
    strictPort: true,
    proxy: {
      '/api': {
        target: process.env.DDC_ADMIN_PROXY ?? 'http://127.0.0.1:18150',
        changeOrigin: true,
      },
    },
  },
  preview: {
    proxy: {
      '/api': {
        target: process.env.DDC_ADMIN_PROXY ?? 'http://127.0.0.1:18150',
        changeOrigin: true,
      },
    },
  },
  build: {
    sourcemap: false,
    chunkSizeWarningLimit: 900,
  },
  test: {
    globals: true,
    environment: 'jsdom',
    fileParallelism: false,
    setupFiles: './src/test/setup.ts',
    exclude: ['e2e/**', 'node_modules/**', 'dist/**'],
    coverage: {
      reporter: ['text', 'html'],
    },
  },
})
