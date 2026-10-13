import type { ManifestErrorCode, PlatformKey, PlatformManifest } from './types'

export const PORTAL_HOST_VERSION = '5.3.2'

export class ManifestLoadError extends Error {
  readonly code: ManifestErrorCode
  readonly status?: number

  constructor(code: ManifestErrorCode, message: string, status?: number) {
    super(message)
    this.name = 'ManifestLoadError'
    this.code = code
    this.status = status
  }
}

type Semver = [number, number, number]

const semverPattern = /^(\d+)\.(\d+)\.(\d+)(?:-[0-9A-Za-z.-]+)?(?:\+[0-9A-Za-z.-]+)?$/

const parseSemver = (value: string): Semver | undefined => {
  const match = semverPattern.exec(value.trim())
  if (!match) {
    return undefined
  }
  return [Number(match[1]), Number(match[2]), Number(match[3])]
}

const compareSemver = (left: Semver, right: Semver): number => {
  for (let index = 0; index < left.length; index += 1) {
    if (left[index] !== right[index]) {
      return left[index] - right[index]
    }
  }
  return 0
}

const satisfiesRange = (version: string, range: string): boolean => {
  const parsedVersion = parseSemver(version)
  if (!parsedVersion || !range.trim()) {
    return false
  }

  return range.split('||').some((alternative) => alternative.trim().split(/\s+/).every((expression) => {
    const match = /^(>=|<=|>|<|=)?\s*(\d+\.\d+\.\d+(?:-[0-9A-Za-z.-]+)?(?:\+[0-9A-Za-z.-]+)?)$/.exec(expression)
    if (!match) {
      return false
    }
    const expected = parseSemver(match[2])
    if (!expected) {
      return false
    }
    const comparison = compareSemver(parsedVersion, expected)
    switch (match[1] ?? '=') {
      case '>':
        return comparison > 0
      case '>=':
        return comparison >= 0
      case '<':
        return comparison < 0
      case '<=':
        return comparison <= 0
      default:
        return comparison === 0
    }
  }))
}

const manifestUrl = (environment: string): string => `/portal-manifest/${encodeURIComponent(environment)}.json`

const portalOrigin = (): string => globalThis.location?.origin ?? 'http://portal.local'

const localChildOriginsEnabled = (): boolean => (
  import.meta.env.VITE_PORTAL_ALLOW_LOCAL_CHILD_ORIGINS === 'true'
)

const isAllowedUrl = (value: string): boolean => {
  if (!value.trim() || value.includes('\\')) {
    return false
  }

  try {
    const origin = new URL(portalOrigin())
    const parsed = new URL(value, portalOrigin())
    if (parsed.origin === origin.origin && parsed.protocol === origin.protocol) {
      return true
    }
    return localChildOriginsEnabled()
      && parsed.protocol === origin.protocol
      && parsed.hostname === origin.hostname
      && (parsed.hostname === '127.0.0.1' || parsed.hostname === 'localhost')
  } catch {
    return false
  }
}

/** Validates the deployment-owned HTML entry before Wujie can load a child. */
export const validateChildUrl = (value: string, platformKey: PlatformKey, standalone = false): URL => {
  const reject = (): never => { throw new ManifestLoadError('MANIFEST_URL_NOT_ALLOWED', 'Manifest URL does not match the approved child mount') }
  if (!['tianquan-shoubing', 'tianquan-jianshen', 'tianshu', 'yuheng'].includes(platformKey) || !isAllowedUrl(value)) return reject()
  let decoded: string
  try { decoded = decodeURIComponent(value) } catch { return reject() }
  if (/[\\]/.test(decoded) || Array.from(decoded).some((character) => character.charCodeAt(0) < 32 || character.charCodeAt(0) === 127) || /%[0-9a-f]{2}/i.test(decoded)
    || decoded.split('/').some((segment) => segment === '.' || segment === '..')) return reject()
  const parsed = new URL(value, portalOrigin())
  if (parsed.username || parsed.password || parsed.search || parsed.hash
    || (import.meta.env.PROD && parsed.protocol !== 'https:')) return reject()
  const localStandaloneOrigin = parsed.origin !== portalOrigin() && localChildOriginsEnabled()
  if (localStandaloneOrigin && (standalone || parsed.pathname === '/' || parsed.pathname === '/index.html')) return parsed
  const shortKey = platformKey.replace(/^tianquan-/, '')
  const mounts = [`/apps/${platformKey}/`, `/apps/${shortKey}/`, `/children/${platformKey}/`]
  if (standalone) mounts.push(`/${platformKey}/`)
  if (mounts.some((mount) => standalone ? parsed.pathname.startsWith(mount)
    : parsed.pathname === mount || parsed.pathname === `${mount}index.html`)) return parsed
  return reject()
}

