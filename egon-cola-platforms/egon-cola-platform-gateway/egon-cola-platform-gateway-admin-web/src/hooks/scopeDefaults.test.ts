import { describe, expect, it } from 'vitest'
import { resolveInitialScope, scopeOptions } from './scopeDefaults'

describe('Gateway Admin scope defaults', () => {
  it('uses trimmed deployment scope values', () => {
    expect(resolveInitialScope(' dev ', ' codex-local ')).toEqual({
      env: 'dev',
      namespace: 'codex-local',
    })
  })

  it('falls back when deployment scope values are blank', () => {
    expect(resolveInitialScope(' ', undefined)).toEqual({
      env: 'dev',
      namespace: 'default',
    })
  })

  it('keeps a configured namespace selectable', () => {
    expect(scopeOptions('codex-local', ['default', 'public', 'internal']))
      .toContainEqual({
        value: 'codex-local',
        label: 'Namespace: codex-local',
      })
  })
})
