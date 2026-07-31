import { describe, expect, it } from 'vitest'
import { detectConfigFormat, prepareConfigEditor, serializeConfigEditor } from './configFormat'

const gatewayConfig = () => {
  const snapshot = {
    ruleSchemaVersion: 'v1',
    releaseId: 'release-1',
    generatedAt: '2026-07-28T00:00:00Z',
    ruleContentSha256: 'old-content-sha',
    artifactSha256: 'old-artifact-sha',
    content: { env: 'dev', routes: [] },
  }
  return {
    configKey: 'gateway.rules.active',
    valueType: 'JSON',
    configValue: JSON.stringify({
      activationSchemaVersion: 'v1',
      releaseId: 'release-1',
      mode: 'INLINE',
      ruleSchemaVersion: 'v1',
      totalSize: 1,
      ruleContentSha256: 'old-content-sha',
      artifactSha256: 'old-artifact-sha',
      inlineSnapshot: JSON.stringify(snapshot),
      chunks: [],
    }),
  }
}

describe('detectConfigFormat', () => {
  it('identifies supported configuration file formats', () => {
    expect(detectConfigFormat({ configKey: 'application.yaml', configValue: '' })).toBe('YAML')
    expect(detectConfigFormat({ configKey: 'runtime.toml', configValue: '' })).toBe('TOML')
    expect(detectConfigFormat({ configKey: 'banner.txt', configValue: '{}' })).toBe('TXT')
    expect(detectConfigFormat({ configKey: 'feature.flags', valueType: 'JSON', configValue: '{}' })).toBe('JSON')
    expect(detectConfigFormat({ configKey: 'runtime', configValue: 'server:\n  port: 8080' })).toBe('YAML')
    expect(detectConfigFormat({ configKey: 'runtime', configValue: '[server]\nport = 8080' })).toBe('TOML')
  })

  it('keeps typed scalar values as text configuration', () => {
    expect(detectConfigFormat({ configKey: 'feature.enabled', valueType: 'BOOLEAN', configValue: 'true' })).toBe('TXT')
    expect(detectConfigFormat({ configKey: 'request.timeout', valueType: 'INTEGER', configValue: '200' })).toBe('TXT')
    expect(detectConfigFormat({ configKey: 'feature.flags', valueType: 'STRING', configValue: '{"enabled":true}' })).toBe('JSON')
  })
})

describe('prepareConfigEditor', () => {
  it('preserves ordinary JSON business fields', () => {
    const value = { data: { version: 1 }, metadata: { owner: 'ops' } }
    const editor = prepareConfigEditor({ configKey: 'business.json', valueType: 'JSON', configValue: JSON.stringify(value) })
    expect(editor.adapter).toBe('PLAIN')
    expect(editor.format).toBe('JSON')
    expect(JSON.parse(editor.content)).toEqual(value)
    expect(editor.content).toMatch(/"metadata": \{/)
  })

  it('exposes only Gateway inline rule content', () => {
    const editor = prepareConfigEditor(gatewayConfig())
    expect(editor.adapter).toBe('GATEWAY_INLINE_RULE')
    expect(editor.format).toBe('JSON')
    expect(editor.content).toBe('{\n  "env": "dev",\n  "routes": []\n}')
    expect(editor.notice).toMatch(/Gateway/)
    expect(editor.content).not.toMatch(/artifactSha256|generatedAt|releaseId/)
  })

  it('does not unwrap an inconsistent Gateway-like object', () => {
    const config = gatewayConfig()
    const activation = JSON.parse(config.configValue) as Record<string, unknown>
    const snapshot = JSON.parse(String(activation.inlineSnapshot)) as Record<string, unknown>
    snapshot.releaseId = 'different-release'
    activation.inlineSnapshot = JSON.stringify(snapshot)
    const editor = prepareConfigEditor({ ...config, configValue: JSON.stringify(activation) })
    expect(editor.adapter).toBe('PLAIN')
    expect(editor.content).toMatch(/"inlineSnapshot":/)
  })
})

describe('serializeConfigEditor', () => {
  it('rebuilds Gateway checksums and activation metadata', async () => {
    const editor = prepareConfigEditor(gatewayConfig())
    const serialized = await serializeConfigEditor(editor, '{\n  "env": "prod",\n  "routes": []\n}')
    const activation = JSON.parse(serialized) as Record<string, unknown>
    const snapshot = JSON.parse(String(activation.inlineSnapshot)) as Record<string, unknown>
    expect(snapshot.content).toEqual({ env: 'prod', routes: [] })
    expect(snapshot.ruleContentSha256).toBe('a270803a31aceb109ad9e65bd4993c02049e2717798dc3be95b462b81c47167b')
    expect(snapshot.artifactSha256).toBe('784dc9c7bb589bdb5ab542f6170f0fa5751b89ad6d0a1035b2dd7d5902889303')
    expect(activation.ruleContentSha256).toBe(snapshot.ruleContentSha256)
    expect(activation.artifactSha256).toBe(snapshot.artifactSha256)
    expect(activation.totalSize).toBe(295)
    expect(activation.releaseId).toBe('release-1')
    expect(activation.activationSchemaVersion).toBe('v1')
    expect(activation.chunks).toEqual([])
    expect(snapshot.generatedAt).toBe('2026-07-28T00:00:00Z')
    expect(snapshot.releaseId).toBe('release-1')
  })

  it('rejects malformed JSON instead of storing it', async () => {
    const editor = prepareConfigEditor({ configKey: 'feature.json', valueType: 'JSON', configValue: '{}' })
    await expect(serializeConfigEditor(editor, '{ invalid')).rejects.toThrow(/JSON 配置格式无效/)
  })
})
