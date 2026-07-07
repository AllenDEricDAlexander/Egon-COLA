# Components Dynamic Config Center Design

## 1. Context

Egon-COLA `egon-cola-components` is being expanded with a Dynamic Distributed Config Center component. The requirements document describes a full DDC platform, but this repository has an established component boundary:

1. Runtime components live under `egon-cola-components`.
2. Starter-style components use a component root with `starter`, `admin`, and `test` modules.
3. The `starter` module is the business-facing dependency.
4. The `admin` module is an independently deployable backend service.
5. UI code is not stored in this repository.
6. The repository targets Spring Boot 3.5 and JDK 21.

This design adapts the DDC requirements into that component model.

## 2. Confirmed Decisions

1. Component naming is changed to Egon-COLA naming.
2. UI is not implemented in this repository.
3. Account, login, role, menu permission, and data permission features are not implemented.
4. Supported databases are PostgreSQL and SQLite only.
5. Persistence uses Spring Data JPA.
6. SDK OpenAPI provides optional static access-key and secret-key signing from component configuration, but no account system is introduced.
7. Publish modes include `ASYNC`, `STRONG_ALL_ACK`, and `STRONG_QUORUM_ACK`.
8. Earlier DDC code is reference only; it must not be copied blindly.
9. PostgreSQL and SQLite use independent Flyway script locations.
10. MySQL, Spring Boot 2.7, and JDK 17 compatibility are out of scope.

## 3. Goals

This work delivers a core closed-loop DDC component:

1. `@DdcValue` field declaration and runtime injection.
2. SDK startup registration, default value report, config pull, field binding, local version cache, heartbeat, Redis subscription, runtime refresh, and ACK report.
3. Admin backend APIs for app, namespace, config, version, publish task, ACK, instance, and cache operations.
4. PostgreSQL and SQLite schema initialization through independent Flyway locations.
5. Database-backed config state, version history, publish task state, ACK details, instance metadata, and operation logs.
6. Redis-backed cache, Pub/Sub, heartbeat TTL, and temporary publish state.
7. Strong publish confirmation with all-ACK and quorum-ACK modes.
8. Test/sample module proving the main flow: config persisted, cached, published, refreshed by SDK, ACKed, and audited.

## 4. Non-Goals

1. No UI pages, frontend routes, React, Vue, Vite, or static admin assets.
2. No account login, RBAC, menu permissions, or data permissions.
3. No MySQL support.
4. No Spring Boot 2.7 or JDK 17 compatibility.
5. No Nacos, Apollo, Consul, ZooKeeper, MQ, or external config-center adapter.
6. No full import/export platform in the first implementation.
7. No old-package compatibility bridge for `top.atluofu`.
8. No long-running service startup as part of delivery validation.

## 5. Target Module Structure

The new component is added under `egon-cola-components`:

```text
egon-cola-components/
└── egon-cola-component-dynamic-config-center/
    ├── pom.xml
    ├── README.md
    ├── docs/
    ├── egon-cola-component-dynamic-config-center-starter/
    ├── egon-cola-component-dynamic-config-center-admin/
    └── egon-cola-component-dynamic-config-center-test/
```

Coordinates and package root:

```text
groupId: top.egon
artifactId root: egon-cola-component-dynamic-config-center
starter artifactId: egon-cola-component-dynamic-config-center-starter
admin artifactId: egon-cola-component-dynamic-config-center-admin
test artifactId: egon-cola-component-dynamic-config-center-test
package root: top.egon.cola.component.ddc
configuration prefix: egon.cola.component.ddc
```

The components parent POM aggregates the component root. The component root aggregates `starter`, `admin`, and `test`. The BOM exports only the starter artifact. Admin and test modules are not exported by the BOM.

## 6. Architecture

The component follows the existing component style and the DDC requirement for a flat three-layer backend:

```text
Controller / OpenAPI / Processor / Listener
        ↓
Service
        ↓
Repository / Redis / RemoteClient
```

Admin package layout:

```text
top.egon.cola.component.ddc.admin
├── controller
├── service
├── repository
├── model
│   ├── entity
│   ├── dto
│   ├── vo
│   └── enums
├── config
├── common
└── DynamicConfigCenterAdminApplication.java
```

Starter package layout:

```text
top.egon.cola.component.ddc
├── annotation
├── config
├── processor
├── listener
├── service
├── repository
├── client
├── model
│   ├── dto
│   ├── vo
│   └── enums
└── common
```

The design intentionally avoids DDD-style deep nesting. Admin orchestration lives in services, persistence in repositories, SDK runtime behavior in focused services, and infrastructure-specific clients stay behind local package boundaries.

## 7. Admin Data Model

Admin uses Spring Data JPA. Database scripts are independent for PostgreSQL and SQLite:

```text
admin/src/main/resources/db/postgresql/V1__create_ddc_schema.sql
admin/src/main/resources/db/sqlite/V1__create_ddc_schema.sql
```

