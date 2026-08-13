import { describe, expect, it } from 'vitest'
import {
  hasRequiredScopeFields,
  readScopeSearchParams,
  writeScopeSearchParams,
} from './scopeSearchParams'

describe('scopeSearchParams', () => {
  it('reads and writes page-local scope fields without dropping other filters', () => {
    const current = new URLSearchParams('bizCode=retail&env=prod&protocol=HTTP&page=3')
    expect(readScopeSearchParams(current, ['bizCode', 'env'])).toEqual({
      bizCode: 'retail',
      env: 'prod',
    })
    expect(writeScopeSearchParams(current, { bizCode: '', env: 'test' }, ['bizCode', 'env']).toString())
      .toBe('env=test&protocol=HTTP&page=3')
    expect(writeScopeSearchParams(
      new URLSearchParams(),
      { bizCode: 'retail', appCode: 'order' },
      ['bizCode', 'appCode'],
      'resource',
    ).toString()).toBe('resourceBizCode=retail&resourceAppCode=order')
  })

  it('checks only the fields required by the current page', () => {
    expect(hasRequiredScopeFields({ env: 'prod', namespace: 'default' }, ['env', 'namespace'])).toBe(true)
    expect(hasRequiredScopeFields({ env: 'prod' }, ['env', 'namespace'])).toBe(false)
  })
})
