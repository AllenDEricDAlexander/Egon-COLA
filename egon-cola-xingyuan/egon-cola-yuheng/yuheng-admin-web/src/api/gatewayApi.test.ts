import { afterEach, describe, expect, it, vi } from 'vitest'
import { gatewayApi } from './gatewayApi'
import { gatewayEngineRoleOf, normalizeEngineMetadata } from './types'

const jsonResponse = (body: unknown) =>
  new Response(JSON.stringify(body), {
    status: 200,
    headers: { 'Content-Type': 'application/json' },
  })

afterEach(() => {
  vi.unstubAllGlobals()
})

describe('yuheng API response adapters', () => {
  it('accepts canonical role values without guessing malformed or absent metadata', () => {
    expect(gatewayEngineRoleOf({ 'yuheng.engine.role': ' API_RPC ' })).toBe('API_RPC')
    expect(gatewayEngineRoleOf({ 'yuheng.engine.role': 'MCP' })).toBe('MCP')
    for (const metadata of [null, undefined, {}, { role: 'MCP' },
      { 'yuheng.engine.role': 12 }, { 'yuheng.engine.role': 'mcp' }, { 'yuheng.engine.role': 'COMBINED' }]) {
      expect(gatewayEngineRoleOf(metadata)).toBeUndefined()
    }
    expect(normalizeEngineMetadata(['MCP'])).toEqual({})
  })
  it('normalizes nullable role metadata and retains per-node ACK reasons', async () => {
    const state = { instanceId: 'mcp', leaseId: 'lease-mcp', leaseStatus: 'ONLINE',
      status: 'INCONSISTENT', reason: 'VERSION_MISMATCH', activeRuleVersion: 11,
      activeReleaseId: 'release-1', activeRuleChecksum: 'sha', lastApplyStatus: 'ACK_SUCCESS',
      lastAckAt: '2026-09-05T00:00:00Z' }
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(jsonResponse({ value: [
        { instanceId: 'old', metadata: null },
        { instanceId: 'mcp', metadata: { 'yuheng.engine.role': 'MCP', invalid: 12 } },
      ], observedAt: '2026-09-05T00:00:00Z', stale: false }))
      .mockResolvedValueOnce(jsonResponse({ engineNodeCount: 1, readyEngineNodeCount: 0,
        consistent: false, stale: false, source: 'TIANSHU_CONFIG_CLIENT', observedAt: '2026-09-05T00:00:00Z',
        nodes: [state] }))
    vi.stubGlobal('fetch', fetchMock)
    const nodes = await gatewayApi.engineNodes('group-1')
    expect(nodes[0].metadata).toEqual({})
    expect(nodes[1].metadata).toEqual({ 'yuheng.engine.role': 'MCP' })
    expect((await gatewayApi.consistency('group-1')).nodes).toEqual([state])
    expect(fetchMock.mock.calls.map(([url]) => url)).toEqual([
      '/api/v1/yuheng/admin/yuheng-groups/group-1/engine-nodes',
      '/api/v1/yuheng/admin/yuheng-groups/group-1/runtime-consistency',
    ])
  })
  it('loads OpenAPI sync states with encoded scope filters and immutable reads', async () => {
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(jsonResponse([{
        id: 'sync-1',
        applicationId: 'application-1',
        buildId: 'build-1',
        artifactVersion: '1.0.0',
        openapiGroup: 'orders',
        sourceType: 'OPENAPI31',
        status: 'VALID',
        snapshotId: 'snapshot-1',
        definitionSetId: 'set-1',
        operationCount: 2,
        schemaCount: 1,
        canonicalSha256: 'a'.repeat(64),
        lastErrorCode: null,
        lastErrorMessage: null,
        lastAttemptAt: null,
        lastSuccessAt: '2026-08-26T03:00:00Z',
        nextRetryAt: null,
      }]))
      .mockResolvedValueOnce(jsonResponse({
        operationId: 'operation-1',
        operationKey: 'orders:http:GET:/orders/{id}',
        sourceType: 'OPENAPI31',
        snapshotId: 'snapshot-1',
        openapiVersion: '3.1.0',
        openapiGroup: 'orders',
        path: '/orders/{id}',
        method: 'GET',
        openapiOperationId: 'getOrder',
        requestContentTypes: ['application/json'],
        responseContentTypes: ['application/json'],
        operation: { operationId: 'getOrder' },
        syncedAt: '2026-08-26T03:00:00Z',
      }))
      .mockResolvedValueOnce(jsonResponse({
        snapshotId: 'snapshot-1',
        applicationId: 'application-1',
        buildId: 'build-1',
        openapiVersion: '3.1.0',
        documentSha256: 'b'.repeat(64),
        canonicalSha256: 'a'.repeat(64),
        document: { openapi: '3.1.0' },
        fetchedAt: '2026-08-26T02:59:00Z',
        validatedAt: '2026-08-26T03:00:00Z',
      }))
    vi.stubGlobal('fetch', fetchMock)

    await gatewayApi.openapiSyncStates({
      bizCode: 'retail',
      namespace: 'ops',
      env: 'test',
      appCode: 'orders',
    })
    await gatewayApi.operationOpenApi('operation-1')
    await gatewayApi.openapiSnapshotDocument('snapshot-1')

    expect(fetchMock.mock.calls[0][0]).toBe(
      '/api/v1/yuheng/admin/openapi/sync-states?bizCode=retail&namespace=ops&env=test&appCode=orders',
    )
    expect(fetchMock.mock.calls[1][0]).toBe(
      '/api/v1/yuheng/admin/operations/operation-1/openapi',
    )
    expect(fetchMock.mock.calls[2][0]).toBe(
      '/api/v1/yuheng/admin/openapi/snapshots/snapshot-1/document',
    )
    expect(fetchMock.mock.calls.every(([, request]) => (request as RequestInit).method === undefined)).toBe(true)
  })

  it('loads the authoritative scope catalog without static filters', async () => {
    const fetchMock = vi.fn().mockImplementation(() => Promise.resolve(jsonResponse([])))
    vi.stubGlobal('fetch', fetchMock)

    await gatewayApi.scopes()

    expect(fetchMock).toHaveBeenCalledOnce()
    expect(fetchMock.mock.calls[0][0]).toBe('/api/v1/yuheng/admin/scopes')
  })

  it('loads groups across scopes and applications with optional page-local filters', async () => {
    const fetchMock = vi.fn().mockImplementation(() => Promise.resolve(jsonResponse([])))
    vi.stubGlobal('fetch', fetchMock)

    await gatewayApi.groups()
    await gatewayApi.applications({ bizCode: 'retail', env: 'prod' })
    await gatewayApi.applications()

    expect(fetchMock.mock.calls[0][0]).toBe('/api/v1/yuheng/admin/yuheng-groups')
    expect(fetchMock.mock.calls[1][0]).toBe(
      '/api/v1/yuheng/admin/applications?bizCode=retail&env=prod',
    )
    expect(fetchMock.mock.calls[2][0]).toBe('/api/v1/yuheng/admin/applications')
  })

  it('loads an application detail through the Yuheng application controller path', async () => {
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse({
      id: 'application-1',
      bizCode: 'retail',
      applicationCode: 'orders',
      displayName: 'Orders',
      env: 'test',
      namespace: 'yuheng',
      ddcMatched: true,
      revision: 4,
    }))
    vi.stubGlobal('fetch', fetchMock)

    await gatewayApi.application('application-1')

    expect(fetchMock.mock.calls[0][0]).toBe('/api/v1/yuheng/admin/applications/application-1')
  })

  it('loads MCP operation options from the selected application catalog only', async () => {
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse({
      businessDomains: [{
        id: 'business-1',
        code: 'retail',
        displayName: 'Retail',
        entityDomains: [{
          id: 'entity-1',
          code: 'orders',
          displayName: 'Orders',
          interfaceGroups: [{
            id: 'group-1',
            code: 'commands',
            displayName: 'Commands',
            sourceType: 'MANUAL',
            operations: [{
              id: 'operation-1',
              operationKey: 'orders.create',
              protocol: 'HTTP',
              methodIdentity: 'POST /orders',
            }],
          }],
        }],
      }],
    }))
    vi.stubGlobal('fetch', fetchMock)

    await expect(gatewayApi.mcpOperationOptions('application-1')).resolves.toEqual([{
      value: 'operation-1',
      label: 'orders.create · HTTP POST /orders',
    }])

    expect(fetchMock).toHaveBeenCalledOnce()
    expect(fetchMock.mock.calls[0][0]).toBe(
      '/api/v1/yuheng/admin/applications/application-1/catalog',
    )
  })

  it('unwraps engine and provider projections without losing freshness', async () => {
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(jsonResponse({
        value: [{
          appCode: 'yuheng-orders',
          env: 'test',
          namespace: 'yuheng',
          instanceId: 'engine-1',
          leaseId: 'lease-1',
          host: '127.0.0.1',
          port: 18080,
          leaseRole: 'CONFIG_CLIENT',
          status: 'READY',
          registeredAt: '2026-07-25T08:00:00Z',
          lastHeartbeatAt: '2026-07-25T08:00:05Z',
          expireAt: '2026-07-25T08:00:35Z',
        }],
        observedAt: '2026-07-25T08:00:06Z',
        source: 'TIANSHU_CONFIG_CLIENT',
        stale: false,
      }))
      .mockResolvedValueOnce(jsonResponse({
        value: [{
          serviceKey: 'HTTP_PROVIDER:HTTP:orders::',
          protocol: 'HTTP',
          serviceName: 'orders',
          group: 'default',
          version: '1.0.0',
          instanceId: 'orders-1',
          leaseId: 'lease-orders',
          host: '127.0.0.1',
          port: 18090,
          status: 'REGISTERED',
          expireAt: '2026-07-25T08:00:35Z',
          observedAt: '2026-07-25T08:00:06Z',
        }],
        observedAt: '2026-07-25T08:00:06Z',
        source: 'TIANSHU_SERVICE_REGISTRY',
        stale: true,
        refreshError: 'using last known good snapshot',
      }))
    vi.stubGlobal('fetch', fetchMock)

    const nodes = await gatewayApi.engineNodes('group-1')
    const providers = await gatewayApi.providers({
      bizCode: 'test-biz',
      appCode: 'orders',
      env: 'test',
      namespace: 'yuheng',
    })

    expect(nodes[0]).toMatchObject({
      instanceId: 'engine-1',
      observedAt: '2026-07-25T08:00:06Z',
      stale: false,
    })
    expect(providers[0]).toMatchObject({
      instanceId: 'orders-1',
      stale: true,
    })
  })

  it('maps draft content and release identities to the view contract', async () => {
    const release = {
      releaseId: 'release-1',
      gatewayGroupId: 'group-1',
      draftRevision: 2,
      status: 'SUCCESS',
      partialApplied: false,
      validationReport: { valid: true, errors: [], warnings: [] },
      structuredDiff: {},
      changeReason: 'publish',
      createdAt: '2026-07-25T08:00:00Z',
      updatedAt: '2026-07-25T08:00:01Z',
      attempts: [],
    }
    vi.stubGlobal('fetch', vi.fn()
      .mockResolvedValueOnce(jsonResponse({
        gatewayGroupId: 'group-1',
        revision: 2,
        status: 'EDITING',
        routes: [{
          gatewayGroupId: 'group-1',
          routeId: 'route-1',
          operationId: 'operation-1',
          content: { listener: 'INTERNAL', protocol: 'HTTP' },
          enabled: true,
          updatedAt: '2026-07-25T08:00:00Z',
          updatedBy: 'admin',
        }],
        policies: [{
          gatewayGroupId: 'group-1',
          policyId: 'policy-1',
          policyType: 'RATE_LIMIT',
          policyScope: 'ROUTE',
          content: { permitsPerSecond: 100 },
          enabled: true,
          updatedAt: '2026-07-25T08:00:00Z',
          updatedBy: 'admin',
        }],
        updatedAt: '2026-07-25T08:00:00Z',
      }))
      .mockResolvedValueOnce(jsonResponse([release])))

    const draft = await gatewayApi.draft('group-1')
    const releases = await gatewayApi.releases('group-1')

    expect(draft.routes[0].routeContent).toEqual({
      listener: 'INTERNAL',
      protocol: 'HTTP',
    })
    expect(draft.policies[0].policyContent).toEqual({
      permitsPerSecond: 100,
    })
    expect(releases[0].id).toBe('release-1')
  })

  it('preserves transport overrides and unknown draft extensions exactly', async () => {
    const transportPolicy = {
      profile: 'OPENAI_HTTP',
      transportProtocol: 'HTTP',
      requestBodyMode: 'STREAMING',
      responseMode: 'AUTO_STREAM',
      bodyLogEnabled: false,
      retryEnabled: false,
      futureOption: false,
    }
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(jsonResponse({
      gatewayGroupId: 'group-1',
      revision: 3,
      status: 'EDITING',
      routes: [{
        routeId: 'route-ai',
        operationId: 'operation-ai',
        content: {
          host: 'ai.example.com',
          httpMethod: 'POST',
          pathPattern: '/v1/**',
          accessZones: ['PUBLIC'],
          transportPolicy,
          futureRouteOption: { enabled: false },
        },
        enabled: true,
      }],
      policies: [],
      updatedAt: '2026-07-30T08:00:00Z',
    })))

    const value = await gatewayApi.draft('group-1')

    expect(value.routes[0].routeContent.transportPolicy).toEqual(transportPolicy)
    expect(value.routes[0].routeContent.futureRouteOption).toEqual({ enabled: false })
  })

  it('maps runtime consistency field names', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(jsonResponse({
      targetReleaseId: 'release-1',
      targetReleaseStatus: 'SUCCESS',
      engineNodeCount: 3,
      readyEngineNodeCount: 2,
      consistent: false,
      observedAt: '2026-07-25T08:00:00Z',
      source: 'TIANSHU_CONFIG_CLIENT',
      stale: false,
    })))

    await expect(gatewayApi.consistency('group-1')).resolves.toMatchObject({
      targetReleaseId: 'release-1',
      totalNodes: 3,
      readyNodes: 2,
      consistent: false,
    })
  })

  it('uses dedicated Managed and Remote Tool endpoints', async () => {
    const fetchMock = vi.fn().mockImplementation(() => Promise.resolve(jsonResponse([])))
    vi.stubGlobal('fetch', fetchMock)

    await gatewayApi.mcpManagedTools('group-1', 'server-1')
    await gatewayApi.mcpRemoteTools('group-1', 'server-1')

    expect(fetchMock.mock.calls[0][0]).toBe(
      '/api/v1/yuheng/admin/mcp/groups/group-1/managed-tools?serverId=server-1',
    )
    expect(fetchMock.mock.calls[1][0]).toBe(
      '/api/v1/yuheng/admin/mcp/remote-tools?gatewayGroupId=group-1&serverId=server-1',
    )
  })

  it('sends only strict Managed Tool override fields to the override endpoint', async () => {
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse({
      draftRevision: 8,
      resourceId: 'tool-1',
      resourceRevision: 2,
      replayed: false,
    }))
    vi.stubGlobal('fetch', fetchMock)

    await gatewayApi.updateMcpManagedToolOverride('tool-1', {
      gatewayGroupId: 'group-1',
      serverId: 'server-2',
      additionalPermissions: ['order:audit'],
      minimumRiskLevel: 'HIGH',
      enabled: false,
      expectedRevision: 1,
      expectedDraftRevision: 7,
      changeReason: 'harden production access',
    })

    expect(fetchMock.mock.calls[0][0]).toBe(
      '/api/v1/yuheng/admin/mcp/managed-tools/tool-1/override',
    )
    const request = fetchMock.mock.calls[0][1] as RequestInit
    expect(request.method).toBe('PUT')
    expect(JSON.parse(String(request.body))).toEqual({
      gatewayGroupId: 'group-1',
      serverId: 'server-2',
      additionalPermissions: ['order:audit'],
      minimumRiskLevel: 'HIGH',
      enabled: false,
      expectedRevision: 1,
      expectedDraftRevision: 7,
      changeReason: 'harden production access',
    })
    expect(new Headers(request.headers).has('Idempotency-Key')).toBe(true)
  })

  it('maps catalog lifecycle commands to the existing Admin paths', async () => {
    const operation = { operation: { id: 'op-1' }, definitions: [] }
    const fetchMock = vi.fn().mockImplementation((_input: RequestInfo | URL, init?: RequestInit) => {
      if ((init?.method ?? 'GET') === 'POST') return Promise.resolve(jsonResponse(operation))
      return Promise.resolve(jsonResponse(operation))
    })
    vi.stubGlobal('fetch', fetchMock)

    await gatewayApi.updateOperationMetadata('op-1', {
      summary: 'Orders',
      tags: ['orders'],
      owner: 'xingyuan',
    })
    await gatewayApi.updateManualDefinition('op-1', {
      summary: 'Orders',
      tags: ['orders'],
      requestSchema: {type: 'object'},
      responseSchema: {type: 'object'},
      errorSchema: [],
      attributes: {},
      externalAccessible: true,
    })
    await gatewayApi.deprecateOperation('op-1')

    expect(fetchMock.mock.calls.map(([path]) => path)).toEqual([
      '/api/v1/yuheng/admin/operations/op-1/metadata',
      '/api/v1/yuheng/admin/operations/op-1/manual-definition',
      '/api/v1/yuheng/admin/operations/op-1/deprecate',
    ])
    expect(JSON.parse(String((fetchMock.mock.calls[0][1] as RequestInit).body))).toEqual({
      summary: 'Orders',
      tags: ['orders'],
      owner: 'xingyuan',
    })
    expect(JSON.parse(String((fetchMock.mock.calls[1][1] as RequestInit).body))).not.toHaveProperty('secret')
    expect((fetchMock.mock.calls[2][1] as RequestInit).body).toBeUndefined()
    expect(new Headers((fetchMock.mock.calls[0][1] as RequestInit).headers).has('Idempotency-Key')).toBe(true)
  })

  it('loads the supported release diff endpoint', async () => {
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(jsonResponse({routes: {changed: 1}}))
      .mockResolvedValueOnce(jsonResponse({valid: true, findings: []}))
    vi.stubGlobal('fetch', fetchMock)

    await expect(gatewayApi.releaseDiff('release-1')).resolves.toEqual({routes: {changed: 1}})
    await expect(gatewayApi.validateMcpCapability('prompts', 'prompt-1', 'group-1')).resolves.toEqual({valid: true, findings: []})
    expect(fetchMock.mock.calls[0][0]).toBe('/api/v1/yuheng/admin/releases/release-1/diff')
    expect(fetchMock.mock.calls[1][0]).toBe('/api/v1/yuheng/admin/mcp/prompts/prompt-1/validate?gatewayGroupId=group-1')
    expect((fetchMock.mock.calls[1][1] as RequestInit).method).toBe('POST')
  })
})
