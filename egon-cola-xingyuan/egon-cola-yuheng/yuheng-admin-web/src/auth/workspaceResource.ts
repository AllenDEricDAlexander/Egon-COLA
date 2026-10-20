export type WorkspaceResource = 'yuheng' | 'yaoguang'

export const workspaceResource = (pathname: string, search = ''): WorkspaceResource => {
  if (pathname === '/login') {
    const selected = new URLSearchParams(search).get('workspace')
    if (selected !== null && selected !== 'yuheng' && selected !== 'yaoguang') {
      throw new Error('无效的工作区登录入口')
    }
    return selected === 'yaoguang' ? 'yaoguang' : 'yuheng'
  }
  return pathname === '/knowledge' || pathname.startsWith('/knowledge/') ? 'yaoguang' : 'yuheng'
}

export const workspaceBootstrapPath = (resource: WorkspaceResource) =>
  resource === 'yaoguang' ? '/api/v1/yaoguang/auth/bootstrap' : '/api/v1/auth/bootstrap'

export const workspaceLoginPath = (resource: WorkspaceResource) =>
  resource === 'yaoguang' ? '/login?workspace=yaoguang' : '/login'
