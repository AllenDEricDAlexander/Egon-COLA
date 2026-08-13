import { expect, test, type Page, type Route } from '@playwright/test'

const prefix = `e2e-${Date.now()}`
const allCapabilities = [
  'gateway:read',
  'gateway:groups:write',
  'gateway:applications:write',
  'gateway:credentials:write',
  'gateway:catalog:write',
  'gateway:drafts:write',
  'gateway:releases:write',
]

const json = (route: Route, body: unknown, status = 200) =>
  route.fulfill({
    status,
    contentType: 'application/json',
    body: JSON.stringify(body),
  })

const session = (capabilities = allCapabilities) => ({
  actorId: `${prefix}-actor`,
  displayName: 'Gateway E2E',
  actorType: 'USER',
  capabilities,
  roles: ['gateway-admin'],
  expiresAt: '2099-01-01T00:00:00Z',
})

const accessToken = (nonce?: string) => [
  Buffer.from(JSON.stringify({ alg: 'none', typ: 'JWT' })).toString('base64url'),
  Buffer.from(JSON.stringify({
    sub: `${prefix}-actor`,
    exp: 4_102_444_800,
    ...(nonce ? { nonce } : {}),
  })).toString('base64url'),
  'e2e-signature',
].join('.')

const group = {
  id: `${prefix}-group-id`,
  gatewayGroupCode: `${prefix}-group`,
  displayName: 'E2E Gateway',
  env: 'dev',
  namespace: 'default',
  enabled: true,
  revision: 1,
}

const application = {
  id: `${prefix}-application-id`,
  bizCode: 'e2e',
  applicationCode: `${prefix}-application`,
  displayName: 'E2E Application',
  env: 'dev',
  namespace: 'default',
  revision: 1,
}

const emptyDraft = {
  gatewayGroupId: group.id,
  revision: 1,
  status: 'EDITING',
  routes: [],
  policies: [],
  updatedAt: '2026-07-25T00:00:00Z',
}

const release = {
  releaseId: `${prefix}-release`,
  gatewayGroupId: group.id,
  draftRevision: 1,
  status: 'SUCCESS',
  partialApplied: false,
  validationReport: { valid: true, errors: [], warnings: [] },
  structuredDiff: {},
  changeReason: 'E2E publish',
  changeId: `${prefix}-change`,
  createdAt: '2026-07-25T00:00:00Z',
  updatedAt: '2026-07-25T00:00:01Z',
  attempts: [{
    attemptNo: 1,
    status: 'SUCCESS',
    startedAt: '2026-07-25T00:00:00Z',
    completedAt: '2026-07-25T00:00:01Z',
    targets: [{
      instanceId: 'engine-1',
      leaseId: 'lease-1',
      status: 'SUCCESS',
      appliedVersion: 1,
      appliedArtifactSha256: 'a'.repeat(64),
      observedAt: '2026-07-25T00:00:01Z',
    }],
  }],
}

const authenticate = async (
  page: Page,
  capabilities = allCapabilities,
) => {
  await page.route('http://127.0.0.1:18120/oauth2/token', (route) => json(route, {
    access_token: accessToken(),
    token_type: 'Bearer',
    expires_in: 3_600,
  }))
  await page.route('**/api/v1/gateway/admin/session', (route) =>
    json(route, session(capabilities)))
}

