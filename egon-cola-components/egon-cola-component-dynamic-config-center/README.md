# Egon COLA Dynamic Config Center Component

This component provides dynamic configuration for Spring Boot applications through a starter SDK and an independently deployable admin backend.

## Modules

| Module | Purpose |
|---|---|
| `egon-cola-component-dynamic-config-center-starter` | Business application SDK, `@DdcValue`, runtime refresh, heartbeat, and ACK reporting. |
| `egon-cola-component-dynamic-config-center-admin` | Admin backend APIs, JPA persistence, Redis cache, publish task management, and instance state. |
| `egon-cola-component-dynamic-config-center-test` | Sample application and integration-style validation. |

## Starter Dependency

```xml
<dependency>
    <groupId>top.egon</groupId>
    <artifactId>egon-cola-component-dynamic-config-center-starter</artifactId>
    <version>5.2.0-SNAPSHOT</version>
</dependency>
```

## Configuration

Configuration prefix:

```text
egon.cola.component.ddc
```

Example application YAML:

```yaml
egon:
  cola:
    component:
      ddc:
        enabled: true
        app-code: demo-app
        env: dev
        namespace: default
        admin:
          endpoint: http://localhost:18080
          signature-enabled: false
        redis:
          enabled: true
          host: 127.0.0.1
          port: 6379
          database: 0
        consistency:
          ack-enabled: true
          fail-fast: true
```

## Admin API

The admin API base path is:

```text
/api/v1/ddc
```

SDK OpenAPI endpoints are under:

```text
/api/v1/ddc/openapi
```

## Flyway Locations

PostgreSQL:

```yaml
spring:
  flyway:
    locations: classpath:db/postgresql
```

SQLite:

```yaml
spring:
  flyway:
    locations: classpath:db/sqlite
```

## Validation

Run the component test suite:

```bash
mvn -pl egon-cola-components/egon-cola-component-dynamic-config-center -am test
```

Build packages without starting services:

```bash
mvn -pl egon-cola-components/egon-cola-component-dynamic-config-center -am package -DskipTests
```

## Non-Goals

This component intentionally does not include UI, account/login features, RBAC/permission management, MySQL support, Spring Boot 2.7 compatibility, or JDK 17 compatibility.