const invalid = (message: string): ManifestLoadError => new ManifestLoadError('MANIFEST_INVALID', message)

const readManifest = (value: unknown, platformKey: PlatformKey): PlatformManifest => {
  if (!value || typeof value !== 'object' || Array.isArray(value)) {
    throw invalid('Manifest must be a JSON object')
  }

  const root = value as Record<string, unknown>
  const candidate = typeof root.key === 'string'
    ? root
    : root[platformKey] as Record<string, unknown> | undefined
  if (!candidate || typeof candidate !== 'object' || Array.isArray(candidate)) {
    throw invalid('Manifest does not contain the requested xingyuan')
  }
  const requiredStrings = [
    'key',
    'displayName',
    'url',
    'standaloneUrl',
    'version',
    'contractVersion',
    'compatibleHostRange',
  ] as const

  if (requiredStrings.some((field) => typeof candidate[field] !== 'string' || !candidate[field])) {
    throw invalid('Manifest is missing a required string field')
  }
  if (candidate.key !== platformKey) {
    throw invalid('Manifest key does not match the requested xingyuan')
  }
  if (!Array.isArray(candidate.requiredCapabilities)
    || candidate.requiredCapabilities.some((capability) => typeof capability !== 'string' || !capability)) {
    throw invalid('Manifest requiredCapabilities must be a string array')
  }
  if (!parseSemver(candidate.version as string)) {
    throw invalid('Manifest version must be a semantic version')
  }
  validateChildUrl(candidate.url as string, platformKey)
  validateChildUrl(candidate.standaloneUrl as string, platformKey, true)
  if (candidate.summaryUrl !== undefined
    && (typeof candidate.summaryUrl !== 'string' || !isAllowedUrl(candidate.summaryUrl))) {
    throw new ManifestLoadError('MANIFEST_URL_NOT_ALLOWED', 'Manifest summary URL must remain same-origin')
  }

  return {
    key: candidate.key as PlatformKey,
    displayName: candidate.displayName as string,
    url: candidate.url as string,
    standaloneUrl: candidate.standaloneUrl as string,
    version: candidate.version as string,
    contractVersion: candidate.contractVersion as string,
    compatibleHostRange: candidate.compatibleHostRange as string,
    requiredCapabilities: [...candidate.requiredCapabilities as string[]],
    ...(typeof candidate.summaryUrl === 'string' ? { summaryUrl: candidate.summaryUrl } : {}),
  }
}

const isAbortError = (error: unknown): boolean => (
  (error instanceof DOMException && error.name === 'AbortError')
  || (typeof error === 'object' && error !== null && 'name' in error && error.name === 'AbortError')
)

export const loadManifest = async (
  platformKey: PlatformKey,
  environment: string,
  signal: AbortSignal,
  hostVersion = PORTAL_HOST_VERSION,
): Promise<PlatformManifest> => {
  let response: Response
  try {
    response = await fetch(manifestUrl(environment), { credentials: 'include', signal })
  } catch (error) {
    if (isAbortError(error)) {
      throw error
    }
    throw new ManifestLoadError('MANIFEST_FETCH_FAILED', 'Unable to load xingyuan manifest')
  }

  if (!response.ok) {
    throw new ManifestLoadError('MANIFEST_FETCH_FAILED', `Manifest request failed with status ${response.status}`, response.status)
  }

  let payload: unknown
  try {
    payload = await response.json()
  } catch {
    throw invalid('Manifest response is not valid JSON')
  }

  const parsed = readManifest(payload, platformKey)
  if (!satisfiesRange(hostVersion, parsed.compatibleHostRange)) {
    throw new ManifestLoadError(
      'MANIFEST_VERSION_UNSUPPORTED',
      `Platform ${parsed.version} is not compatible with Portal ${hostVersion}`,
    )
  }
  return parsed
}
