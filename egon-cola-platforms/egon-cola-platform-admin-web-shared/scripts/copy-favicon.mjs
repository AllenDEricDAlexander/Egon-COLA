import { copyFileSync, existsSync } from 'node:fs'
import { dirname, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'

// 仓库根的唯一 favicon 来源：egon-cola-platforms/favicon.png
// 复制到 shared 包根，随包发布，供 egonFaviconPlugin 在发布产物中定位。
const here = dirname(fileURLToPath(import.meta.url))
const source = resolve(here, '..', '..', 'favicon.png')
const target = resolve(here, '..', 'favicon.png')

if (!existsSync(source)) {
  console.error(`[copy-favicon] source not found: ${source}`)
  process.exit(1)
}

copyFileSync(source, target)
console.log(`[copy-favicon] copied ${source} -> ${target}`)
