# Egon COLA Dynamic Config Center Component

[English](README.md) | [中文](README.zh-CN.md)

## Overview

`egon-cola-component-dynamic-config-center` is the Egon COLA dynamic configuration center component. It provides a starter SDK for business applications and a standalone admin backend. Business applications bind configuration fields through `@DdcValue`; the starter handles default-value injection, configuration pulls, Redis change listening, runtime refresh, instance registration, heartbeats, and ACK reporting. The admin handles configuration item management, version management, publish tasks, instance status, Redis caching, and consistency strategies.

The component provides an end-to-end backend capability. It does not include a UI, account login, RBAC/permission management, or compatibility targets for MySQL, Spring Boot 2.7, or JDK 17.

## Module Layout

| Module | Description |
|---|---|
| `egon-cola-component-dynamic-config-center-starter` | Business application SDK that provides `@DdcValue`, field binding, configuration refresh, Redis subscription, the Admin OpenAPI client, registration, heartbeat, and ACK support |
| `egon-cola-component-dynamic-config-center-admin` | Standalone management backend that provides REST APIs, JPA persistence, Flyway initialization scripts, Redis caching, publish tasks, and instance management |
| `egon-cola-component-dynamic-config-center-test` | Starter sample application and refresh-flow verification |

## Features

### Starter SDK

After a business application includes the starter, `DdcAutoConfig` auto-configures the following:

| Bean / Capability | Description |
|---|---|
| `DdcValueConverter` | Converts string configurations into `String`, `Integer`, `Long`, `Boolean`, `Double`, `BigDecimal`, `Enum`, `List<String>`, or JSON objects |
| `DdcLocalConfigRepository` | Stores local configuration values, versions, and field bindings |
| `DdcFieldBindingService` | Scans `@DdcValue` fields, injects defaults, and updates fields reflectively during refresh |
| `DdcAdminClient` | Calls the admin OpenAPI over HTTP |
| `DdcRefreshService` | Receives publish messages, compares local versions, updates fields, and reports SUCCESS / FAILED / IGNORED ACKs |
| `DdcRedisChangeListener` | Subscribes to the Redis Topic and receives configuration change messages published by the admin |
| `DdcInstanceService` | Registers instances, sends heartbeats, and takes instances offline |

`@DdcValue` supports two forms:

```java
@DdcValue("rateLimit:100")
private volatile Integer rateLimit;

@DdcValue(value = "", key = "downgradeSwitch", defaultValue = "false", type = Boolean.class)
private volatile Boolean downgradeSwitch;
```

### Admin Backend

The Admin API base path is `/api/v1/ddc`:

| API | Description |
|---|---|
| `GET /api/v1/ddc/manifest` | Manifest for management UI discovery |
| `GET /api/v1/ddc/apps` / `POST /api/v1/ddc/apps` | Query and create applications |
| `GET /api/v1/ddc/namespaces` / `POST /api/v1/ddc/namespaces` | Query and create namespaces |
| `GET /api/v1/ddc/configs` | Query configuration items |
| `POST /api/v1/ddc/configs` | Create a configuration item |
| `PUT /api/v1/ddc/configs/{id}` | Update a configuration item |
| `DELETE /api/v1/ddc/configs/{id}` | Soft-delete a configuration item |
| `POST /api/v1/ddc/configs/{id}/publish` | Publish a configuration |
| `GET /api/v1/ddc/configs/{id}/versions` | Query configuration versions |
| `POST /api/v1/ddc/configs/{id}/rollback` | Roll back a configuration version |
| `GET /api/v1/ddc/publish-tasks` | Query publish tasks |
| `GET /api/v1/ddc/publish-tasks/{changeId}` | Query a single publish task |
| `GET /api/v1/ddc/instances` | Query instances |
| `POST /api/v1/ddc/cache/rebuild` | Rebuild the Redis cache |
| `GET /api/v1/ddc/cache/check` | Check database/cache consistency |

The SDK OpenAPI is under `/api/v1/ddc/openapi` and is called by the starter:

