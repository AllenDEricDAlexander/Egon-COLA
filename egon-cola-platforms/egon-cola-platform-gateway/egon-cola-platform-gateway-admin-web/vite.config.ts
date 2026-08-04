import { defineConfig } from 'vitest/config'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  resolve: {
    dedupe: ["react", "react-dom"],
  },
  server: {
    host: '127.0.0.1',
    port: 18141,
    strictPort: true,
    proxy: {
      '/api': {
        target: process.env.GATEWAY_ADMIN_PROXY ?? 'http://127.0.0.1:18140',
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
