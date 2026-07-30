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
      external: ['react', 'react-dom'],
    },
    sourcemap: false,
  },
  test: {
    environment: 'jsdom',
    exclude: ['node_modules/**', 'dist/**'],
  },
})
