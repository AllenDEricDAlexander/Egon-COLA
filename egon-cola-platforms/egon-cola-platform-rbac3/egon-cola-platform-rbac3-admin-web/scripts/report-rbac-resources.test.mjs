import assert from 'node:assert/strict'
import { test } from 'node:test'
import { canonicalChecksum, projectReport, reportResources } from './report-rbac-resources.mjs'

const definitions = [
  { kind: 'ROUTE', code: 'iam.roles', name: '角色', permission: 'system:role:read', path: '/iam/roles', componentKey: 'rbac3-role-graph', order: 10 },
  { kind: 'FIELD', code: 'iam.roles.name', name: '角色名称', permission: 'system:role:read', resourceCode: 'iam.roles', fieldCode: 'name', jsonPath: 'name', dataType: 'STRING' },
]

test('projects local definitions and computes the server-compatible checksum', () => {
  const report = projectReport(definitions, 'build-1', 2)
  assert.equal(report.resources.length, 1)
  assert.equal(report.fields.length, 1)
  assert.equal(report.checksum, canonicalChecksum(report))
  assert.equal(report.resources[0].permissionCode, 'system:role:read')
})

test('reports through the CI endpoint without exposing a browser client', async () => {
  let call
  const result = await reportResources({
    baseUrl: 'https://gateway.example/',
    businessCode: 'platform',
    applicationCode: 'rbac3-admin',
    serviceAccessToken: 'short-lived',
    buildId: 'build-1',
    expectedApplicationVersion: 0,
    definitionsPath: new URL('../src/app/resourceDefinitions.json', import.meta.url),
    fetcher: async (url, init) => {
      call = { url, init }
      return new Response(JSON.stringify({ added: 1, updated: 0, stale: 0, unchanged: 0, pending: 1, checksum: 'sha256:test' }), { status: 200 })
    },
  })
  assert.equal(result.added, 1)
  assert.match(call.url, /\/businesses\/platform\/applications\/rbac3-admin\/frontend-resources$/)
  assert.equal(call.init.method, 'PUT')
  assert.equal(call.init.headers.Authorization, 'Bearer short-lived')
})

test('rejects a non-success response so CI can block release', async () => {
  await assert.rejects(
    reportResources({
      baseUrl: 'https://gateway.example',
      businessCode: 'platform',
      applicationCode: 'rbac3-admin',
      serviceAccessToken: 'short-lived',
      buildId: 'build-1',
      expectedApplicationVersion: 0,
      definitionsPath: new URL('../src/app/resourceDefinitions.json', import.meta.url),
      fetcher: async () => new Response(JSON.stringify({ message: 'conflict' }), { status: 409 }),
    }),
    /HTTP 409/,
  )
})