const installReadFixtures = async (page: Page) => {
  await page.route('**/api/v1/gateway/admin/scopes', (route) => json(route, [{
    bindingId: `${prefix}-scope`,
    bizCode: 'e2e',
    appCode: application.applicationCode,
    appName: application.displayName,
    env: 'dev',
    namespace: 'default',
    connected: true,
  }]))
  await page.route('**/api/v1/gateway/admin/dashboard**', (route) => json(route, {
    gatewayGroups: 1,
    readyEngines: 2,
    totalEngines: 2,
    inconsistentGroups: 0,
    activeProviders: 1,
    abnormalProviders: 0,
    releaseSuccessRate: 1,
    requestSeries: [],
    protocolCalls: [],
  }))
  await page.route('**/api/v1/gateway/admin/gateway-groups*', (route) =>
    json(route, [group]))
  await page.route(`**/api/v1/gateway/admin/gateway-groups/${group.id}`, (route) =>
    json(route, group))
  await page.route(`**/api/v1/gateway/admin/gateway-groups/${group.id}/engine-nodes`, (route) =>
    json(route, {
      value: [{
        appCode: 'gateway',
        env: 'dev',
        namespace: 'default',
        instanceId: 'engine-1',
        leaseId: 'lease-1',
        leaseRole: 'CONFIG_CLIENT',
        status: 'READY',
        registeredAt: '2026-07-25T00:00:00Z',
        lastHeartbeatAt: '2026-07-25T00:00:01Z',
        expireAt: '2099-01-01T00:00:00Z',
      }],
      observedAt: '2026-07-25T00:00:01Z',
      source: 'DDC_CONFIG_CLIENT',
      stale: false,
    }))
  await page.route(`**/api/v1/gateway/admin/gateway-groups/${group.id}/runtime-consistency`, (route) =>
    json(route, {
      targetReleaseId: release.releaseId,
      targetReleaseStatus: 'SUCCESS',
      engineNodeCount: 2,
      readyEngineNodeCount: 2,
      consistent: true,
      observedAt: '2026-07-25T00:00:01Z',
      source: 'DDC_CONFIG_CLIENT',
      stale: false,
    }))
  await page.route('**/api/v1/gateway/admin/applications*', (route) =>
    json(route, [application]))
  await page.route(`**/api/v1/gateway/admin/applications/${application.id}/catalog`, (route) =>
    json(route, {
      applicationId: application.id,
      businessDomains: [{
        id: 'business-1',
        code: 'sales',
        displayName: 'Sales',
        entityDomains: [{
          id: 'entity-1',
          code: 'order',
          displayName: 'Order',
          interfaceGroups: [{
            id: 'group-1',
            code: 'order-controller',
            displayName: 'Order Controller',
            sourceType: 'MANUAL',
            operations: [],
          }],
        }],
      }],
    }))
  await page.route(`**/api/v1/gateway/admin/gateway-groups/${group.id}/draft`, (route) =>
    json(route, emptyDraft))
  await page.route(`**/api/v1/gateway/admin/gateway-groups/${group.id}/draft/diff`, (route) =>
    json(route, { routes: [], policies: [] }))
  await page.route(`**/api/v1/gateway/admin/gateway-groups/${group.id}/releases`, (route) =>
    json(route, route.request().method() === 'POST' ? release : [release],
      route.request().method() === 'POST' ? 201 : 200))
  await page.route(`**/api/v1/gateway/admin/releases/${release.releaseId}`, (route) =>
    json(route, release))
  await page.route('**/api/v1/gateway/admin/providers/instances*', (route) =>
    json(route, {
      value: [{
        serviceKey: 'HTTP_PROVIDER:HTTP:orders::',
        protocol: 'HTTP',
        serviceName: 'orders',
        group: 'default',
        version: '1.0.0',
        instanceId: 'provider-1',
        leaseId: 'provider-lease-1',
        host: 'provider',
        port: 8080,
        status: 'REGISTERED',
        expireAt: '2099-01-01T00:00:00Z',
        observedAt: '2026-07-25T00:00:01Z',
      }],
      observedAt: '2026-07-25T00:00:01Z',
      source: 'DDC_SERVICE_REGISTRY',
      stale: false,
    }))
  await page.route('**/api/v1/gateway/admin/observability/traces*', (route) =>
    json(route, { items: [], page: 0, size: 20, total: 0 }))
  await page.route('**/api/v1/gateway/admin/audit*', (route) =>
    json(route, { items: [], page: 0, size: 20, total: 0 }))
}

test('login, logout and a later 401 clear the browser session', async ({ page }) => {
  let allowRefresh = false
  await page.route('http://127.0.0.1:18120/oauth2/authorize?**', (route) => {
    return route.fulfill({ status: 200, contentType: 'text/html', body: '<h1>Unified Identity</h1>' })
  })
  await page.route('http://127.0.0.1:18120/oauth2/token', (route) => {
    if (!allowRefresh) {
      return json(route, { error: 'invalid_grant' }, 401)
    }
    return json(route, {
      access_token: accessToken(),
      token_type: 'Bearer',
      expires_in: 3_600,
    })
  })
  await page.route('http://127.0.0.1:18120/oauth2/revoke', (route) => json(route, {}))
  await page.route('**/api/v1/gateway/admin/session', (route) =>
    json(route, session()))
  await installReadFixtures(page)
  await page.goto('/dashboard?bizCode=e2e&appCode=' + application.applicationCode + '&env=dev&namespace=default')
  await expect(page.getByRole('heading', { name: 'Gateway Admin' })).toBeVisible()
  await page.getByRole('button', { name: '使用统一身份登录' }).click()
  await expect(page).toHaveURL(/\/oauth2\/authorize\?/)
  const authorization = new URL(page.url()).searchParams
  expect(authorization.get('code_challenge_method')).toBe('S256')
  expect(authorization.get('tenant_id')).toBe('default')

  allowRefresh = true
  await page.goto('/dashboard?bizCode=e2e&appCode=' + application.applicationCode + '&env=dev&namespace=default')
  await expect(page.getByText('Gateway E2E')).toBeVisible()
  await page.getByRole('button', { name: '退出' }).click()
  await expect(page.getByRole('heading', { name: 'Gateway Admin' })).toBeVisible()

  await page.goto('/gateway-groups')
  await expect(page.getByText('Gateway E2E')).toBeVisible()
  allowRefresh = false
  await page.route('**/api/v1/gateway/admin/providers/instances*', (route) =>
    json(route, { code: 'TOKEN_EXPIRED' }, 401))
  await page.getByRole('link', { name: 'Provider' }).click()
  await expect(page.getByRole('heading', { name: 'Gateway Admin' })).toBeVisible()
})

