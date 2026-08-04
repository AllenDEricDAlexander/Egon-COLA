import react from '@vitejs/plugin-react'
import { defineConfig } from 'vitest/config'

export default defineConfig({
  plugins: [react()],
  resolve: {
    dedupe: ["react", "react-dom"],
  },
  server: {
    host: '127.0.0.1',
    port: 18131,
    strictPort: true,
    proxy: {
      '/api': {
        target: process.env.RBAC3_ADMIN_PROXY ?? 'http://127.0.0.1:18130',
        changeOrigin: true,
      },
    },
  },
  build: {
    sourcemap: false,
    chunkSizeWarningLimit: 900,
    rollupOptions: {
      output: {
        manualChunks: (id) => {
          const antdPath = id.split('/node_modules/antd/es/')[1]
          if (antdPath) {
            const component = antdPath.split('/')[0]
            if (['table', 'form', 'select', 'input', 'input-number', 'checkbox'].includes(component)) return 'vendor-antd-data'
            if (['modal', 'drawer', 'popconfirm', 'tooltip'].includes(component)) return 'vendor-antd-overlay'
            return 'vendor-antd-core'
          }
          if (id.includes('/node_modules/@ant-design/')) return 'vendor-antd-core'
          if (id.includes('/node_modules/@tanstack/')) return 'vendor-query'
          if (id.includes('/node_modules/react') || id.includes('/node_modules/scheduler/')) return 'vendor-react'
          return undefined
        },
      },
    },
  },
  test: {
    environment: 'jsdom',
    fileParallelism: false,
    setupFiles: './src/test/setup.ts',
    exclude: ['e2e/**', 'node_modules/**', 'dist/**'],
  },
})
