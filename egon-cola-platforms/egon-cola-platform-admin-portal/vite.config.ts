import { resolve as resolvePath } from 'node:path'
import react from '@vitejs/plugin-react'
import { defineConfig } from 'vitest/config'
import {egonFaviconPlugin} from '@egon-cola/admin-web-shared/vite-plugin'

export default defineConfig({
  plugins: [react(), egonFaviconPlugin()],
  publicDir: process.env.PORTAL_PUBLIC_DIR ?? 'public',
  resolve: {
    alias: {
      '@ant-design/icons': resolvePath(import.meta.dirname, 'node_modules/@ant-design/icons'),
      '@tanstack/react-query': resolvePath(import.meta.dirname, 'node_modules/@tanstack/react-query'),
      antd: resolvePath(import.meta.dirname, 'node_modules/antd'),
      i18next: resolvePath(import.meta.dirname, 'node_modules/i18next'),
      react: resolvePath(import.meta.dirname, 'node_modules/react'),
      'react-dom': resolvePath(import.meta.dirname, 'node_modules/react-dom'),
      'react-i18next': resolvePath(import.meta.dirname, 'node_modules/react-i18next'),
      'react-router-dom': resolvePath(import.meta.dirname, 'node_modules/react-router-dom'),
    },
    dedupe: ['react', 'react-dom', 'react-i18next', 'i18next', 'antd', '@tanstack/react-query', 'react-router-dom', '@ant-design/icons'],
  },
  server: {
    host: '127.0.0.1',
    port: 18125,
    strictPort: true,
    proxy: {
      '/oauth2': {
        target: process.env.PORTAL_AUTH_PROXY ?? 'http://127.0.0.1:18180',
        changeOrigin: true,
      },
      '/api': {
        target: process.env.PORTAL_API_PROXY ?? 'http://127.0.0.1:18100',
        changeOrigin: true,
      },
      ...(process.env.PORTAL_MANIFEST_PROXY
        ? {
            '/portal-manifest': {
              target: process.env.PORTAL_MANIFEST_PROXY,
              changeOrigin: true,
            },
          }
        : {}),
    },
  },
  build: { sourcemap: false, chunkSizeWarningLimit: 900 },
  test: {
    environment: 'jsdom',
    setupFiles: './src/test/setup.ts',
    exclude: ['node_modules/**', 'dist/**'],
  },
})
