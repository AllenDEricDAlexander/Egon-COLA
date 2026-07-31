import { defineConfig } from 'vitest/config'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      '/api': {
        target: process.env.DDC_ADMIN_PROXY ?? 'http://127.0.0.1:18080',
        changeOrigin: true,
      },
    },
  },
  preview: {
    proxy: {
      '/api': {
        target: process.env.DDC_ADMIN_PROXY ?? 'http://127.0.0.1:18080',
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
    setupFiles: './src/test/setup.ts',
    exclude: ['e2e/**', 'node_modules/**', 'dist/**'],
    coverage: {
      reporter: ['text', 'html'],
    },
  },
})
