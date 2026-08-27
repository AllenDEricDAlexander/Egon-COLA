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

vi.mock('wujie-react', async () => {
  const React = await import('react')

  const MockWujieReact = (props: Record<string, unknown>) => {
    React.useEffect(() => {
      const beforeLoad = props.beforeLoad as (() => void) | undefined
      const afterMount = props.afterMount as (() => void) | undefined
      const loadError = props.loadError as ((url: string, error: Error) => void) | undefined
      const events = ((window as unknown as Record<string, unknown>).__WUJIE_TEST_EVENTS ?? []) as string[]
      ;(window as unknown as Record<string, unknown>).__WUJIE_TEST_EVENTS = events
      ;(window as unknown as Record<string, unknown>).__WUJIE_TEST_TRIGGER_LOAD_ERROR = () => {
        events.push('loadError')
        loadError?.('/children/test.js', new Error('child load failed'))
      }
      events.push('beforeLoad')
      beforeLoad?.()
      events.push('afterMount')
      afterMount?.()
      return () => {
        const beforeUnmount = props.beforeUnmount as (() => void) | undefined
        const afterUnmount = props.afterUnmount as (() => void) | undefined
        events.push('beforeUnmount')
        beforeUnmount?.()
        events.push('afterUnmount')
        afterUnmount?.()
      }
    // Wujie callback props belong to one child instance; re-running on every
    // callback identity change would simulate an unmount during parent state updates.
    // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [props.name, props.url])
    return React.createElement('div', { 'data-testid': 'wujie-child' }, '子应用已加载')
  }

  return { default: MockWujieReact }
})
