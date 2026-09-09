import type { ComponentType } from 'react'
import { describe, expect, it } from 'vitest'
import { Rbac3ComponentRegistry } from './Rbac3ComponentRegistry'

const LocalPage: ComponentType = () => null

describe('Rbac3ComponentRegistry', () => {
  it('resolves only build-time local React components', () => {
    const registry = new Rbac3ComponentRegistry()
    registry.register('orders-page', LocalPage)

    expect(registry.resolve('orders-page')).toBe(LocalPage)
    expect(registry.has('orders-page')).toBe(true)
  })

  it('rejects duplicate keys and unknown components', () => {
    const registry = new Rbac3ComponentRegistry([['orders-page', LocalPage]])

    expect(() => registry.register('orders-page', LocalPage)).toThrow(/duplicate/i)
    expect(() => registry.require('missing-page')).toThrow(/unknown/i)
  })

  it('rejects URLs and remote JavaScript identifiers', () => {
    const registry = new Rbac3ComponentRegistry()
    const remoteModule = ['https', 'remote.example/page.js'].join('://')

    expect(() => registry.register(remoteModule, LocalPage))
      .toThrow(/local component key/i)
    expect(() => registry.register('remote.js', remoteModule as never))
      .toThrow(/React component/i)
  })
})
