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
      namespaceCode: 'default',
      env: 'local',
      appCode: 'orders',
    })
  })

  it('falls back when deployment scope values are blank', () => {
    expect(resolveInitialScope(' ', undefined, ' ', undefined)).toEqual({
      bizCode: '',
      namespaceCode: '',
      env: '',
      appCode: '',
    })
  })
})