Runtime selects the proper location with configuration:

```yaml
spring:
  flyway:
    locations: classpath:db/postgresql
```

The first implementation creates one logical schema version per supported database. No existing migration is modified.

Core tables:

```text
ddc_app              app metadata
ddc_namespace        app + env + namespace isolation
ddc_config_item      current config state
ddc_config_version   config change history
ddc_publish_task     publish task state
ddc_publish_ack      instance ACK detail
ddc_instance         SDK instance metadata and heartbeat state
ddc_operation_log    operation audit log
```

User, role, permission, user-role, and role-permission tables are not created because account and permission features are out of scope.

IDs use string IDs generated in application code so PostgreSQL and SQLite do not diverge on identity behavior. Entity time fields use `LocalDateTime` and persist as `created_at` and `updated_at`.

`ddc_config_item` has a uniqueness constraint:

```text
app_code + env + namespace + config_key
```

`ddc_config_item` also stores:

```text
config_value
default_value
value_type
current_version
description
enabled
deleted
created_at
updated_at
```

Each create, update, delete, and rollback writes `ddc_config_version`. Rollback never mutates old history; it creates a new version whose new value equals a previous version's value.

## 8. Admin API

Admin APIs use:

```text
/api/v1/ddc
```

API groups:

```text
/apps                 app management
/namespaces           namespace management
/configs              config CRUD, versions, rollback, publish
/publish-tasks        publish task query, detail, retry
/instances            SDK instance query and state
/cache                cache rebuild, cache check, cache view
/openapi              SDK register, heartbeat, offline, pull, ACK, default report
```

Management APIs are not protected by an account system in this version. SDK OpenAPI uses optional static access-key and secret-key signing through component configuration, guarded by a `signature-enabled` property, and this is not tied to user accounts.

## 9. Publish Flow

Publish request flow:

```text
1. Controller receives a config publish request.
2. Admin creates a changeId before entering the transaction.
3. ConfigService validates app, env, namespace, key, expected version, value type, and change reason.
4. A database transaction updates ddc_config_item, writes ddc_config_version, creates ddc_publish_task, initializes target ACK rows, and writes ddc_operation_log.
5. After transaction success, Admin writes Redis config and version cache.
6. After Redis cache success, Admin publishes the Redis topic message.
7. SDK receives the message, validates version and checksum, refreshes local fields, and reports ACK.
8. PublishService updates publish task status from ACK details.
```

Transaction failure handling:

```text
1. The main transaction is rolled back.
2. Admin catches the failure and runs an independent failure-record flow.
3. If the publish task was not persisted before the rollback, Admin creates a FAILED task with the same changeId.
4. Admin records the exception message in ddc_publish_task or ddc_operation_log.
5. The Controller reports the exception to the caller and must not return publish success.
```

Redis failure handling:

```text
Database transaction success but Redis cache update failure:
    mark publish task FAILED, record the error, return failure.

Redis cache update success but Redis publish failure:
    mark publish task FAILED, record the error, allow retry.
```

Publish modes:

```text
ASYNC
    Return after Redis publish succeeds. ACK updates task state asynchronously.

STRONG_ALL_ACK
    Wait until every online target instance reports SUCCESS, or timeout.

STRONG_QUORUM_ACK
    Wait until the configured quorum threshold is reached, or timeout.
```

Timeout and partial states remain queryable in `ddc_publish_task` and `ddc_publish_ack`.

## 10. Design Pattern Decisions

`PublishConsistencyPolicy` uses Strategy because ACK completion rules vary by publish mode and are expected to evolve independently from persistence and Redis publishing. This avoids scattering mode-specific `if/else` logic across the publish service.

`DdcValueConverter` starts as a single focused converter class instead of a converter strategy hierarchy. The supported type set is known and small enough for direct implementation.

`DdcFieldBindingService` stays a direct service rather than a layered abstraction. Field binding and refresh are core SDK behavior, and extra interfaces would not add useful extension points in the first implementation.

## 11. Redis Design

Redis is cache and communication infrastructure, not the final source of truth.

Keys:

```text
ddc:config:{appCode}:{env}:{namespace}:{key}
ddc:version:{appCode}:{env}:{namespace}:{key}
ddc:instance:{appCode}:{env}:{namespace}:{instanceId}
ddc:instances:{appCode}:{env}:{namespace}
ddc:publish:{changeId}
ddc:publish:ack:{changeId}
```

Topic:

```text
ddc:topic:{appCode}:{env}:{namespace}
```

The first implementation uses namespace-level topics only. The message carries `configKey`, so SDK refresh remains key-specific without requiring one subscription per config key.

Cache rules:

