import assert from 'node:assert/strict'
import test from 'node:test'

import {
  detectConfigFormat,
  prepareConfigEditor,
  serializeConfigEditor,
} from '../../../main/resources/static/ddc-admin/config-format.mjs'

const gatewayConfig = () => {
  const snapshot = {
    ruleSchemaVersion: 'v1',
    releaseId: 'release-1',
    generatedAt: '2026-07-28T00:00:00Z',
    ruleContentSha256: 'old-content-sha',
    artifactSha256: 'old-artifact-sha',
    content: {
      env: 'dev',
      routes: [],
    },
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

test('detectConfigFormat identifies supported configuration file formats', () => {
  assert.equal(detectConfigFormat({ configKey: 'application.yaml', configValue: '' }), 'YAML')
  assert.equal(detectConfigFormat({ configKey: 'runtime.toml', configValue: '' }), 'TOML')
  assert.equal(detectConfigFormat({ configKey: 'banner.txt', configValue: '{}' }), 'TXT')
  assert.equal(detectConfigFormat({ configKey: 'feature.flags', valueType: 'JSON', configValue: '{}' }), 'JSON')
  assert.equal(detectConfigFormat({ configKey: 'runtime', configValue: 'server:\n  port: 8080' }), 'YAML')
  assert.equal(detectConfigFormat({ configKey: 'runtime', configValue: '[server]\nport = 8080' }), 'TOML')
})

test('detectConfigFormat keeps typed scalar values as text configuration', () => {
  assert.equal(detectConfigFormat({ configKey: 'feature.enabled', valueType: 'BOOLEAN', configValue: 'true' }), 'TXT')
  assert.equal(detectConfigFormat({ configKey: 'request.timeout', valueType: 'INTEGER', configValue: '200' }), 'TXT')
  assert.equal(detectConfigFormat({ configKey: 'feature.flags', valueType: 'STRING', configValue: '{"enabled":true}' }), 'JSON')
})

test('prepareConfigEditor preserves ordinary JSON business fields', () => {
  const value = {
    data: { version: 1 },
    metadata: { owner: 'ops' },
  }

  const editor = prepareConfigEditor({
    configKey: 'business.json',
    valueType: 'JSON',
    configValue: JSON.stringify(value),
  })

  assert.equal(editor.adapter, 'PLAIN')
  assert.equal(editor.format, 'JSON')
  assert.deepEqual(JSON.parse(editor.content), value)
  assert.match(editor.content, /"metadata": \{/)
})

test('prepareConfigEditor exposes only Gateway inline rule content', () => {
  const editor = prepareConfigEditor(gatewayConfig())

  assert.equal(editor.adapter, 'GATEWAY_INLINE_RULE')
  assert.equal(editor.format, 'JSON')
  assert.equal(editor.content, '{\n  "env": "dev",\n  "routes": []\n}')
  assert.match(editor.notice, /Gateway/)
  assert.doesNotMatch(editor.content, /artifactSha256|generatedAt|releaseId/)
})

test('prepareConfigEditor does not unwrap an inconsistent Gateway-like object', () => {
  const config = gatewayConfig()
  const activation = JSON.parse(config.configValue)
  const snapshot = JSON.parse(activation.inlineSnapshot)
  snapshot.releaseId = 'different-release'
  activation.inlineSnapshot = JSON.stringify(snapshot)

  const editor = prepareConfigEditor({
    ...config,
    configValue: JSON.stringify(activation),
  })

  assert.equal(editor.adapter, 'PLAIN')
  assert.match(editor.content, /"inlineSnapshot":/)
})

test('serializeConfigEditor rebuilds Gateway checksums and activation metadata', async () => {
  const editor = prepareConfigEditor(gatewayConfig())

  const serialized = await serializeConfigEditor(
    editor,
    '{\n  "env": "prod",\n  "routes": []\n}',
  )

  const activation = JSON.parse(serialized)
  const snapshot = JSON.parse(activation.inlineSnapshot)
  assert.deepEqual(snapshot.content, { env: 'prod', routes: [] })
  assert.equal(
    snapshot.ruleContentSha256,
    'a270803a31aceb109ad9e65bd4993c02049e2717798dc3be95b462b81c47167b',
  )
  assert.equal(
    snapshot.artifactSha256,
    '784dc9c7bb589bdb5ab542f6170f0fa5751b89ad6d0a1035b2dd7d5902889303',
  )
  assert.equal(activation.ruleContentSha256, snapshot.ruleContentSha256)
  assert.equal(activation.artifactSha256, snapshot.artifactSha256)
  assert.equal(activation.totalSize, 295)
  assert.equal(activation.releaseId, 'release-1')
  assert.equal(activation.activationSchemaVersion, 'v1')
  assert.deepEqual(activation.chunks, [])
  assert.equal(snapshot.generatedAt, '2026-07-28T00:00:00Z')
  assert.equal(snapshot.releaseId, 'release-1')
})

test('serializeConfigEditor rejects malformed JSON instead of storing it', async () => {
  const editor = prepareConfigEditor({
    configKey: 'feature.json',
    valueType: 'JSON',
    configValue: '{}',
  })

  await assert.rejects(
    serializeConfigEditor(editor, '{ invalid'),
    /JSON 配置格式无效/,
  )
})