test('server capabilities hide mutations and missing read renders 403', async ({ page }) => {
  await authenticate(page, ['gateway:read'])
  await installReadFixtures(page)
  await page.goto('/gateway-groups')
  await expect(page.getByRole('button', { name: '新建 Gateway Group' })).toBeDisabled()

  await page.unroute('**/api/v1/gateway/admin/session')
  await page.route('**/api/v1/gateway/admin/session', (route) => json(route, session([])))
  await page.reload()
  await expect(page.getByText('当前账号缺少 gateway:read 能力')).toBeVisible()
})

test('gateway group create, edit and disable use management APIs', async ({ page }) => {
  await authenticate(page)
  await installReadFixtures(page)
  const create = page.waitForRequest((request) =>
    request.url().endsWith('/gateway-groups') && request.method() === 'POST')
  await page.route('**/api/v1/gateway/admin/gateway-groups', (route) =>
    json(route, group, 201))
  await page.route(`**/api/v1/gateway/admin/gateway-groups/${group.id}/disable`, (route) =>
    json(route, { ...group, enabled: false }))
  await page.goto('/gateway-groups')
  await page.getByRole('button', { name: '新建 Gateway Group' }).click()
  await page.getByLabel('Group Code').fill(`${prefix}-new-group`)
  await page.getByLabel('名称').fill('New E2E Group')
  await page.getByLabel('Scope Binding').click()
  await page.getByText('dev / default (' + application.applicationCode + ')', { exact: true }).click()
  await page.getByRole('button', { name: /确\s*定/ }).click()
  expect((await create).postDataJSON().gatewayGroupCode).toBe(`${prefix}-new-group`)
  await page.getByRole('button', { name: /停\s*用/ }).click()
  await page.getByRole('button', { name: '确 定' }).click()
})

test('application and credential lifecycle never re-displays old secrets', async ({ page }) => {
  await authenticate(page)
  await installReadFixtures(page)
  await page.route(`**/api/v1/gateway/admin/applications/${application.id}/credentials`, (route) =>
    json(route, route.request().method() === 'POST' ? {
      id: 'credential-1',
      accessKey: 'gw_e2e',
      secret: 'shown-once',
      status: 'ACTIVE',
      validFrom: '2026-07-25T00:00:00Z',
    } : []))
  await page.goto('/applications')
  await page.getByRole('button', { name: 'Credential' }).click()
  await page.getByRole('button', { name: '签发 Credential' }).click()
  await expect(page.getByText('Secret 只显示一次')).toBeVisible()
  await page.getByText('新 Credential', { exact: true }).click()
  await expect(page.locator('.json-panel')).toContainText('shown-once')
})

test('manual three-level catalog and operation use structured forms', async ({ page }) => {
  await authenticate(page)
  await installReadFixtures(page)
  await page.route('**/manual-interface-groups', (route) => json(route, { id: 'group-2' }, 201))
  const operation = page.waitForRequest((request) =>
    request.url().includes('/manual-operations') && request.method() === 'POST')
  await page.route('**/manual-operations', (route) =>
    json(route, { operation: { id: 'operation-1' }, definitions: [] }, 201))
  await page.goto('/interface-catalog')
  await page.getByRole('button', { name: '新建 Operation' }).click()
  await page.getByLabel('接口组').click()
  await page.getByText('sales / order / order-controller').click()
  await page.getByLabel('Path').fill('/orders')
  await page.getByLabel('Provider Service').fill('orders')
  await page.getByRole('button', { name: /确\s*定/ }).click()
  expect((await operation).postDataJSON()).toMatchObject({
    protocol: 'HTTP',
    path: '/orders',
    externalAccessible: false,
  })
})

