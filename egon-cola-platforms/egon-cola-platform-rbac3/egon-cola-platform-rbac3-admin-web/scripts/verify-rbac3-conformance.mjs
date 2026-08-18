import { access, readdir, readFile } from 'node:fs/promises'
import { dirname, extname, join, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'

const packageRoot = resolve(dirname(fileURLToPath(import.meta.url)), '..')
const sourceRoot = join(packageRoot, 'src')
const distRoot = join(packageRoot, 'dist')
const forbiddenSource = [
  'ManifestDetailPage',
  'ResourceManifest',
  '/auth/bootstrap',
  'reportFrontendResources',
  'system:resource:report',
]
const forbiddenDist = ['report-rbac-resources.mjs', 'rbac3:resource-catalog:report', 'SERVICE_ACCESS_TOKEN']

const files = async (directory) => {
  const entries = await readdir(directory, { withFileTypes: true })
  const result = []
  for (const entry of entries) {
    const path = join(directory, entry.name)
    if (entry.isDirectory()) result.push(...await files(path))
    else if (['.ts', '.tsx', '.json', '.js', '.mjs'].includes(extname(entry.name))) result.push(path)
  }
  return result
}

await access(join(sourceRoot, 'app/resourceDefinitions.json'))
await access(join(packageRoot, 'scripts/report-rbac-resources.mjs'))
for (const file of await files(sourceRoot)) {
  const source = await readFile(file, 'utf8')
  const violation = forbiddenSource.find((value) => source.includes(value))
  if (violation) throw new Error(`forbidden RBAC3 web source symbol: ${violation} (${file})`)
  if (source.includes("from 'scripts/") || source.includes('from "scripts/')) {
    throw new Error(`browser source imports CI scripts (${file})`)
  }
}
for (const file of await files(distRoot)) {
  const source = await readFile(file, 'utf8')
  const violation = forbiddenDist.find((value) => source.includes(value))
  if (violation) throw new Error(`forbidden RBAC3 web bundle material: ${violation} (${file})`)
}
process.stdout.write('rbac3 conformance guard passed\n')
