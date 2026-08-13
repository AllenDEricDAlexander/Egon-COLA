import { existsSync, readFileSync } from 'node:fs'
import { dirname, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'
import type { Plugin } from 'vite'

export interface EgonFaviconPluginOptions {
  /** 显式 favicon 文件路径；缺省时按包内文件、仓库根 favicon.png 的顺序自动查找。 */
  readonly source?: string
  /** 浏览器侧访问路径，默认 `/favicon.png`。 */
  readonly publicPath?: string
}

const CONTENT_TYPE = 'image/png'

/**
 * 按优先级定位 favicon.png（唯一资源来源为仓库根 egon-cola-platforms/favicon.png）：
 *
 * 1. 显式传入的路径；
 * 2. 本包内发布文件（node_modules/@egon-cola/admin-web-shared/favicon.png，
 *    从进程 cwd 向上遍历，兼容 workspace hoist；vite build 会把 vite.config
 *    连同依赖重新 bundle，import.meta.url 会被重写到临时文件，因此不能依赖它）；
 * 3. import.meta.url 相对位置（dev 直连场景：包根、monorepo 仓库根）。
 */
const locateFavicon = (explicit?: string): string | null => {
  const candidates: Array<string | undefined> = [explicit]

  let dir = process.cwd()
  for (let level = 0; level < 6; level += 1) {
    candidates.push(resolve(dir, 'node_modules/@egon-cola/admin-web-shared/favicon.png'))
    const parent = resolve(dir, '..')
    if (parent === dir) break
    dir = parent
  }

  const packageRoot = resolve(dirname(fileURLToPath(import.meta.url)), '..', '..')
  candidates.push(resolve(packageRoot, 'favicon.png'), resolve(packageRoot, '..', 'favicon.png'))

  for (const candidate of candidates) {
    if (candidate && existsSync(candidate)) return candidate
  }
  return null
}

/**
 * 统一 favicon Vite 插件：四个业务 Web 共用同一配置方式与同一资源来源。
 *
 * - dev / preview：挂载 middleware 提供 favicon 内容
 * - index.html：统一注入 `<link rel="icon">`
 * - build：将 favicon.png 输出到产物根目录
 */
export const egonFaviconPlugin = (options: EgonFaviconPluginOptions = {}): Plugin => {
  const publicPath = options.publicPath ?? '/favicon.png'
  let resolvedSource: string | null | undefined
  let warned = false

  const sourcePath = (): string | null => {
    if (resolvedSource === undefined) {
      resolvedSource = locateFavicon(options.source)
      if (!resolvedSource && !warned) {
        warned = true
        console.warn('[egon-favicon] favicon.png not found; skipping favicon injection')
      }
    }
    return resolvedSource
  }

  const serve = (req: { url?: string }, res: {
    setHeader: (name: string, value: string) => void
    end: (body: Buffer) => void
  }): boolean => {
    if (req.url !== publicPath) return false
    const source = sourcePath()
    if (!source) {
      res.setHeader('Content-Type', 'text/plain')
      res.end(Buffer.from('favicon not found'))
      return true
    }
    res.setHeader('Content-Type', CONTENT_TYPE)
    res.end(readFileSync(source))
    return true
  }

  return {
    name: 'egon-favicon',
    configureServer(server) {
      server.middlewares.use((req, res, next) => {
        if (serve(req, res)) return
        next()
      })
    },
    configurePreviewServer(server) {
      server.middlewares.use((req, res, next) => {
        if (serve(req, res)) return
        next()
      })
    },
    transformIndexHtml() {
      if (!sourcePath()) return undefined
      return [{
        tag: 'link',
        attrs: { rel: 'icon', type: CONTENT_TYPE, href: publicPath },
        injectTo: 'head-prepend',
      }]
    },
    generateBundle() {
      const source = sourcePath()
      if (!source) return
      const fileName = publicPath.replace(/^\//, '')
      if (fileName.includes('/')) {
        // 仅支持产物根目录下的 favicon，避免跨目录 emit 的路径歧义。
        console.warn(`[egon-favicon] publicPath "${publicPath}" must be at the dist root; skipped`)
        return
      }
      this.emitFile({ type: 'asset', fileName, source: readFileSync(source) })
    },
  }
}
