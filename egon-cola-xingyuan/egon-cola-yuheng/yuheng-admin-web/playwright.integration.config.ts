import { defineConfig } from '@playwright/test'
import { execFileSync } from 'node:child_process'
import { createHash, randomUUID, X509Certificate } from 'node:crypto'
import { lstatSync, mkdirSync, readFileSync } from 'node:fs'
import { dirname, isAbsolute, join, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'

const workspace = dirname(fileURLToPath(import.meta.url))
const repository = resolve(workspace, '../../..')
const aliases = ['edge-ui', 'edge-shoubing', 'edge-jianshen', 'edge-yuheng', 'edge-tianshu'] as const

export interface IntegrationClientFact {
  clientPk: string
  clientId: string
  realmCode: 'default' | 'tenant-b'
  issuer: string
  registrationId: string
  alias: string
  resourceUris: string[]
  allowedScopes: string[]
  accessTokenTtlSeconds: number
}

export interface IntegrationPublicClientFact {
  resourceKey: string
  readPath: string
  realmCode: 'default' | 'tenant-b'
  issuer: string
  clientId: string
  clientPk: string
  redirectUri: string
  resourceUri: string
  scopes: string[]
}

export interface IntegrationInputs {
  runId: string
  root: string
  evidenceDirectory: string
  origin: string
  manifestSha256: string
  certificatePin: string
  tenantIds: Record<'default' | 'tenant-b', string>
  subjects: Record<'admin' | 'viewer', string>
  secretFiles: Record<string, string>
  clients: IntegrationClientFact[]
  publicClients: IntegrationPublicClientFact[]
  timeoutMs: number
  runtimeProbe?: { fixtureSha256: string; artifactSha256: string }
}

/** Fixed private task operations; the public harness command surface remains unchanged. */
export function taskControl(inputs: IntegrationInputs, operation: 'REVOCATION_ON' | 'REVOCATION_OFF' | 'LOGOUT_QUEUE'): {
  value: Record<string, unknown>; path: string; sha256: string
} {
  if (process.env.XINGYUAN_ISOLATED_INTEGRATION !== 'true'
      || !['REVOCATION_ON', 'REVOCATION_OFF', 'LOGOUT_QUEUE'].includes(operation)) rejected()
  const manifestFile = process.env.XINGYUAN_INTEGRATION_MANIFEST
  if (!manifestFile || !manifestFile.startsWith(`${inputs.root}/`)
      || createHash('sha256').update(readPrivateFile(manifestFile, inputs.root)).digest('hex') !== inputs.manifestSha256) rejected()
  const mode = operation === 'LOGOUT_QUEUE' ? 'logout-queue-status' : 'revocation-fault'
  const result = join(inputs.evidenceDirectory, `task-control-${randomUUID()}.json`)
  const startedAt = Date.now()
  const arguments_ = ['-I', join(repository, 'scripts/unified-xingyuan/lib/isolated_integration.py'), mode, manifestFile, result]
  if (operation !== 'LOGOUT_QUEUE') arguments_.push(operation === 'REVOCATION_ON' ? 'on' : 'off')
  const environment = Object.fromEntries(['PATH', 'HOME', 'LANG', 'LC_ALL', 'TMPDIR'].filter(key => process.env[key] !== undefined).map(key => [key, process.env[key]!]))
  try {
    execFileSync('python3', arguments_, { timeout: inputs.timeoutMs + 5000, stdio: 'ignore',
      env: { ...environment, TZ: 'UTC', XINGYUAN_ISOLATED_INTEGRATION: 'true' } })
  } catch { rejected('XINGYUAN_DEPENDENCY_UNAVAILABLE') }
  const raw = readPrivateFile(result, inputs.evidenceDirectory)
  const value = readIntegrationJson(raw)
  if (value.schemaVersion !== 1 || value.runId !== inputs.runId || value.manifestSha256 !== inputs.manifestSha256
      || value.status !== (operation === 'LOGOUT_QUEUE' ? 'OBSERVED' : 'COMMITTED')) rejected('XINGYUAN_VERIFICATION_FAILED')
  if (operation === 'LOGOUT_QUEUE') {
    const fields = ['schemaVersion', 'runId', 'manifestSha256', 'scope', 'status', 'recordedAt', 'queuedCount', 'pendingCount', 'profileSha256', 'streamReference', 'groupReference']
    if (Object.keys(value).length !== fields.length || fields.some(key => !(key in value)) || value.scope !== 'TASK_EDGE_LOGOUT_QUEUE_COUNTS_ONLY'
        || !Number.isSafeInteger(value.queuedCount) || !Number.isSafeInteger(value.pendingCount)
        || (value.pendingCount as number) < 0 || (value.queuedCount as number) < (value.pendingCount as number) || (value.queuedCount as number) > 100000
        || ['profileSha256', 'streamReference', 'groupReference'].some(key => typeof value[key] !== 'string' || !/^[0-9a-f]{64}$/.test(value[key] as string))
        || value.streamReference !== createHash('sha256').update(`xingyuan-it:${inputs.runId}:bff:logout:v1`).digest('hex')
        || value.groupReference !== createHash('sha256').update(`xingyuan-it:${inputs.runId}:bff:revocation:v1`).digest('hex')
        || typeof value.recordedAt !== 'string' || !value.recordedAt.endsWith('Z') || !Number.isFinite(Date.parse(value.recordedAt))
        || Date.parse(value.recordedAt) < startedAt || Date.parse(value.recordedAt) > Date.now() + 1000) rejected('XINGYUAN_VERIFICATION_FAILED')
  } else {
    const fields = ['schemaVersion', 'operationId', 'runId', 'manifestSha256', 'status', 'completedAt', 'targets', 'counts', 'steps', 'error']
    const counts = object(value.counts)
    if (Object.keys(value).length !== fields.length || fields.some(key => !(key in value)) || value.operationId !== 'CLI-007' || value.error !== null
        || Object.keys(counts).length !== 5 || ['databaseCount', 'tenantCount', 'clientCount', 'catalogCount', 'liveCaseCount'].some(key => counts[key] !== 0)
        || !Array.isArray(value.steps) || value.steps.length !== 1 || object(value.steps[0]).stepId !== `TASK_AS_REVOCATION_FAULT_${operation === 'REVOCATION_ON' ? 'ON' : 'OFF'}`
        || object(value.steps[0]).state !== 'COMMITTED' || object(value.steps[0]).evidence !== 'evidence/revocation-fault-state.json'
        || !Array.isArray(value.targets) || value.targets.length !== 1 || object(value.targets[0]).targetKey !== 'nginx' || object(value.targets[0]).state !== 'RUNNING'
        || typeof value.completedAt !== 'string' || !value.completedAt.endsWith('Z') || !Number.isFinite(Date.parse(value.completedAt))
        || Date.parse(value.completedAt) < startedAt || Date.parse(value.completedAt) > Date.now() + 1000) rejected('XINGYUAN_VERIFICATION_FAILED')
  }
  return { value, path: result.slice(inputs.root.length + 1), sha256: createHash('sha256').update(raw).digest('hex') }
}

export function bffRefreshWaitMilliseconds(clients: readonly Pick<IntegrationClientFact, 'realmCode' | 'alias' | 'accessTokenTtlSeconds'>[],
  realm: 'default' | 'tenant-b'): number {
  if (realm !== 'default' && realm !== 'tenant-b') rejected()
  const required = aliases.filter(alias => alias !== 'edge-ui')
  const selected = required.map(alias => {
    const candidates = clients.filter(value => value.realmCode === realm && value.alias === alias)
    if (candidates.length !== 1 || !Number.isInteger(candidates[0].accessTokenTtlSeconds)
        || candidates[0].accessTokenTtlSeconds < 300 || candidates[0].accessTokenTtlSeconds > 600) rejected()
    return candidates[0].accessTokenTtlSeconds
  })
  // The original Edge provider uses Spring's default 60-second skew; its stock-provider contract is tested in SA.
  return (Math.max(...selected) - 60 + 1) * 1000
}

function rejected(code = 'XINGYUAN_TARGET_UNSAFE'): never {
  throw new Error(code)
}

export function readPrivateFile(path: string, root?: string, maximum = 128 * 1024): Buffer {
  if (!isAbsolute(path) || resolve(path) !== path || (root && !path.startsWith(`${root}/`))) rejected()
  try {
    for (let current = path; ; current = dirname(current)) {
      if (lstatSync(current).isSymbolicLink()) rejected()
      if (dirname(current) === current) break
    }
    const before = lstatSync(path)
    if (!before.isFile() || before.uid !== process.getuid?.() || (before.mode & 0o777) !== 0o600) rejected()
    if (before.size > maximum) rejected('XINGYUAN_INPUT_INVALID')
    const data = readFileSync(path)
    const after = lstatSync(path)
    if (data.length > maximum || before.ino !== after.ino || before.size !== after.size || before.mtimeMs !== after.mtimeMs) rejected()
    return data
  } catch (error) {
    if (error instanceof Error && /^XINGYUAN_(TARGET_UNSAFE|INPUT_INVALID)$/.test(error.message)) throw error
    rejected('XINGYUAN_INPUT_UNAVAILABLE')
  }
}

export function privateDirectory(path: string, root: string): void {
  if (path !== root && !path.startsWith(`${root}/`)) rejected()
  try {
    mkdirSync(path, { recursive: true, mode: 0o700 })
    for (let current = path; ; current = dirname(current)) {
      const value = lstatSync(current)
      if (!value.isDirectory() || value.isSymbolicLink() || value.uid !== process.getuid?.() || (value.mode & 0o777) !== 0o700) rejected()
      if (current === root) break
    }
  } catch {
    rejected()
  }
}

// JSON.parse cannot detect duplicate keys; the existing Python runtime supplies only strict decoding here.
export function readIntegrationJson(data: Buffer): Record<string, unknown> {
  if (data.byteLength > 2 * 1024 * 1024) rejected('XINGYUAN_INPUT_INVALID')
  try {
    const normalized = execFileSync('python3', ['-I', '-c', [
      'import json,sys',
      'def unique(pairs):',
      ' value={}',
      ' for key,item in pairs:',
      '  if key in value: raise ValueError()',
      '  value[key]=item',
      ' return value',
      'value=json.loads(sys.stdin.buffer.read(),object_pairs_hook=unique,parse_constant=lambda value: (_ for _ in ()).throw(ValueError()))',
      'def safe(value):',
      ' if type(value) is int and abs(value)>9007199254740991: return str(value)',
      ' if isinstance(value,list): return [safe(item) for item in value]',
      ' if isinstance(value,dict): return {key:safe(item) for key,item in value.items()}',
      ' return value',
      'sys.stdout.write(json.dumps(safe(value),allow_nan=False,ensure_ascii=False))',
    ].join('\n')], { input: data, timeout: 2000, maxBuffer: 8 * 1024 * 1024, stdio: ['pipe', 'pipe', 'pipe'] })
    const value: unknown = JSON.parse(normalized.toString('utf8'))
    if (!value || typeof value !== 'object' || Array.isArray(value)) rejected('XINGYUAN_INPUT_INVALID')
    return value as Record<string, unknown>
  } catch {
    rejected('XINGYUAN_INPUT_INVALID')
  }
}

function object(value: unknown): Record<string, unknown> {
  if (!value || typeof value !== 'object' || Array.isArray(value)) rejected('XINGYUAN_INPUT_INVALID')
  return value as Record<string, unknown>
}

function text(value: unknown): string {
  if (typeof value !== 'string' || !value || /[\u0000-\u001f]/.test(value)) rejected('XINGYUAN_INPUT_INVALID')
  return value
}

export function loadIntegrationInputs(): IntegrationInputs {
  if (process.env.XINGYUAN_ISOLATED_INTEGRATION !== 'true') rejected()
  if (process.env.DEBUG || process.env.PWDEBUG || process.env.NODE_TLS_REJECT_UNAUTHORIZED === '0') rejected()
  const reference = process.env.XINGYUAN_INTEGRATION_MANIFEST
  if (!reference) rejected('XINGYUAN_INPUT_UNAVAILABLE')
  const bytes = readPrivateFile(reference)
  const manifest = readIntegrationJson(bytes)
  const fields = ['schemaVersion', 'runId', 'repoRoot', 'runtimeRoot', 'publicOrigin', 'postgres', 'redis', 'databases',
    'platformStoragePartitionId', 'tenantCodes', 'userRecipe', 'authorizationRecipe', 'processes', 'artifacts', 'secretFiles',
    'deadlines', 'catalogFiles', 'policyFiles']
  if (Object.keys(manifest).length !== fields.length || fields.some(key => !(key in manifest))) rejected('XINGYUAN_INPUT_INVALID')
  const runId = text(manifest.runId)
  if (!/^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/.test(runId)) rejected()
  const root = join(repository, `target/xingyuan-integration-${runId}`)
  if (manifest.schemaVersion !== 1 || manifest.repoRoot !== repository || manifest.runtimeRoot !== root
      || manifest.publicOrigin !== 'https://127.0.0.1:28443' || !reference.startsWith(`${root}/`)) rejected()
  privateDirectory(root, root)
  const digest = createHash('sha256').update(bytes).digest('hex')
  const registry = readIntegrationJson(readPrivateFile(join(root, 'secrets/runtime-registry.json'), join(root, 'secrets'), 2 * 1024 * 1024))
  if (registry.runId !== runId || registry.manifestSha256 !== digest || registry.publicOrigin !== manifest.publicOrigin) rejected()
  const tenants = object(registry.tenantIds)
  const subjects = object(registry.subjects)
  const tenantIds = { default: text(tenants.default), 'tenant-b': text(tenants['tenant-b']) }
  if (!Object.values(tenantIds).every(value => /^[1-9][0-9]{0,18}$/.test(value)) || tenantIds.default === tenantIds['tenant-b']) rejected()
  const secretFiles = Object.fromEntries(Object.entries(object(manifest.secretFiles)).map(([key, value]) => [key, text(value)]))
  const caFile = secretFiles['tls-ca-cert']
  const leafFile = secretFiles['tls-server-cert']
  if (!caFile || !leafFile || process.env.NODE_EXTRA_CA_CERTS !== caFile) rejected('XINGYUAN_INPUT_UNAVAILABLE')
  let certificatePin: string
  try {
    const ca = new X509Certificate(readPrivateFile(caFile, join(root, 'secrets')))
    const leaf = new X509Certificate(readPrivateFile(leafFile, join(root, 'secrets')))
    const now = Date.now()
    if (!ca.ca || !ca.verify(ca.publicKey) || !leaf.verify(ca.publicKey) || leaf.issuer !== ca.subject
        || leaf.checkIP('127.0.0.1') !== '127.0.0.1'
        || [ca, leaf].some(value => Date.parse(value.validFrom) > now || Date.parse(value.validTo) <= now)) rejected()
    certificatePin = createHash('sha256').update(leaf.publicKey.export({ type: 'spki', format: 'der' })).digest('base64')
  } catch {
    rejected()
  }
  if (!Array.isArray(registry.clients)) rejected('XINGYUAN_INPUT_INVALID')
  const clients: IntegrationClientFact[] = []
  const mapping = object(registry.aliases)
  const registrations = new Set<string>()
  for (const alias of aliases) {
    const binding = object(mapping[alias])
    const issuers = object(binding.issuerRegistrationIds)
    if (Object.keys(issuers).length !== 2) rejected()
    for (const realmCode of ['default', 'tenant-b'] as const) {
      const issuer = `${manifest.publicOrigin}/tenants/${tenantIds[realmCode]}`
      const registrationId = text(issuers[issuer])
      if (!/^[A-Za-z0-9._-]{1,128}$/.test(registrationId)) rejected()
      const candidates = registry.clients.map(object).filter(value => value.alias === alias && value.realmCode === realmCode)
      if (candidates.length !== 1 || registrations.has(registrationId)) rejected()
      const client = candidates[0]
      if (client.issuer !== issuer || client.registrationId !== registrationId || client.clientType !== 'CONFIDENTIAL'
          || client.purpose !== 'USER' || client.status !== 'ACTIVE'
          || !/^[1-9][0-9]{0,18}$/.test(String(client.clientPk))
          || !Number.isInteger(client.accessTokenTtlSeconds) || (client.accessTokenTtlSeconds as number) < 300 || (client.accessTokenTtlSeconds as number) > 600
          || !Array.isArray(client.resourceUris) || client.resourceUris.length !== 1
          || !Array.isArray(client.allowedScopes) || client.allowedScopes.length > 64) rejected()
      if (realmCode === 'default' && binding.defaultRegistrationId !== registrationId) rejected()
      if (BigInt(String(client.clientPk)) > 9223372036854775807n) rejected()
      registrations.add(registrationId)
      clients.push({ clientPk: String(client.clientPk), clientId: text(client.clientId), realmCode, issuer, registrationId, alias,
        resourceUris: client.resourceUris.map(text), allowedScopes: client.allowedScopes.map(text), accessTokenTtlSeconds: client.accessTokenTtlSeconds as number })
    }
  }
  for (const alias of aliases) {
    const pair = clients.filter(value => value.alias === alias)
    if (pair.length !== 2 || pair[0].clientId === pair[1].clientId || pair[0].clientPk === pair[1].clientPk
        || pair[0].resourceUris[0] !== pair[1].resourceUris[0]) rejected()
  }
  if (!Array.isArray(registry.cliResources) || registry.cliResources.length > 32 || !Array.isArray(registry.resources)) rejected()
  const publicClients: IntegrationPublicClientFact[] = []
  const cliKeys = new Set<string>()
  const cliIds = new Set<string>()
  const registered = registry.clients.map(object)
  const resources = registry.resources.map(object)
  for (const raw of registry.cliResources) {
    const binding = object(raw)
    const resourceKey = text(binding.resourceKey)
    if (!/^[a-z][a-z0-9-]{2,63}$/.test(resourceKey) || cliKeys.has(resourceKey)) rejected()
    cliKeys.add(resourceKey)
    const found = resources.filter(value => value.resourceServerId === binding.resourceServerId && value.status === 'ACTIVE')
    if (found.length !== 1) rejected()
    const resourceUri = text(found[0].resourceUri)
    const readPath = text(binding.readPath)
    if (!readPath.startsWith('/') || readPath.startsWith('//') || /\\|%2e|%2f|%5c|#|\.\./i.test(readPath)) rejected()
    const ids = object(binding.clientIds)
    if (Object.keys(ids).length !== 2) rejected()
    for (const realmCode of ['default', 'tenant-b'] as const) {
      const clientId = text(ids[realmCode])
      const selection = registered.filter(value => value.clientId === clientId && value.realmCode === realmCode)
      if (selection.length !== 1 || cliIds.has(`${realmCode}:${clientId}`)) rejected()
      cliIds.add(`${realmCode}:${clientId}`)
      const current = selection[0]
      const issuer = `${manifest.publicOrigin}/tenants/${tenantIds[realmCode]}`
      if (current.clientType !== 'PUBLIC' || current.purpose !== 'USER' || current.status !== 'ACTIVE' || current.issuer !== issuer
          || !Array.isArray(current.resourceUris) || current.resourceUris.length !== 1 || current.resourceUris[0] !== resourceUri
          || current.refreshTokenTtlSeconds !== 0 || !Array.isArray(current.redirectUris) || current.redirectUris.length !== 1
          || !Array.isArray(current.allowedScopes) || !current.allowedScopes.includes('openid') || current.allowedScopes.includes('offline_access')
          || !/^[1-9][0-9]{0,18}$/.test(String(current.clientPk)) || BigInt(String(current.clientPk)) > 9223372036854775807n) rejected()
      const redirectUri = text(current.redirectUris[0])
      let redirect: URL
      try { redirect = new URL(redirectUri) } catch { rejected() }
      if (redirect.protocol !== 'https:' || redirect.hostname !== '127.0.0.1' || redirect.username || redirect.password
          || redirect.search || redirect.hash || !redirect.port || Number(redirect.port) < 1024 || redirect.port === '28443') rejected()
      publicClients.push({ resourceKey, readPath, realmCode, clientId, clientPk: String(current.clientPk), issuer,
        redirectUri, resourceUri, scopes: current.allowedScopes.map(text) })
    }
  }
  const seconds = object(manifest.deadlines).httpSeconds
  if (!Number.isInteger(seconds) || (seconds as number) < 1 || (seconds as number) > 30) rejected()
  const evidenceDirectory = process.env.XINGYUAN_VERIFICATION_EVIDENCE_DIRECTORY ?? join(root, 'evidence')
  const relativeEvidence = evidenceDirectory.slice(join(root, 'evidence').length)
  if (evidenceDirectory !== join(root, 'evidence') && (!evidenceDirectory.startsWith(`${join(root, 'evidence')}/`)
    || !/^\/verify-[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/.test(relativeEvidence))) rejected()
  privateDirectory(evidenceDirectory, root)
  privateDirectory(join(root, 'browser-profiles'), root)
  privateDirectory(join(root, 'artifacts/browser'), root)
  const runtimeProbe = runtimeProbeInputs(manifest, root)
  return { runId, root, evidenceDirectory, origin: text(manifest.publicOrigin), manifestSha256: digest, certificatePin,
    tenantIds, subjects: { admin: text(subjects.admin), viewer: text(subjects.viewer) }, secretFiles, clients, publicClients,
    runtimeProbe, timeoutMs: (seconds as number) * 1000 }
}

const discovery = process.argv.includes('--list')
const grepIndex = process.argv.indexOf('--grep')
const cliSelection = grepIndex >= 0 && Boolean(process.argv[grepIndex + 1]?.includes('CLI-006'))
const inputs = discovery ? undefined : loadIntegrationInputs()
export function runtimeProbeInputs(manifest: Record<string, unknown>, root: string): { fixtureSha256: string; artifactSha256: string } | undefined {
  if (!Array.isArray(manifest.policyFiles)) rejected('XINGYUAN_INPUT_INVALID')
  const expectedPath = join(root, 'artifacts/task-runtime-probe.jar')
  const candidates = manifest.policyFiles.map(object).filter(value => typeof value.path === 'string'
    && value.path.endsWith('/task-runtime-probe.jar'))
  if (candidates.length === 0) return undefined
  if (candidates.length !== 1 || candidates[0].path !== expectedPath) rejected()
  const fixtureSha256 = text(candidates[0].sha256)
  const artifactSha256 = text(object(object(manifest.artifacts).edge).sha256)
  if (![fixtureSha256, artifactSha256].every(value => /^[0-9a-f]{64}$/.test(value))
      || createHash('sha256').update(readPrivateFile(expectedPath, root, 2 * 1024 * 1024)).digest('hex') !== fixtureSha256) rejected()
  return { fixtureSha256, artifactSha256 }
}

export interface CanonicalSessionProof {
  bindingReference: string
  issuer: string
  tenantId: string
  generation: number
  originalCreatedAt: string
  absoluteExpiresAt: string
}

export function canonicalSessionProof(value: Record<string, unknown>, expected: {
  runId: string; manifestSha256: string; fixtureSha256: string; artifactSha256: string; nonce: string;
  issuer: string; tenantId: string; issuedAt: string; expiresAt: string
}): CanonicalSessionProof {
  const fields = ['schemaVersion', 'runId', 'manifestSha256', 'role', 'fixtureSha256', 'artifactSha256',
    'nonce', 'operation', 'status', 'completedAt', 'result']
  const completed = text(value.completedAt)
  if (Object.keys(value).length !== fields.length || fields.some(key => !(key in value))
      || value.schemaVersion !== 1 || value.runId !== expected.runId || value.manifestSha256 !== expected.manifestSha256
      || value.role !== 'edge' || value.fixtureSha256 !== expected.fixtureSha256 || value.artifactSha256 !== expected.artifactSha256
      || value.nonce !== expected.nonce || value.operation !== 'EDGE_BINDING' || value.status !== 'COMPLETED'
      || !completed.endsWith('Z') || !Number.isFinite(Date.parse(completed))
      || Date.parse(completed) < Date.parse(expected.issuedAt) || Date.parse(completed) > Date.parse(expected.expiresAt)) rejected('XINGYUAN_VERIFICATION_FAILED')
  const result = object(value.result)
  const resultFields = ['bindingReference', 'issuer', 'tenantId', 'generation', 'originalCreatedAt', 'absoluteExpiresAt']
  const createdAt = text(result.originalCreatedAt), absoluteExpiry = text(result.absoluteExpiresAt)
  if (Object.keys(result).length !== resultFields.length || resultFields.some(key => !(key in result))
      || typeof result.bindingReference !== 'string' || !/^[0-9a-f]{64}$/.test(result.bindingReference)
      || result.issuer !== expected.issuer || result.tenantId !== expected.tenantId
      || typeof result.generation !== 'number' || !Number.isSafeInteger(result.generation) || result.generation < 1
      || !createdAt.endsWith('Z') || !absoluteExpiry.endsWith('Z')
      || !Number.isFinite(Date.parse(createdAt)) || !Number.isFinite(Date.parse(absoluteExpiry))
      || Date.parse(createdAt) > Date.parse(completed) || Date.parse(absoluteExpiry) <= Date.parse(completed)) rejected('XINGYUAN_VERIFICATION_FAILED')
  return result as unknown as CanonicalSessionProof
}

export default defineConfig({
  testDir: './e2e/integration',
  testMatch: '**/xingyuan-identity-integration.spec.ts',
  fullyParallel: false,
  workers: 1,
  retries: 0,
  grep: cliSelection ? /CLI-006:/ : /LIVE-/,
  timeout: 900_000,
  reporter: [['list']],
  outputDir: inputs ? join(inputs.root, 'artifacts/browser') : join(repository, '.runtime/xingyuan-plan-execution/browser-discovery-unused'),
  use: {
    baseURL: 'https://127.0.0.1:28443',
    headless: true,
    trace: 'off',
    screenshot: 'off',
    video: 'off',
    ignoreHTTPSErrors: false,
    actionTimeout: 30_000,
    navigationTimeout: 30_000,
    launchOptions: { args: inputs ? [`--ignore-certificate-errors-spki-list=${inputs.certificatePin}`] : [] },
  },
})
