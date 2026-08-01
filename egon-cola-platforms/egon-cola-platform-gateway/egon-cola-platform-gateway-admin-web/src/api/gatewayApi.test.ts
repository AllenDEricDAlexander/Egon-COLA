import { afterEach, describe, expect, it, vi } from 'vitest'
import { gatewayApi } from './gatewayApi'

const jsonResponse = (body: unknown) =>
  new Response(JSON.stringify(body), {
    status: 200,
    headers: { 'Content-Type': 'application/json' },
  })

afterEach(() => {
  vi.unstubAllGlobals()
})

describe('gateway API response adapters', () => {
  it('loads the authoritative scope catalog without static filters', async () => {
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse([]))
    vi.stubGlobal('fetch', fetchMock)

    await gatewayApi.scopes()

    expect(fetchMock).toHaveBeenCalledOnce()
    expect(fetchMock.mock.calls[0][0]).toBe('/api/v1/gateway/admin/scopes')
  })

  it('unwraps engine and provider projections without losing freshness', async () => {
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(jsonResponse({
        value: [{
          appCode: 'gateway-orders',
          env: 'test',
          namespace: 'gateway',
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
        source: 'DDC_CONFIG_CLIENT',
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
        source: 'DDC_SERVICE_REGISTRY',
        stale: true,
        refreshError: 'using last known good snapshot',
      }))
    vi.stubGlobal('fetch', fetchMock)

    const nodes = await gatewayApi.engineNodes('group-1')
    const providers = await gatewayApi.providers({
      bizCode: 'test-biz',
      appCode: 'orders',
      env: 'test',
      namespace: 'gateway',
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
      source: 'DDC_CONFIG_CLIENT',
      stale: false,
    })))

    await expect(gatewayApi.consistency('group-1')).resolves.toMatchObject({
      targetReleaseId: 'release-1',
      totalNodes: 3,
      readyNodes: 2,
      consistent: false,
    })
  })
})
