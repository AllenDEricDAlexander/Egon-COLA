# Dynamic Config Center Manifest

The admin module exposes component metadata from:

```text
GET /api/v1/ddc/manifest
```

The response describes the backend API surface for an external UI or gateway:

```json
{
  "component": "dynamic-config-center",
  "displayName": "Dynamic Config Center",
  "version": "5.2.0-SNAPSHOT",
  "enabled": true,
  "baseApiPath": "/api/v1/ddc",
  "frontendModuleKey": "dynamic-config-center",
  "routeBase": "/components/dynamic-config-center"
}
```

Admin API groups:

```text
/apps
/namespaces
/configs
/publish-tasks
/instances
/cache
/openapi
/manifest
```
