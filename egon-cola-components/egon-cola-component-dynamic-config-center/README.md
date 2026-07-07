# Egon COLA Dynamic Config Center Component

This component provides Spring Boot dynamic configuration through a business-facing starter and an independently deployable admin backend.

## Modules

| Module | Purpose |
|---|---|
| `egon-cola-component-dynamic-config-center-starter` | Business application SDK, `@DdcValue`, runtime refresh, heartbeat, ACK. |
| `egon-cola-component-dynamic-config-center-admin` | Backend APIs, persistence, Redis cache, publish task management, instance state. |
| `egon-cola-component-dynamic-config-center-test` | Sample application and integration-style validation. |

## Configuration Prefix

`egon.cola.component.ddc`

## Admin API

The admin API base path is `/api/v1/ddc`.

## Boundary

This repository stores the starter, admin backend, and test/sample module. It does not store UI code, login/account features, RBAC, MySQL support, Spring Boot 2.7 compatibility, or JDK 17 compatibility.