```text
1. Database is the trusted source.
2. Admin writes Redis current value and version only after database transaction success.
3. Admin publishes the Redis topic only after Redis cache update success.
4. Cache rebuild reads from database and rewrites Redis for a selected app, env, and namespace.
5. Cache check compares database value/version with Redis value/version.
```

## 12. Starter SDK Design

Business code declares dynamic values with:

```java
@DdcValue("downgradeSwitch:0")
private volatile Integer downgradeSwitch;
```

Annotation properties:

```text
value
key
defaultValue
type
required
refreshable
```

SDK startup flow:

```text
1. Read egon.cola.component.ddc properties.
2. Register instance through Admin OpenAPI.
3. Scan Bean fields annotated with @DdcValue.
4. Report annotation defaults to Admin.
5. Pull current config values from Admin.
6. Convert values and reflectively assign fields.
7. Record local versions.
8. Start Redis topic listener.
9. Start heartbeat task.
```

Runtime refresh flow:

```text
1. Receive Redis message with changeId, configKey, valueType, configValue, targetVersion, and checksum.
2. Validate app, env, namespace, key, checksum, and local binding existence.
3. Ignore lower or equal versions.
4. Convert the new value before touching the existing field.
5. Assign all bound fields for the key.
6. Update local version after all assignments succeed.
7. Report SUCCESS ACK.
8. Report IGNORED ACK for lower-version or duplicate messages.
9. Report FAILED ACK for conversion or reflection failure without changing the current field value or local version.
```

The binding cache is thread-safe:

```text
Map<configKey, List<DdcFieldBinding>>
Map<configKey, localVersion>
```

Supported value types:

```text
String
Integer / int
Long / long
Boolean / boolean
Double / double
BigDecimal
Enum
List<String>
JSON object to declared Java type
```

AOP proxy compatibility is handled by resolving the target class and target object through Spring utilities before field scanning and assignment.

## 13. Instance and ACK Handling

SDK heartbeat:

```text
1. Report heartbeat through Admin OpenAPI.
2. Write Redis instance key with TTL.
3. Add instanceId to the Redis instance set.
```

Admin instance query uses database metadata and Redis TTL or last heartbeat time to determine online/offline status.

SDK graceful shutdown calls the offline OpenAPI. Admin marks the instance OFFLINE and removes its Redis instance key.

ACK handling:

```text
1. SDK reports ACK through Admin OpenAPI.
2. Admin persists ddc_publish_ack first.
3. Admin updates ddc_publish_task counts and status.
4. Duplicate ACK is idempotent by changeId + instanceId.
5. Redis ACK state is temporary acceleration only; database remains final.
```

## 14. Error Handling

Core failure behavior:

```text
Database transaction failure:
    rollback, create independent FAILED record when possible, report exception.

Redis cache update failure:
    mark publish task FAILED, return failure.

Redis publish failure:
    mark publish task FAILED, allow retry.

SDK type conversion failure:
    keep old field value and local version, report FAILED ACK.

SDK reflection failure:
    keep old local version, report FAILED ACK.

SDK low-version message:
    ignore and report IGNORED ACK.

Duplicate Redis message:
    handle idempotently and report existing or IGNORED result.

Admin unavailable at SDK startup:
    follow fail-fast and Redis fallback configuration.
```

## 15. Validation Plan

Unit tests:

```text
DdcValue expression parsing
type conversion
field binding cache
local version comparison
PublishConsistencyPolicy
Redis key and topic construction
Admin request validation
```

Admin integration tests:

```text
JPA repository read/write
PostgreSQL schema script validation
SQLite schema script validation
config create/update/delete/rollback version records
publish task failure marking
ACK idempotency
cache rebuild and check
```

Starter and sample tests:

```text
@DdcValue startup injection
Redis topic refresh
multi-field and multi-bean binding
low-version IGNORED ACK
type conversion failure without field pollution
SUCCESS, FAILED, and IGNORED ACK report
heartbeat and graceful offline
```

Recommended Maven validation after implementation:

```text
mvn -pl egon-cola-components/egon-cola-component-dynamic-config-center -am test
```

If targeted reactor tests need `-am`, use `-Dsurefire.failIfNoSpecifiedTests=false` to avoid unrelated module no-test failures.

## 16. Acceptance Criteria

1. The new DDC component is aggregated by `egon-cola-components`.
2. The BOM exports only `egon-cola-component-dynamic-config-center-starter`.
3. The starter auto-configures in a Spring Boot 3.5 business application.
4. The admin module packages as an independent jar and includes a Dockerfile.
5. PostgreSQL and SQLite have independent Flyway schema scripts.
6. The main flow works: config persisted, Redis cache updated, message published, SDK field refreshed, ACK reported, task completed or failed.
7. Publish task failure is visible for database, Redis cache, and Redis publish failures.
8. No UI, account, role, permission, MySQL, Spring Boot 2.7, or JDK 17 support is introduced.
9. Existing dynamic-thread-pool and common components are not refactored as part of this work.
