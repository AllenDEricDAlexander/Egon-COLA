import {access, readdir, readFile} from 'node:fs/promises'
import {dirname, extname, join, resolve} from 'node:path'
import {fileURLToPath} from 'node:url'

const packageRoot = resolve(dirname(fileURLToPath(import.meta.url)), '..')
const sourceRoot = join(packageRoot, 'src')
const distRoot = join(packageRoot, 'dist')
const platformsRoot = resolve(packageRoot, '../..')
const forbiddenSource = [
  'ManifestDetailPage',
  'ResourceManifest',
  '/auth/bootstrap',
  'reportFrontendResources',
  'system:resource:report',
  'RolePermission',
  'PermissionResource',
  'permissionIds',
  '/iam/roles/:roleId/permissions',
  'iam.resource.report',
]
const forbiddenDist = [
  'report-rbac-resources.mjs',
  'rbac3:resource-catalog:report',
  'RBAC3_SERVICE_ACCESS_TOKEN',
  'SERVICE_ACCESS_TOKEN',
  'RolePermission',
  'PermissionResource',
  '/iam/roles/:roleId/permissions',
]

const files = async (directory) => {
  const entries = await readdir(directory, {withFileTypes: true})
  const result = []
  for (const entry of entries) {
    const path = join(directory, entry.name)
    if (entry.isDirectory()) result.push(...await files(path))
    else if (['.ts', '.tsx', '.json', '.js', '.mjs'].includes(extname(entry.name))
      && !/\.test\.(?:ts|tsx|js|mjs)$/.test(entry.name)) result.push(path)
  }
  return result
}

const assertSharedVersion = async () => {
  const consumerRoots = [
    packageRoot,
    join(platformsRoot, 'egon-cola-tianshu/egon-cola-tianshu-admin-web'),
    join(platformsRoot, 'egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin-web'),
    join(platformsRoot, 'egon-cola-yuheng/yuheng-admin-web'),
  ]
  for (const root of consumerRoots) {
    const manifest = JSON.parse(await readFile(join(root, 'package.json'), 'utf8'))
    if (manifest.dependencies?.['@egon-cola/xingyuan-admin-web-shared'] !== '^0.2.0') {
      throw new Error(`shared dependency is not ^0.2.0 (${root})`)
    }
    const lockPath = root === packageRoot
      ? join(platformsRoot, 'egon-cola-tianquan-jianshen/package-lock.json')
      : join(root, 'package-lock.json')
    const lockText = await readFile(lockPath, 'utf8')
    const lock = JSON.parse(lockText)
    const entry = lock.packages?.['node_modules/@egon-cola/xingyuan-admin-web-shared']
    if (!/^0\.2\.\d+$/.test(entry?.version ?? '')
      || entry.resolved?.includes('0.1.4')
      || entry.resolved?.startsWith('file:')
      || entry.resolved?.startsWith('link:')) {
      throw new Error(`shared lock does not resolve a published 0.2.x version (${lockPath})`)
    }
  }
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
await assertSharedVersion()
process.stdout.write('rbac3 conformance guard passed\n')