test('draft route supports structured editing and confirmed deletion', async ({ page }) => {
  await authenticate(page)
  await installReadFixtures(page)
  const draft = {
    ...emptyDraft,
    routes: [{
      routeId: 'route-1',
      operationId: 'operation-1',
      content: {
        host: 'orders.internal',
        httpMethod: 'GET',
        pathPattern: '/orders',
        accessZones: ['INTERNAL'],
        priority: 0,
      },
      enabled: true,
    }],
  }
  await page.unroute(`**/api/v1/gateway/admin/gateway-groups/${group.id}/draft`)
  await page.route(`**/api/v1/gateway/admin/gateway-groups/${group.id}/draft`, (route) =>
    json(route, draft))
  await page.route('**/draft/routes/route-1', (route) =>
    json(route, { revision: 2, resourceId: 'route-1', replayed: false }))
  await page.route('**/api/v1/gateway/admin/operations/operation-1', (route) => json(route, {
    operation: { protocol: 'HTTP', externalAccessible: false },
    definitions: [],
  }))
  await page.goto(`/gateway-groups/${group.id}/draft/routes`)
  await page.getByRole('button', { name: /编\s*辑/ }).click()
  await page.getByLabel('Path Pattern').fill('/orders/{id}')
  const update = page.waitForRequest((request) =>
    request.url().endsWith('/draft/routes/route-1') && request.method() === 'PUT')
  await page.getByRole('button', { name: /确\s*定/ }).click()
  expect((await update).postDataJSON().content.pathPattern).toBe('/orders/{id}')
})

test('policy edit and delete preserve revision controls', async ({ page }) => {
  await authenticate(page)
  await installReadFixtures(page)
  const draft = {
    ...emptyDraft,
    policies: [{
      policyId: 'policy-1',
      policyType: 'RATE_LIMIT',
      policyScope: 'ROUTE',
      content: { permitsPerSecond: 100 },
      enabled: true,
    }],
  }
  await page.unroute(`**/api/v1/gateway/admin/gateway-groups/${group.id}/draft`)
  await page.route(`**/api/v1/gateway/admin/gateway-groups/${group.id}/draft`, (route) =>
    json(route, draft))
  const deletion = page.waitForRequest((request) =>
    request.url().endsWith('/draft/policies/policy-1') && request.method() === 'DELETE')
  await page.route('**/draft/policies/policy-1', (route) =>
    json(route, { revision: 2, resourceId: 'policy-1', replayed: false }))
  await page.goto(`/gateway-groups/${group.id}/draft/policies`)
  await page.getByRole('button', { name: /删\s*除/ }).click()
  await page.getByRole('button', { name: '确 定' }).click()
  expect((await deletion).postDataJSON()).toMatchObject({
    expectedRevision: 1,
    changeReason: 'Delete policy from Gateway Admin Web',
  })
})

test('draft validation and publish create a release', async ({ page }) => {
  await authenticate(page)
  await installReadFixtures(page)
  await page.route('**/draft/validate', (route) =>
    json(route, { valid: true, errors: [], warnings: [] }))
  const publish = page.waitForRequest((request) =>
    request.url().endsWith(`/gateway-groups/${group.id}/releases`)
      && request.method() === 'POST')
  await page.goto(`/gateway-groups/${group.id}/releases`)
  await page.getByRole('button', { name: '校验并发布' }).click()
  await page.getByRole('dialog', { name: '发布确认' }).locator('textarea').fill('E2E publish')
  await page.getByRole('button', { name: /确\s*定/ }).click()
  expect((await publish).postDataJSON()).toMatchObject({
    expectedDraftRevision: 1,
    changeReason: 'E2E publish',
  })
})

test('release targets and group consistency expose every engine', async ({ page }) => {
  await authenticate(page)
  await installReadFixtures(page)
  await page.goto(`/gateway-groups/${group.id}/overview`)
  await expect(page.getByText('2 / 2')).toBeVisible()
  await page.goto(`/gateway-groups/${group.id}/releases/${release.releaseId}`)
  await expect(page.getByText('engine-1')).toBeVisible()
  await expect(page.getByText('lease-1')).toBeVisible()
})

test('provider, trace and audit queries are available without excluded products', async ({ page }) => {
  await authenticate(page)
  await installReadFixtures(page)
  await page.goto('/providers?bizCode=e2e&appCode=' + application.applicationCode + '&env=dev&namespace=default')
  await expect(page.getByText('provider-1')).toBeVisible()
  await page.goto('/observability/traces?env=dev&namespace=default')
  await expect(page.getByRole('heading', { name: '调用观测' })).toBeVisible()
  await page.goto('/audit?env=dev&namespace=default')
  await expect(page.getByRole('heading', { name: '审计日志' })).toBeVisible()
  await expect(page.getByText('Nginx')).toHaveCount(0)
  await expect(page.getByText('Nacos')).toHaveCount(0)
  await expect(page.getByText('Dubbo')).toHaveCount(0)
})
