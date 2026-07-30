# Dynamic Config Center Manifest

The admin module exposes component metadata from:

```text
GET /api/v1/ddc/manifest
```

Response fields:

| Field | Meaning |
|---|---|
| `component` | Stable component key, `dynamic-config-center`. |
| `displayName` | Human-readable component name. |
| `version` | Backend component version. |
| `enabled` | Whether this backend module is enabled. |
| `baseApiPath` | Admin API base path. |
| `frontendModuleKey` | External UI module key. |
| `routeBase` | Suggested external UI route base. |

The version defaults to the Starter's filtered `sdk.version` metadata. Operators can override it with
`egon.cola.component.ddc.admin.manifest.version`.

Example:

```json
{
  "component": "dynamic-config-center",
  "displayName": "Dynamic Config Center",
  "version": "<component-version>",
  "enabled": true,
  "baseApiPath": "/api/v1/ddc",
  "frontendModuleKey": "dynamic-config-center",
  "routeBase": "/components/dynamic-config-center"
}
```

Admin API groups:

| Group | Path |
|---|---|
| Apps | `/api/v1/ddc/apps` |
| Namespaces | `/api/v1/ddc/namespaces` |
| Configs | `/api/v1/ddc/configs` |
| Publish tasks | `/api/v1/ddc/publish-tasks` |
| Instances | `/api/v1/ddc/instances` |
| Cache | `/api/v1/ddc/cache` |
| SDK OpenAPI | `/api/v1/ddc/openapi` |
| Manifest | `/api/v1/ddc/manifest` |
