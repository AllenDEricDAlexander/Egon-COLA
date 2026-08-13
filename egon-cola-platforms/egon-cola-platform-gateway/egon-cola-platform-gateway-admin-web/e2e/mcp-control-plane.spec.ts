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
  resourceUri: 'https://resource.egon.top/gateway-mcp-commerce',
  listCacheTtlSeconds: 30,
  enabled: true,
  revision: 0,
}

const capability = (
  kind: string,
  id: string,
  name: string,
  content: Record<string, unknown>,
) => ({
  kind,
  id,
  gatewayGroupId: group.id,
  serverId: server.id,
  name,
  content,
  enabled: true,
  revision: 1,
})

const tool = {
  toolId: 'tool-e2e',
  gatewayGroupId: group.id,
  operationId: 'operation-e2e',
  operationKey: 'order.query',
  name: 'order.query',
  description: 'Query an order',
  operationProtocol: 'HTTP',
  inputSchema: { type: 'object' },
  outputSchema: { type: 'object' },
  codeServerId: server.id,
  codeServerCode: server.serverCode,
  serverId: server.id,
  serverCode: server.serverCode,
  codePermissions: ['order:read'],
  additionalPermissions: ['order:audit'],
  effectivePermissions: ['order:audit', 'order:read'],
  codeRiskLevel: 'MEDIUM',
  minimumRiskLevel: 'HIGH',
  effectiveRiskLevel: 'HIGH',
  idempotent: true,
  enabled: true,
  overrideRevision: 1,
}
const remoteProvider = {
  id: 'provider-e2e',
  gatewayGroupId: group.id,
  providerCode: 'inventory-remote',
  content: {
    displayName: 'Inventory Remote',
    dialect: 'RC_2026_07_28',
    transportType: 'STREAMABLE_HTTP',
    endpointReference: 'http://127.0.0.1:19090/mcp',
    status: 'DISCOVERED',
  },
  enabled: true,
  revision: 1,
}
const remoteCapability = {
  id: 'remote-capability-e2e',
  providerId: remoteProvider.id,
  primitiveType: 'TOOL',
  remoteName: 'inventory.lookup',
  descriptor: { description: 'Lookup inventory' },
  capabilityFingerprint: 'abcdef0123456789',
  syncedAt: '2026-08-03T00:00:00Z',
}
const remoteMount = {
  id: 'mount-e2e',
  gatewayGroupId: group.id,
  serverId: server.id,
  providerId: remoteProvider.id,
  namespace: 'inventory',
  capabilityFingerprint: remoteCapability.capabilityFingerprint,
  content: { primitiveTypes: ['TOOL', 'RESOURCE', 'PROMPT', 'APP'], conflictPolicy: 'REJECT' },
  enabled: true,
  revision: 1,
}
const remoteTool = {
  id: 'remote-tool-e2e',
  gatewayGroupId: group.id,
  serverId: server.id,
  serverCode: server.serverCode,
  name: 'inventory.lookup',
  description: 'Lookup remote inventory',
  remoteMountId: remoteMount.id,
  inputSchema: { type: 'object' },
  outputSchema: { type: 'object' },
  annotations: {},
  requiredPermissions: ['inventory:read'],
  riskLevel: 'LOW',
  idempotent: true,
  enabled: true,
  revision: 1,
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
      'gateway:mcp:approve',
      'gateway:releases:write',
    ],
    roles: ['gateway-mcp-admin'],
  }))
  await page.route('**/api/v1/gateway/admin/scopes', (route) => json(route, [{
    ...scope,
    bindingId: 'scope-mcp-e2e',
    appName: 'Order',
    connected: true,
  }]))
  await page.route('**/api/v1/gateway/admin/gateway-groups*', (route) => json(route, [group]))
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
  await page.route(
    `**/api/v1/gateway/admin/mcp/groups/${group.id}/managed-tools?**`,
    (route) => json(route, [tool]),
  )
  await page.route('**/api/v1/gateway/admin/mcp/remote-tools?**', (route) => json(route, [remoteTool]))
  await page.route(`**/api/v1/gateway/admin/mcp/servers/${server.id}/resources?**`, (route) => json(route, [
    capability('RESOURCE', 'resource-e2e', 'order-schema', {
      uri: 'schema://commerce/order',
      driverType: 'STATIC_TEXT',
    }),
  ]))
  await page.route(`**/api/v1/gateway/admin/mcp/servers/${server.id}/resource-templates?**`, (route) => json(route, [
    capability('RESOURCE_TEMPLATE', 'template-e2e', 'order-by-id', {
      uriTemplate: 'order://commerce/{orderId}',
      driverType: 'LOCAL_OPERATION',
    }),
  ]))
  await page.route(`**/api/v1/gateway/admin/mcp/servers/${server.id}/prompts?**`, (route) => json(route, [
    capability('PROMPT', 'prompt-e2e', 'order-assistant', {
      sourceType: 'STRICT_TEMPLATE',
      arguments: ['orderId'],
    }),
  ]))
  await page.route(`**/api/v1/gateway/admin/mcp/servers/${server.id}/task-policies?**`, (route) => json(route, [
    capability('TASK_POLICY', 'task-policy-e2e', tool.name, {
      durable: true,
      maxAttempts: 3,
    }),
  ]))
  await page.route(`**/api/v1/gateway/admin/mcp/servers/${server.id}/app-bindings?**`, (route) => json(route, [
    capability('APP_BINDING', 'app-binding-e2e', 'order-console', {
      appArtifactId: 'artifact-e2e',
      allowedTools: [tool.name],
    }),
  ]))
  await page.route('**/api/v1/gateway/admin/mcp/remote/providers?**', (route) => json(route, [remoteProvider]))
  await page.route(`**/api/v1/gateway/admin/mcp/remote/providers/${remoteProvider.id}/discover`, (route) => json(route, [remoteCapability]))
  await page.route('**/api/v1/gateway/admin/mcp/remote/mounts?**', (route) => json(route, [remoteMount]))
  await page.route('**/api/v1/gateway/admin/mcp/apps/artifacts?**', (route) => json(route, [{
    id: 'artifact-e2e',
    gatewayGroupId: group.id,
    appCode: 'order-console',
    version: '1.0.0',
    displayName: 'Order Console',
    resourceUri: 'ui://commerce/order-console',
    artifactReference: 'mcp-app://artifact-e2e',
    sha256: 'f'.repeat(64),
    sizeBytes: 2048,
    mimeType: 'text/html',
    contentSecurityPolicy: "default-src 'none'; script-src 'self'",
    permissions: ['ui:render'],
    allowedOrigins: [],
    createdBy: 'mcp-e2e-user',
    createdAt: '2026-08-03T00:00:00Z',
  }]))
  await page.route(`**/api/v1/gateway/admin/mcp/servers/${server.id}/capability-preview`, (route) => json(route, {
    content: {
      serverCode: server.serverCode,
      tools: [tool.name],
      remoteMounts: [remoteMount.namespace],
    },
    validation: { valid: true, findings: [] },
  }))
  await page.route(`**/api/v1/gateway/admin/mcp/servers/${server.id}/validate`, (route) => json(route, {
    valid: true,
    findings: [],
  }))
  await page.route(`**/api/v1/gateway/admin/gateway-groups/${group.id}/runtime-consistency`, (route) => json(route, {
    targetReleaseId: 'release-e2e',
    targetReleaseStatus: 'ACTIVE',
    engineNodeCount: 1,
    readyEngineNodeCount: 1,
    consistent: true,
    observedAt: '2026-08-03T00:00:00Z',
    source: 'DDC',
    stale: false,
  }))
  await page.route(`**/api/v1/gateway/admin/gateway-groups/${group.id}/engine-nodes`, (route) => json(route, {
    value: [{
      appCode: scope.appCode,
      env: scope.env,
      namespace: scope.namespace,
      instanceId: 'gateway-engine-e2e',
      leaseId: 'lease-e2e',
      leaseRole: 'INTERNAL_GATEWAY',
      status: 'READY',
      registeredAt: '2026-08-03T00:00:00Z',
      lastHeartbeatAt: '2026-08-03T00:00:00Z',
      expireAt: '2026-08-03T00:01:00Z',
    }],
    observedAt: '2026-08-03T00:00:00Z',
    source: 'DDC',
    stale: false,
  }))
  await page.route('**/api/v1/gateway/admin/mcp/tasks?**', (route) => json(route, [{
    id: 'task-e2e',
    principalFingerprint: 'principal-e2e',
    subjectId: 'mcp-e2e-user',
    tenantId: 'tenant-e2e',
    clientId: 'client-e2e',
    serverCode: server.serverCode,
    toolName: tool.name,
    requestDigest: 'request-e2e',
    state: 'WORKING',
    executionDeadline: '2026-08-03T00:05:00Z',
    expiresAt: '2026-08-04T00:00:00Z',
    attemptCount: 1,
    maxAttempts: 3,
    revision: 1,
    createdAt: '2026-08-03T00:00:00Z',
    updatedAt: '2026-08-03T00:00:00Z',
  }]))
  await page.route('**/api/v1/gateway/admin/mcp/approvals', (route) => json(route, {
    approvalId: 'approval-e2e',
    approvalToken: 'approval-token-shown-once',
    expiresAt: '2026-08-03T00:02:00Z',
  }))
  await page.route('**/api/v1/gateway/admin/applications*', (route) => json(route, [{
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

test('MCP Server workbench covers the complete control-plane lifecycle', async ({ page }) => {
  await authenticate(page)
  await page.goto('/mcp/servers')
  await expect(page.getByRole('heading', { name: 'MCP Servers' })).toBeVisible()
  await page.getByRole('button', { name: '工作台' }).click()
  await expect(page.getByRole('heading', { name: 'Commerce MCP' })).toBeVisible()

  await expect(page.getByText(tool.name, { exact: true })).toBeVisible()
  await expect(page.getByRole('button', { name: /新增.*Tool/ })).toHaveCount(0)
  await expect(page.getByRole('button', { name: '严格 Override' })).toBeVisible()

  await page.getByRole('tab', { name: 'Remote Tools' }).click()
  await expect(page.getByText(remoteTool.name, { exact: true })).toBeVisible()
  await page.getByRole('button', { name: '新增 Remote Tool' }).click()
  await expect(page.getByLabel('Remote Mount')).toBeVisible()
  await expect(page.getByLabel('Operation')).toHaveCount(0)
  await page.getByRole('button', { name: /取\s*消/ }).click()

  await page.getByRole('tab', { name: 'Resources', exact: true }).first().click()
  await expect(page.getByText('order-schema', { exact: true })).toBeVisible()
  await page.getByRole('tab', { name: 'Resource Templates' }).click()
  await expect(page.getByText('order-by-id', { exact: true })).toBeVisible()

  await page.getByRole('tab', { name: 'Prompts' }).click()
  await expect(page.getByText('order-assistant', { exact: true })).toBeVisible()

  await page.getByRole('tab', { name: 'Tasks' }).click()
  await expect(page.getByRole('tabpanel', { name: 'Task Policies' }).getByText(tool.name, { exact: true })).toBeVisible()
  await page.getByRole('tab', { name: 'Runtime Tasks' }).click()
  await page.getByLabel('Tenant ID').fill('tenant-e2e')
  await page.getByRole('button', { name: /查\s*询/ }).click()
  await expect(page.getByText('task-e2e', { exact: true })).toBeVisible()

  await page.getByRole('tab', { name: 'Apps' }).click()
  await expect(page.getByText('order-console', { exact: true })).toBeVisible()
  await page.getByRole('button', { name: '安全报告' }).click()
  await expect(page.getByText('f'.repeat(64), { exact: true })).toBeVisible()
  await page.getByRole('button', { name: 'Close' }).click()
  await page.getByRole('tab', { name: 'App Bindings' }).click()
  await expect(page.getByRole('tabpanel', { name: 'App Bindings' }).getByText('order-console', { exact: true })).toBeVisible()

  await page.getByRole('tab', { name: 'Remote Mounts' }).click()
  await expect(page.getByText(remoteMount.namespace, { exact: true })).toBeVisible()
  await expect(page.getByText(remoteProvider.providerCode, { exact: true })).toBeVisible()

  await page.getByRole('tab', { name: 'Preview & Release' }).click()
  await expect(page.getByText('MCP 校验通过')).toBeVisible()
  await expect(page.getByRole('button', { name: /发\s*布/ })).toBeEnabled()

  await page.getByRole('tab', { name: 'Runtime' }).click()
  await expect(page.getByText('release-e2e', { exact: true })).toBeVisible()
  await expect(page.getByText('gateway-engine-e2e', { exact: true })).toBeVisible()

  await page.getByRole('tab', { name: 'Approvals' }).click()
  await page.getByLabel('High-Risk Tool').fill(tool.name)
  await page.getByRole('button', { name: '签发 Approval' }).click()
  await expect(page.getByText('approval-token-shown-once', { exact: true })).toBeVisible()

  await page.getByRole('tab', { name: 'Protocol Inspector' }).click()
  await page.getByRole('button', { name: '生成请求' }).click()
  await expect(page.getByText('/mcp/commerce')).toBeVisible()
  await expect(page.locator('pre.json-panel')).toContainText('2025-11-25')
})

test('Remote provider discovery exposes reviewed capability fingerprints', async ({ page }) => {
  await authenticate(page)
  await page.goto('/mcp/remote-providers')
  await expect(page.getByRole('heading', { name: 'Remote MCP Providers' })).toBeVisible()
  await expect(page.getByText(remoteProvider.providerCode, { exact: true })).toBeVisible()
  await page.getByRole('button', { name: 'Discover / Diff' }).click()
  await expect(page.getByText('已发现 1 个能力。', { exact: false })).toBeVisible()
  await page.getByText('Reviewed Remote Capabilities', { exact: true }).click()
  await expect(page.locator('.json-panel')).toContainText(remoteCapability.capabilityFingerprint)
})
