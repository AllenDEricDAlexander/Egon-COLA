import '@testing-library/jest-dom/vitest'
import { cleanup } from '@testing-library/react'
import { afterEach, vi } from 'vitest'

if (typeof globalThis.ResizeObserver === 'undefined') {
  globalThis.ResizeObserver = class ResizeObserver {
    observe(): void {}
    unobserve(): void {}
    disconnect(): void {}
  }
}

Object.defineProperty(window, 'matchMedia', {
  writable: true,
  value: (query: string) => ({
    matches: query.includes('min-width'),
    media: query,
    onchange: null,
    addListener: () => undefined,
    removeListener: () => undefined,
    addEventListener: () => undefined,
    removeEventListener: () => undefined,
    dispatchEvent: () => false,
  }),
})

afterEach(() => {
  cleanup()
})

vi.mock('wujie', () => {
  type WujieTestOptions = {
    readonly el: HTMLElement
    readonly afterMount?: () => void
    readonly beforeUnmount?: () => void
    readonly afterUnmount?: () => void
    readonly loadError?: (url: string, error: Error) => void
  }

  const startApp = vi.fn(async (options: WujieTestOptions) => {
    const events = ((window as unknown as Record<string, unknown>).__WUJIE_TEST_EVENTS ?? []) as string[]
    ;(window as unknown as Record<string, unknown>).__WUJIE_TEST_EVENTS = events
    events.push('beforeLoad')
    const child = document.createElement('div')
    child.dataset.testid = 'wujie-child'
    child.textContent = '子应用已加载'
    options.el.appendChild(child)
    events.push('afterMount')
    options.afterMount?.()
    ;(window as unknown as Record<string, unknown>).__WUJIE_TEST_TRIGGER_LOAD_ERROR = () => {
      events.push('loadError')
      options.loadError?.('/children/test.js', new Error('child load failed'))
    }
    return () => {
      events.push('beforeUnmount')
      options.beforeUnmount?.()
      child.remove()
      events.push('afterUnmount')
      options.afterUnmount?.()
    }
  })

  return { startApp, destroyApp: vi.fn(async () => undefined) }
})
