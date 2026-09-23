# Dynamic Thread Pool Manifest

The dynamic thread pool admin exposes a manifest for management UI discovery.

Endpoint:

```text
GET /api/v1/dtp/manifest
```

Response fields:

The endpoint returns the standard admin response envelope. The manifest payload is under `data`.

| Field | Meaning |
|---|---|
| `code` | Response status code. |
| `info` | Response status message. |
| `traceId` | Trace identifier copied from MDC when present. |
| `data` | Manifest payload. |
| `data.component` | Stable component key, `dynamic-thread-pool`. |
| `data.name` | Human-readable component name. |
| `data.version` | Manifest version reported from `egon.cola.component.dtp.manifest.version`, whose built-in default is `5.2.1`. It is not derived from the Maven artifact version. |
| `data.enabled` | Currently always `true` in the admin response. |
| `data.baseApi` | Admin API base path. |
| `data.frontend.module` | Frontend module key. |
| `data.frontend.routeBase` | Frontend route base. |
| `data.frontend.menus` | Menu definitions. |
| `data.permissions` | Permission definitions. |
