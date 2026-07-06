# Egon COLA Dynamic Thread Pool Component

This component provides Spring Boot starter based executor governance and an independently deployable admin service.

## Modules

| Module | Purpose |
|---|---|
| `egon-cola-component-dynamic-thread-pool-starter` | Business application integration, executor registration, snapshots, Redis change listening, MDC propagation, audit events, and Micrometer metrics. |
| `egon-cola-component-dynamic-thread-pool-admin` | REST management API, Redis-backed queries and change publishing, manifest endpoint, and Docker packaging. |
| `egon-cola-component-dynamic-thread-pool-test` | Component sample and validation module. |

## Configuration Prefix

```yaml
egon:
  cola:
    component:
      dtp:
        enabled: true
        app-name: ${spring.application.name}
        instance-id: ${spring.application.name}-${server.port}
        registry:
          type: redis
          redis:
            host: 127.0.0.1
            port: 6379
            password:
            database: 0
        report:
          enabled: true
          interval: 20s
        trace:
          enabled: true
          mdc-enabled: true
          trace-id-key: traceId
          request-id-key: requestId
        virtual:
          enabled: true
          default-concurrency-limit: 500
```

## Admin API

The admin API base path is `/api/v1/dtp`.

## Manifest

The admin exposes `GET /api/v1/dtp/manifest` for dynamic frontend discovery.

## UI Boundary

UI code is not stored in `egon-cola-components`.
