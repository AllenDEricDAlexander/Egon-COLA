import '@testing-library/jest-dom/vitest'

// antd 的 Menu/Grid 等组件依赖 ResizeObserver，jsdom 未提供。
if (typeof globalThis.ResizeObserver === 'undefined') {
  globalThis.ResizeObserver = class ResizeObserver {
    observe(): void {}
    unobserve(): void {}
    disconnect(): void {}
  }
}
