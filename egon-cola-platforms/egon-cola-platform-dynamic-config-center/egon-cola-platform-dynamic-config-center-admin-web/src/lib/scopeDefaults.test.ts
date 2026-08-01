import { describe, expect, it } from 'vitest'
import { resolveInitialScope } from './scopeDefaults'

describe('DDC Admin scope defaults', () => {
  it('uses trimmed deployment scope values', () => {
    expect(resolveInitialScope(
      ' retail ',
      ' orders ',
      ' local ',
      ' default ',
    )).toEqual({
      bizCode: 'retail',
      appCode: 'orders',
      env: 'local',
      namespace: 'default',
    })
  })

  it('falls back when deployment scope values are blank', () => {
    expect(resolveInitialScope(' ', undefined, ' ', undefined)).toEqual({
      bizCode: 'default',
      appCode: 'default-app',
      env: 'dev',
      namespace: 'default',
    })
  })
})
