import react from '@vitejs/plugin-react'
import { defineConfig } from 'vitest/config'

export default defineConfig({
  plugins: [react()],
  build: {
    lib: {
      entry: 'src/index.ts',
      formats: ['es'],
      fileName: 'index',
    },
    rollupOptions: {
      external: (id) => id === 'react'
        || id.startsWith('react/')
        || id === 'react-dom'
        || id.startsWith('react-dom/'),
    },
    sourcemap: false,
  },
  test: {
    environment: 'jsdom',
    exclude: ['node_modules/**', 'dist/**'],
  },
})
