import { describe, expect, it } from 'vitest'
import {
  readRouteForm,
  transportFieldPresentation,
  validateTransportRoute,
  writeCanonicalRoute,
  type RouteFormValues,
} from './routeTransport'

const validRoute = (): RouteFormValues => ({
  host: 'ai.example.com',
  httpMethod: 'POST',
  pathPattern: '/v1/**',
  accessZones: ['PUBLIC'],
  priority: 0,
  advancedContent: '{}',
})

describe('route transport mapper', () => {
  it('reads legacy route keys into canonical form fields without inventing Host', () => {
    const values = readRouteForm({
      host: 'ai.example.com',
      listener: 'PUBLIC',
      method: 'POST',
      path: '/v1/**',
      protocol: 'HTTP',
      fullMethodName: 'legacy.Service/Call',
      providerServiceName: 'legacy-provider',
      customExtension: { enabled: false },
    })

    expect(values).toMatchObject({
      host: 'ai.example.com',
      accessZones: ['PUBLIC'],
      httpMethod: 'POST',
      pathPattern: '/v1/**',
    })
    expect(JSON.parse(values.advancedContent)).toEqual({
      customExtension: { enabled: false },
    })

    const missingHost = readRouteForm({
      listener: 'PUBLIC',
      method: 'POST',
      path: '/v1/**',
    })
    expect(missingHost.host).toBeUndefined()
    expect(validateTransportRoute(missingHost)).toContainEqual(
      expect.objectContaining({
        path: 'host',
        message: '历史草稿缺少 Host，请补录',
      }),
    )
  })

  it('writes canonical keys, keeps explicit false, and recursively removes undefined', () => {
    const content = writeCanonicalRoute({
      ...validRoute(),
      advancedContent: JSON.stringify({
        listener: 'INTERNAL',
        method: 'GET',
        path: '/legacy',
        customExtension: { enabled: false },
      }),
      transportPolicy: {
        profile: 'OPENAI_HTTP',
        requestBodyMode: 'STREAMING',
        responseMode: undefined,
        bodyLogEnabled: false,
        retryEnabled: false,
      },
    })

    expect(content).toEqual({
      customExtension: { enabled: false },
      host: 'ai.example.com',
      httpMethod: 'POST',
      pathPattern: '/v1/**',
      accessZones: ['PUBLIC'],
      priority: 0,
      transportPolicy: {
        profile: 'OPENAI_HTTP',
        requestBodyMode: 'STREAMING',
        bodyLogEnabled: false,
        retryEnabled: false,
      },
    })
    expect(content).not.toHaveProperty('listener')
    expect(content).not.toHaveProperty('method')
    expect(content).not.toHaveProperty('path')
  })

  it('preserves existing transport overrides and only presents profile defaults', () => {
    const values = readRouteForm({
      host: 'ai.example.com',
      httpMethod: 'POST',
      pathPattern: '/v1/**',
      accessZones: ['PUBLIC'],
      priority: 0,
      transportPolicy: {
        profile: 'OPENAI_HTTP',
        connectTimeoutMs: 15000,
        futureOption: false,
      },
    })

    expect(transportFieldPresentation(values.transportPolicy, 'requestBodyMode')).toEqual({
      value: 'STREAMING',
      source: 'PROFILE_DEFAULT',
    })
    expect(transportFieldPresentation(values.transportPolicy, 'connectTimeoutMs')).toEqual({
      value: 15000,
      source: 'ROUTE_OVERRIDE',
    })
    expect(writeCanonicalRoute(values).transportPolicy).toEqual({
      profile: 'OPENAI_HTTP',
      connectTimeoutMs: 15000,
      futureOption: false,
    })
  })

  it('rejects unknown transport enum values from stored JSON', () => {
    const values = readRouteForm({
      host: 'ai.example.com',
      httpMethod: 'POST',
      pathPattern: '/v1/**',
      accessZones: ['PUBLIC'],
      transportPolicy: {
        profile: 'FUTURE_PROFILE',
        transportProtocol: 'TCP',
        requestBodyMode: 'BUFFERED',
        responseMode: 'CACHED',
      },
    })

    expect(validateTransportRoute(values).map((item) => item.path)).toEqual([
      'transportPolicy.profile',
      'transportPolicy.transportProtocol',
      'transportPolicy.requestBodyMode',
      'transportPolicy.responseMode',
    ])
  })

  it('validates every numeric transport override at the documented boundaries', () => {
    const ranges = [
      ['maxRequestBodyBytes', 1, 1_073_741_824],
      ['connectTimeoutMs', 100, 60_000],
      ['responseHeaderTimeoutMs', 1_000, 600_000],
      ['streamIdleTimeoutMs', 1_000, 1_800_000],
      ['totalTimeoutMs', 1_000, 7_200_000],
      ['websocketIdleTimeoutMs', 1_000, 7_200_000],
      ['websocketMaxFrameBytes', 1_024, 67_108_864],
    ] as const

    ranges.forEach(([field, minimum, maximum]) => {
      const atBounds = {
        ...validRoute(),
        transportPolicy: { [field]: minimum },
      }
      expect(validateTransportRoute(atBounds)).toEqual([])
      expect(validateTransportRoute({
        ...validRoute(),
        transportPolicy: { [field]: maximum },
      })).toEqual([])
      expect(validateTransportRoute({
        ...validRoute(),
        transportPolicy: { [field]: minimum - 1 },
      })).toContainEqual(expect.objectContaining({
        path: `transportPolicy.${field}`,
      }))
      expect(validateTransportRoute({
        ...validRoute(),
        transportPolicy: { [field]: maximum + 1 },
      })).toContainEqual(expect.objectContaining({
        path: `transportPolicy.${field}`,
      }))
    })
  })

  it('rejects WebSocket and streaming transport for RPC operations', () => {
    expect(validateTransportRoute({
      ...validRoute(),
      operationProtocol: 'HTTP',
      httpMethod: 'POST',
      transportPolicy: { transportProtocol: 'WEBSOCKET' },
    })).toContainEqual(expect.objectContaining({
      path: 'httpMethod',
      message: expect.stringContaining('GET'),
    }))

    expect(validateTransportRoute({
      ...validRoute(),
      operationProtocol: 'RPC',
      transportPolicy: {
        transportProtocol: 'WEBSOCKET',
        requestBodyMode: 'STREAMING',
        responseMode: 'SSE',
      },
    })).toContainEqual(expect.objectContaining({
      path: 'transportPolicy.transportProtocol',
      message: expect.stringContaining('RPC'),
    }))
  })

  it('does not promote model business or static provider fields into route content', () => {
    const content = writeCanonicalRoute({
      ...validRoute(),
      model: 'gpt-example',
      tokenQuota: 1000,
      billingPlan: 'paid',
      promptTemplate: 'prompt',
      ragEnabled: true,
      agentId: 'agent-1',
      providerUrl: 'https://provider.example.com',
    } as RouteFormValues & Record<string, unknown>)

    expect(content).not.toHaveProperty('model')
    expect(content).not.toHaveProperty('tokenQuota')
    expect(content).not.toHaveProperty('billingPlan')
    expect(content).not.toHaveProperty('promptTemplate')
    expect(content).not.toHaveProperty('ragEnabled')
    expect(content).not.toHaveProperty('agentId')
    expect(content).not.toHaveProperty('providerUrl')
  })
})