| API | Description |
|---|---|
| `POST /instances/register` | Register an instance |
| `POST /instances/heartbeat` | Send an instance heartbeat |
| `POST /instances/offline` | Take an instance offline |
| `GET /configs/pull` | Pull all configurations |
| `GET /configs/{key}` | Pull a single configuration |
| `POST /publish/ack` | Report a publish ACK |
| `POST /defaults/report` | Report annotation defaults |

### Publish Consistency

The admin supports three publish modes:

| PublishMode | Behavior |
|---|---|
| `ASYNC` | Completes immediately after the message is written to and published through Redis |
| `STRONG_ALL_ACK` | Succeeds only after every target instance returns a successful ACK; fails when all targets finish and any failure or timeout exists |
| `STRONG_QUORUM_ACK` | Succeeds after a majority of instances return successful ACKs; fails when the number of failures/timeouts makes a majority impossible |

### Redis Keys

The starter and admin share `DdcKeys`:

| Key / Topic | Description |
|---|---|
| `ddc:config:{appCode}:{env}:{namespace}:{key}` | Configuration value |
| `ddc:version:{appCode}:{env}:{namespace}:{key}` | Configuration version |
| `ddc:instance:{appCode}:{env}:{namespace}:{instanceId}` | Instance details |
| `ddc:instances:{appCode}:{env}:{namespace}` | Instance set |
| `ddc:publish:{changeId}` | Publish task |
| `ddc:publish:ack:{changeId}` | Publish ACK |
| `ddc:topic:{appCode}:{env}:{namespace}` | Configuration publish Topic |

## Dependency Setup

Include the starter in a business application:

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>top.egon</groupId>
            <artifactId>egon-cola-components-bom</artifactId>
            <version>${egon-cola.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>

<dependencies>
    <dependency>
        <groupId>top.egon</groupId>
        <artifactId>egon-cola-component-dynamic-config-center-starter</artifactId>
    </dependency>
</dependencies>
```

Build and deploy the Admin service as a standalone application within the component:

```bash
./mvnw -B -ntp -pl egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-admin -am package -DskipTests
```

## Configuration

The business application configuration prefix is `egon.cola.component.ddc`:

```yaml
spring:
  application:
    name: order-service

egon:
  cola:
    component:
      ddc:
        enabled: true
        app-code: order-service
        env: dev
        namespace: default
        admin:
          endpoint: http://localhost:18080
          access-key:
          secret-key:
          signature-enabled: false
        redis:
          enabled: true
          host: 127.0.0.1
          port: 6379
          password:
          database: 0
        instance:
          heartbeat-interval-seconds: 10
          heartbeat-timeout-seconds: 30
        consistency:
          ack-enabled: true
          fail-fast: true
```

Default Admin port and Flyway configuration:

```yaml
server:
  port: 18080

spring:
  application:
    name: egon-cola-ddc-admin
  flyway:
    locations: classpath:db/postgresql

egon:
  cola:
    component:
      ddc:
        enabled: false
        admin:
          redis:
            host: 127.0.0.1
            port: 6379
            database: 0
```

Flyway script locations:

```text
classpath:db/postgresql/V1__create_ddc_schema.sql
classpath:db/sqlite/V1__create_ddc_schema.sql
```

## Complete Usage Example

### 1. Bind Dynamic Configuration in a Business Application

Use `volatile` for configuration fields so that other threads can observe refreshed values promptly.

```java
package demo.order;

import org.springframework.stereotype.Service;
import top.egon.cola.component.ddc.annotation.DdcValue;

@Service
public class OrderSwitchService {

    @DdcValue("downgradeSwitch:false")
    private volatile Boolean downgradeSwitch;

    @DdcValue(value = "rateLimit:100", type = Integer.class)
    private volatile Integer rateLimit;

    public boolean shouldDowngrade() {
        return Boolean.TRUE.equals(downgradeSwitch);
    }

    public int rateLimit() {
        return rateLimit == null ? 100 : rateLimit;
    }
}
```

### 2. Create a Configuration Item

```bash
curl -X POST 'http://localhost:18080/api/v1/ddc/configs?operator=admin' \
  -H 'Content-Type: application/json' \
  -d '{
    "appCode": "order-service",
    "env": "dev",
    "namespace": "default",
    "configKey": "rateLimit",
    "configValue": "100",
    "defaultValue": "100",
    "valueType": "INTEGER",
    "description": "Order API rate-limit threshold"
  }'
