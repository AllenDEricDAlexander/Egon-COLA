import { expect, test, type Page, type Route } from '@playwright/test'

const json = (route: Route, body: unknown, status = 200) => route.fulfill({
  status,
  contentType: 'application/json',
  body: JSON.stringify(body),
})

const scope = {
  bizCode: 'retail',
  appCode: 'order',
  env: 'local',
  namespace: 'ops',
}

const group = {
  ...scope,
  id: 'group-mcp-e2e',
  gatewayGroupCode: 'retail-local',
  displayName: 'Retail Local',
  enabled: true,
  revision: 1,
}

const server = {
  id: 'server-mcp-e2e',
  gatewayGroupId: group.id,
  serverCode: 'commerce',
  displayName: 'Commerce MCP',
  dialects: ['STABLE_2025_11_25', 'RC_2026_07_28'],
  oauthAudience: 'gateway-mcp-commerce',
  listCacheTtlSeconds: 30,
  enabled: true,
  revision: 0,
}

const accessToken = [
  Buffer.from(JSON.stringify({ alg: 'none', typ: 'JWT' })).toString('base64url'),
  Buffer.from(JSON.stringify({ sub: 'mcp-e2e-user', exp: 4_102_444_800 })).toString('base64url'),
  'e2e-signature',
].join('.')

const authenticate = async (page: Page) => {
  await page.route('http://127.0.0.1:18120/oauth2/token', (route) => json(route, {
    access_token: accessToken,
    token_type: 'Bearer',
    expires_in: 3_600,
  }))
  await page.route('**/api/v1/gateway/admin/session', (route) => json(route, {
    actorId: 'mcp-e2e-user',
    displayName: 'MCP E2E',
    actorType: 'USER',
    capabilities: [
      'gateway:read',
      'gateway:mcp:read',
      'gateway:mcp:write',
      'gateway:mcp:test',
      'gateway:mcp:release',
      'gateway:mcp:runtime:read',
    ],
    roles: ['gateway-mcp-admin'],
  }))
  await page.route('**/api/v1/gateway/admin/scopes', (route) => json(route, [{
    ...scope,
    bindingId: 'scope-mcp-e2e',
    appName: 'Order',
    connected: true,
  }]))
  await page.route('**/api/v1/gateway/admin/gateway-groups?**', (route) => json(route, [group]))
  await page.route(`**/api/v1/gateway/admin/gateway-groups/${group.id}/draft`, (route) => json(route, {
    gatewayGroupId: group.id,
    revision: 3,
    status: 'EDITING',
    routes: [],
    policies: [],
    updatedAt: '2026-08-03T00:00:00Z',
  }))
  await page.route('**/api/v1/gateway/admin/mcp/servers?**', (route) => json(route, [server]))
  await page.route(`**/api/v1/gateway/admin/mcp/servers/${server.id}`, (route) => json(route, server))
  await page.route(`**/api/v1/gateway/admin/mcp/servers/${server.id}/tools?**`, (route) => json(route, []))
  await page.route('**/api/v1/gateway/admin/applications?**', (route) => json(route, [{
    ...scope,
    id: 'application-e2e',
    applicationCode: 'order',
    displayName: 'Order',
    ddcMatched: true,
    revision: 1,
  }]))
  await page.route('**/api/v1/gateway/admin/applications/application-e2e/catalog', (route) => json(route, {
    applicationId: 'application-e2e',
    businessDomains: [{
      id: 'business-e2e',
      code: 'commerce',
      displayName: 'Commerce',
      entityDomains: [{
        id: 'entity-e2e',
        code: 'order',
        displayName: 'Order',
        interfaceGroups: [{
          id: 'interface-e2e',
          code: 'order-query',
          displayName: 'Order Query',
          sourceType: 'STARTER',
          operations: [{
            id: 'operation-e2e',
            operationKey: 'order.query',
            protocol: 'HTTP',
            methodIdentity: 'GET /orders/{id}',
            externalAccessible: false,
            lifecycleStatus: 'ACTIVE',
            sourceType: 'STARTER',
            revision: 1,
          }],
        }],
      }],
    }],
  }))
  await page.route(`**/api/v1/gateway/admin/mcp/servers/${server.id}/protocol-inspect`, (route) => json(route, {
    path: '/mcp/commerce',
    headers: {
      'Content-Type': 'application/json',
      'MCP-Protocol-Version': '2025-11-25',
    },
    body: { jsonrpc: '2.0', id: 'inspect-1', method: 'initialize', params: {} },
    releaseCandidate: false,
  }))
}

test('MCP Server workbench binds Tools to Operations and inspects protocol templates', async ({ page }) => {
  await authenticate(page)
  await page.goto('/mcp/servers')
  await expect(page.getByRole('heading', { name: 'MCP Servers' })).toBeVisible()
  await page.getByRole('button', { name: '工作台' }).click()
  await expect(page.getByRole('heading', { name: 'Commerce MCP' })).toBeVisible()

  await page.getByRole('button', { name: '新增 Tool' }).click()
  await expect(page.getByLabel('Operation')).toBeVisible()
  await expect(page.getByLabel('Provider URL')).toHaveCount(0)
  await page.getByRole('button', { name: /取\s*消/ }).click()

  await page.getByRole('tab', { name: 'Protocol Inspector' }).click()
  await page.getByRole('button', { name: '生成请求' }).click()
  await expect(page.getByText('/mcp/commerce')).toBeVisible()
  await expect(page.locator('pre.json-panel')).toContainText('2025-11-25')
})
