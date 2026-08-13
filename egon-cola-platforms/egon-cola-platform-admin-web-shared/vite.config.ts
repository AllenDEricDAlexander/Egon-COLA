import { resolve } from 'node:path'
import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

export default defineConfig({
  plugins: [react()],
  build: {
    emptyOutDir: false,
    lib: {
      entry: {
        'admin-web-shared': resolve(import.meta.dirname, 'src/index.ts'),
        // Vite 插件独立入口：node 环境专用，避免 node 内置模块进入浏览器主入口。
        'vite-plugin': resolve(import.meta.dirname, 'src/vite/faviconPlugin.ts'),
      },
      formats: ['es'],
      fileName: (_format, entryName) => `${entryName}.js`,
    },
    rollupOptions: {
      external: [
        'react',
        'react-dom',
        'react/jsx-runtime',
        'antd',
        '@ant-design/icons',
        '@tanstack/react-query',
        'react-router-dom',
        'i18next',
        'react-i18next',
        'node:fs',
        'node:path',
        'node:url',
      ],
    },
  },
  test: {
    environment: 'jsdom',
    setupFiles: ['./src/test/setup.ts'],
    exclude: ['node_modules/**', 'dist/**'],
  },
})
