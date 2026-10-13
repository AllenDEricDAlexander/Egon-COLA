import {loadEnv} from 'vite'
import { defineConfig } from 'vitest/config'
import react from '@vitejs/plugin-react'
import { egonFaviconPlugin } from '@egon-cola/xingyuan-admin-web-shared/vite-plugin'

export default defineConfig(({mode}) => {
  const configuredBase = loadEnv(mode, process.cwd(), 'VITE_').VITE_BASE_PATH ?? '/'
  if (!configuredBase.startsWith('/') || configuredBase.startsWith('//')
    || /[\\?#%]/.test(configuredBase) || Array.from(configuredBase).some((character) => character.charCodeAt(0) < 32 || character.charCodeAt(0) === 127) || /\/{2,}/.test(configuredBase)
    || configuredBase.split('/').some((segment) => segment === '.' || segment === '..')) {
    throw new Error('Invalid VITE_BASE_PATH')
  }
  const basePath = configuredBase === '/' ? '/' : configuredBase.replace(/\/?$/, '/')

  return {
    base: basePath,
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
          changeOrigin: false,
        },
        '/bff': {
          target: process.env.YUHENG_AUTH_PROXY ?? 'http://127.0.0.1:18180',
          changeOrigin: false,
        },
        '/login/oauth2': {
          target: process.env.YUHENG_AUTH_PROXY ?? 'http://127.0.0.1:18180',
          changeOrigin: false,
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
  }
})
