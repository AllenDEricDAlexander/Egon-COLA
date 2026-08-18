import { readdir, readFile } from 'node:fs/promises'
import { dirname, extname, join, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'

const dist = resolve(dirname(fileURLToPath(import.meta.url)), '..', 'dist')
const forbidden = [
  'report-rbac-resources',
  'rbac3:resource-catalog:report',
  'RBAC3_SERVICE_ACCESS_TOKEN',
  'SERVICE_ACCESS_TOKEN',
]

const files = async (directory) => {
  const entries = await readdir(directory, { withFileTypes: true })
  const result = []
  for (const entry of entries) {
    const path = join(directory, entry.name)
    if (entry.isDirectory()) result.push(...await files(path))
    else if (['.js', '.mjs', '.cjs', '.css', '.html'].includes(extname(entry.name))) result.push(path)
  }
  return result
}

for (const file of await files(dist)) {
  const source = await readFile(file, 'utf8')
  const violation = forbidden.find((value) => source.includes(value))
  if (violation) throw new Error(`browser bundle contains forbidden CI material: ${violation} (${file})`)
}
process.stdout.write('browser bundle guard passed\n')