```

### 3. Publish a Configuration

```bash
curl -X POST 'http://localhost:18080/api/v1/ddc/configs/{configId}/publish?operator=admin' \
  -H 'Content-Type: application/json' \
  -d '{
    "configValue": "200",
    "publishMode": "STRONG_QUORUM_ACK",
    "expectedVersion": 1,
    "timeoutMs": 30000
  }'
```

After publication, the admin updates the database version, writes the configuration value to Redis, and publishes a `DdcPublishMessage`. When the business application receives the message, `DdcRefreshService` updates the local field and reports an ACK.

### 4. Query Publish Tasks and Instances

```bash
curl 'http://localhost:18080/api/v1/ddc/publish-tasks'
curl 'http://localhost:18080/api/v1/ddc/publish-tasks/{changeId}'
curl 'http://localhost:18080/api/v1/ddc/instances?appCode=order-service&env=dev&namespace=default'
```

### 5. Use Annotation Defaults Without a Local Admin

If the admin is not running during local development, disable Redis and fail-fast. Fields will retain the defaults declared in `@DdcValue`:

```yaml
egon:
  cola:
    component:
      ddc:
        enabled: true
        app-code: demo-app
        env: dev
        namespace: default
        redis:
          enabled: false
        consistency:
          fail-fast: false
```

## Design Principles and Implementation Details

### Design Principles

1. Separate the starter from the admin. Business applications depend only on the lightweight SDK, while the management backend is deployed independently.
2. Address configurations with `appCode + env + namespace + configKey` to prevent contamination across applications, environments, and namespaces.
3. Declare defaults alongside business code so local startup can continue when the admin is unavailable.
4. In the publish flow, write the database and version first, then update Redis and publish the message. The business side uses the version to decide whether a refresh is needed.
5. Encapsulate consistency strategies independently so asynchronous, all-ACK, and quorum-ACK modes share the same publish task and ACK models.

### Implementation Details

- `DdcBeanPostProcessor` scans Spring Bean fields annotated with `@DdcValue` and delegates binding to `DdcFieldBindingService`.
- `DdcValueParser` supports the `key:defaultValue` expression and explicit `key`, `defaultValue`, and `type` annotation attributes.
- `DdcValueConverter` converts string configurations into target field types; complex objects use Jackson JSON deserialization.
- `DdcLocalConfigRepository` stores configuration versions and reports an `IGNORED` ACK when it receives an older version.
- `DdcPublishService.publish` uses `TransactionTemplate` to prepare the publish task, target ACK records, and version record, then writes to Redis and publishes the message.
- `PublishFailureRecorder` records failed tasks when publication raises an exception so failed publish paths remain traceable.
- `DdcTraceIdFilter` and the global exception handler provide unified tracing and error responses on the admin side.
- Admin PostgreSQL and SQLite scripts are located under `classpath:db/postgresql` and `classpath:db/sqlite`. Every new database change must use a new Flyway version file; existing scripts must not be modified.

## Boundaries and Operational Notes

- The component does not include a UI, accounts, login, roles, permissions, or RBAC.
- The current scripts cover PostgreSQL and SQLite, not MySQL.
- `@DdcValue` fields are updated through reflection and must not be `final`.
- Defaults come from code annotations; runtime published values come from the admin/Redis.
- With strong-consistency publication enabled, business instances must be able to report ACKs. Otherwise, the publish task remains running or eventually fails.

## Validation Commands

```bash
./mvnw -B -ntp -pl egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-starter -am test
./mvnw -B -ntp -pl egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-admin -am test
./mvnw -B -ntp -pl egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-test -am -Dsurefire.failIfNoSpecifiedTests=false test
```
