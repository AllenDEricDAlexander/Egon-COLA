import { afterEach, describe, expect, it, vi } from 'vitest'
import { loadManifest } from './loader'

const manifest = (overrides: Record<string, unknown> = {}) => ({
  key: 'tianquan-shoubing',
  displayName: 'Tianquan-Shoubing',
  url: '/children/tianquan-shoubing/assets/index.js',
  standaloneUrl: '/tianquan-shoubing/overview',
  version: '5.3.2',
  contractVersion: 'xingyuan-1',
  compatibleHostRange: '>=5.0.0 <6.0.0',
  requiredCapabilities: ['tianquan-shoubing:read'],
  ...overrides,
})

const jsonResponse = (body: unknown, status = 200): Response => new Response(
  JSON.stringify(body),
  { status, headers: { 'Content-Type': 'application/json' } },
)

afterEach(() => {
  vi.unstubAllGlobals()
  vi.unstubAllEnvs()
})

describe('loadManifest', () => {
  it('accepts a compatible manifest and keeps the standalone fallback', async () => {
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse(manifest()))
    vi.stubGlobal('fetch', fetchMock)

    await expect(loadManifest('tianquan-shoubing', 'local', new AbortController().signal)).resolves.toMatchObject({
      key: 'tianquan-shoubing',
      url: '/children/tianquan-shoubing/assets/index.js',
      standaloneUrl: '/tianquan-shoubing/overview',
    })
    expect(fetchMock).toHaveBeenCalledWith('/portal-manifest/local.json', {
      credentials: 'include',
      signal: expect.any(AbortSignal),
    })
    expect(new Headers(fetchMock.mock.calls[0][1].headers).has('Authorization')).toBe(false)
  })

  it('selects a xingyuan entry and allows explicit local child origins', async () => {
    vi.stubEnv('VITE_PORTAL_ALLOW_LOCAL_CHILD_ORIGINS', 'true')
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(jsonResponse({
      tianquan-shoubing: manifest(),
      yuheng: manifest({
        key: 'yuheng',
        url: 'http://localhost:18141/',
        standaloneUrl: 'http://localhost:18141/dashboard',
        requiredCapabilities: ['yuheng:read'],
      }),
    })))

    await expect(loadManifest('yuheng', 'local', new AbortController().signal))
      .resolves.toMatchObject({
        key: 'yuheng',
        url: 'http://localhost:18141/',
        standaloneUrl: 'http://localhost:18141/dashboard',
      })
  })

  it('rejects invalid manifests before a child can mount', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(jsonResponse(manifest({ url: '' }))))

    await expect(loadManifest('tianquan-shoubing', 'local', new AbortController().signal))
      .rejects.toMatchObject({ code: 'MANIFEST_INVALID' })
  })

  it('rejects a manifest outside the Portal host version range', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(jsonResponse(
      manifest({ compatibleHostRange: '>=9.0.0' }),
    )))

    await expect(loadManifest('tianquan-shoubing', 'local', new AbortController().signal))
      .rejects.toMatchObject({ code: 'MANIFEST_VERSION_UNSUPPORTED' })
  })

  it('rejects unsafe asset URLs and drops unknown sensitive fields', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(jsonResponse(manifest({
      url: 'https://untrusted.example/child.js',
      accessToken: 'must-not-cross-the-boundary',
    }))))

    await expect(loadManifest('tianquan-shoubing', 'local', new AbortController().signal))
      .rejects.toMatchObject({ code: 'MANIFEST_URL_NOT_ALLOWED' })
  })

  it('preserves AbortError without converting it into a mount failure', async () => {
    const abort = new DOMException('aborted', 'AbortError')
    vi.stubGlobal('fetch', vi.fn().mockRejectedValue(abort))

    await expect(loadManifest('tianquan-shoubing', 'local', new AbortController().signal)).rejects.toBe(abort)
  })
})
