import {describe, expect, it} from 'vitest'
import {workspaceBootstrapPath, workspaceLoginPath, workspaceResource} from './workspaceResource'

describe('workspace resource ownership', () => {
  it.each(['/knowledge', '/knowledge/12', '/knowledge/12/wiki/34'])(
    'uses the knowledge resource for %s', (path) => {
      expect(workspaceResource(path)).toBe('yaoguang')
    },
  )

  it.each(['/dashboard', '/llm/configuration', '/knowledge-other'])(
    'uses the gateway resource for %s', (path) => {
      expect(workspaceResource(path)).toBe('yuheng')
    },
  )

  it('keeps the knowledge login and bootstrap bound to its own resource', () => {
    expect(workspaceResource('/login', '?workspace=yaoguang')).toBe('yaoguang')
    expect(workspaceLoginPath('yaoguang')).toBe('/login?workspace=yaoguang')
    expect(workspaceBootstrapPath('yaoguang')).toBe('/api/v1/yaoguang/auth/bootstrap')
    expect(workspaceResource('/login')).toBe('yuheng')
    expect(workspaceBootstrapPath('yuheng')).toBe('/api/v1/auth/bootstrap')
    expect(() => workspaceResource('/login', '?workspace=arbitrary-client')).toThrow()
  })
})
