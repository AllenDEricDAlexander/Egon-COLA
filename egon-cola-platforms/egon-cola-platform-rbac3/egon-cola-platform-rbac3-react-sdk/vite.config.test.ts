import { describe, expect, it } from 'vitest'
import viteConfig from './vite.config'

describe('React SDK library build', () => {
  it('externalizes React peer dependency entry points and subpaths', () => {
    const external = viteConfig.build?.rollupOptions?.external

    expect(external).toEqual(expect.any(Function))
    if (typeof external !== 'function') return

    expect(external('react', undefined, false)).toBe(true)
    expect(external('react/jsx-runtime', undefined, false)).toBe(true)
    expect(external('react/jsx-dev-runtime', undefined, false)).toBe(true)
    expect(external('react-dom', undefined, false)).toBe(true)
    expect(external('react-dom/client', undefined, false)).toBe(true)
    expect(external('reactive-library', undefined, false)).toBe(false)
  })
})
