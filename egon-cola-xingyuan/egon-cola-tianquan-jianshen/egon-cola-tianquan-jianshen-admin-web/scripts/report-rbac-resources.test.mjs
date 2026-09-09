import assert from 'node:assert/strict'
import { test } from 'node:test'
import { canonicalChecksum, projectReport, reportResources } from './report-rbac-resources.mjs'

const definitions = [
  { kind: 'ROUTE', code: 'iam.roles', name: '角色', suggestedPermissionCode: 'system:role:read', apiResourceCodes: ['iam.api.roles.list'], path: '/iam/roles', componentKey: 'tianquan-jianshen-role-graph', order: 10 },
  { kind: 'FIELD', code: 'iam.roles.name', name: '角色名称', suggestedPermissionCode: 'system:role:read', resourceCode: 'iam.roles', fieldCode: 'name', jsonPath: 'name', dataType: 'STRING' },
]

test('projects local definitions and computes the server-compatible checksum', () => {
  const report = projectReport(definitions, 'build-1', 2)
  assert.equal(report.resources.length, 1)
  assert.equal(report.fields.length, 1)
  assert.equal(report.checksum, canonicalChecksum(report))
  assert.equal(report.resources[0].suggestedPermissionCode, 'system:role:read')
  assert.deepEqual(report.resources[0].apiResourceCodes, ['iam.api.roles.list'])
  assert.equal(Object.hasOwn(report.resources[0], 'permissionCode'), false)
  assert.equal(Object.hasOwn(report.resources[0], 'permission'), false)
})

test('rejects the removed permission field instead of falling back to it', () => {
  assert.throws(
    () => projectReport([{ kind: 'ROUTE', code: 'legacy', name: 'Legacy', permission: 'legacy:read' }], 'build-1', 0),
    /legacy permission field/,
  )
})

test('reports through the CI endpoint without exposing a browser client', async () => {
  let call
  const result = await reportResources({
    baseUrl: 'https://yuheng.example/',
    businessCode: 'xingyuan',
    applicationCode: 'tianquan-jianshen-admin',
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
  assert.match(call.url, /\/registration\/businesses\/xingyuan\/applications\/tianquan-jianshen-admin\/frontend-resources$/)
  assert.equal(call.init.method, 'PUT')
  assert.equal(call.init.headers.Authorization, 'Bearer short-lived')
})

test('rejects a non-success response so CI can block release', async () => {
  await assert.rejects(
    reportResources({
      baseUrl: 'https://yuheng.example',
      businessCode: 'xingyuan',
      applicationCode: 'tianquan-jianshen-admin',
      serviceAccessToken: 'short-lived',
      buildId: 'build-1',
      expectedApplicationVersion: 0,
      definitionsPath: new URL('../src/app/resourceDefinitions.json', import.meta.url),
      fetcher: async () => new Response(JSON.stringify({ message: 'conflict' }), { status: 409 }),
    }),
    /HTTP 409/,
  )
})
