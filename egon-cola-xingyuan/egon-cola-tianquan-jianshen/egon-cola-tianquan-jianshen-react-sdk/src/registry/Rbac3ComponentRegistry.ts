import type { ComponentType } from 'react'

export type Rbac3ComponentRegistration = readonly [string, ComponentType]

/** Registry for statically imported components. It never resolves remote code. */
export class Rbac3ComponentRegistry {
  private readonly components = new Map<string, ComponentType>()

  constructor(entries: readonly Rbac3ComponentRegistration[] = []) {
    entries.forEach(([key, component]) => this.register(key, component))
  }

  register(key: string, component: ComponentType): this {
    const normalized = requireLocalKey(key)
    if (this.components.has(normalized)) {
      throw new Error(`duplicate RBAC3 component key: ${normalized}`)
    }
    if (typeof component !== 'function') {
      throw new Error(`RBAC3 component key ${normalized} must reference a local React component`)
    }
    this.components.set(normalized, component)
    return this
  }

  has(key: string): boolean {
    return this.components.has(key)
  }

  resolve(key: string): ComponentType | null {
    return this.components.get(key) ?? null
  }

  require(key: string): ComponentType {
    const component = this.resolve(key)
    if (component === null) {
      throw new Error(`unknown RBAC3 component key: ${key}`)
    }
    return component
  }

  keys(): readonly string[] {
    return [...this.components.keys()]
  }
}

const requireLocalKey = (value: string): string => {
  const key = value.trim()
  if (key.length === 0 || key.length > 128 || key.includes('://')
    || !/^[A-Za-z0-9][A-Za-z0-9._/-]*$/.test(key)) {
    throw new Error('RBAC3 registry requires a local component key')
  }
  return key
}
