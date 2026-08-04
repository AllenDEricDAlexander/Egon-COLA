import react from '@vitejs/plugin-react'
import { defineConfig } from 'vitest/config'

export default defineConfig({
  plugins: [react()],
  resolve: {
    dedupe: ['react', 'react-dom'],
  },
  server: {
    host: '127.0.0.1',
    port: 18121,
    strictPort: true,
    proxy: {
      '/api': {
        target: process.env.IDP_ADMIN_PROXY ?? 'http://127.0.0.1:18120',
        changeOrigin: true,
      },
    },
  },
  build: { sourcemap: false, chunkSizeWarningLimit: 900 },
  test: {
    environment: 'jsdom',
    setupFiles: './src/test/setup.ts',
    exclude: ['node_modules/**', 'dist/**'],
  },
})
