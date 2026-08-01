import { describe, expect, it } from 'vitest'
import type { GatewayScopeBinding, Scope } from '../api/types'
import {
  changeScope,
  optionsFor,
  resolveInitialScope,
} from './scopeDefaults'

const defaultScope: Scope = {
  bizCode: 'retail',
  namespace: 'default',
  env: 'local',
  appCode: 'order',
}

const opsScope: Scope = {
  ...defaultScope,
  namespace: 'ops',
}

const configuredScope: Scope = {
  ...defaultScope,
  appCode: 'invoice',
}

const invalidScope: Scope = {
  bizCode: 'missing',
  namespace: 'missing',
  env: 'missing',
  appCode: 'missing',
}

const binding = (
  scope: Scope,
  connected: boolean,
): GatewayScopeBinding => ({
  ...scope,
  bindingId: `${scope.namespace}-${scope.appCode}`,
  appName: scope.appCode,
  connected,
})

const bindings = [
  binding(defaultScope, true),
  binding(opsScope, true),
  binding(configuredScope, false),
]

describe('Gateway Admin DDC scopes', () => {
  it('uses last valid then configured then connected then first binding', () => {
    expect(resolveInitialScope(bindings, opsScope, configuredScope))
      .toEqual(opsScope)
    expect(resolveInitialScope(bindings, invalidScope, configuredScope))
      .toEqual(configuredScope)
    expect(resolveInitialScope(bindings, invalidScope, undefined))
      .toEqual(defaultScope)
    expect(resolveInitialScope(
      bindings.map((item) => ({ ...item, connected: false })),
      undefined,
      undefined,
    )).toEqual(defaultScope)
  })

  it('keeps valid descendants and otherwise resets to the first valid branch', () => {
    expect(changeScope(bindings, defaultScope, 'namespace', 'ops'))
      .toEqual(opsScope)
    expect(optionsFor(bindings, opsScope, 'appCode')).toEqual([
      { value: 'order', label: 'App: order' },
    ])
  })

  it('returns undefined when no DDC binding exists', () => {
    expect(resolveInitialScope([], undefined, undefined)).toBeUndefined()
  })
})
