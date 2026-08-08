# RBAC3 Operations Runbook

## 1. Preconditions and ownership

Operators own PostgreSQL, the three Redis roles, DDC Admin, Gateway Admin,
Gateway Engine, TLS, secret distribution, OAuth/HMAC credentials, DNS and
process supervision. RBAC3 scripts never create or start those services.

Use Java 21. Admin is packaged as the normal library JAR plus an executable JAR
with classifier `exec`. Node 24 is required only for building the SDK/Admin Web.

Before deployment, allocate per instance:

- a unique `RBAC3_INSTANCE_ID`;
- a unique `RBAC3_SNOWFLAKE_MACHINE_ID` in `0..1023` (no host-derived default);
- a distinct advertised port and reachable advertised host;
- a writable, instance-specific Gateway report state file;
- the same artifact version but a traceable build ID;
- graceful shutdown time greater than worker drain time.

## 2. Required Admin configuration

### 2.1 PostgreSQL and dual Flyway

| Variable | Required semantics |
| --- | --- |
| `RBAC3_POSTGRES_URL` | Explicit PostgreSQL JDBC URL; no localhost fallback |
| `RBAC3_POSTGRES_USER` / `RBAC3_POSTGRES_PASSWORD` | Deployment-managed credential |
| `RBAC3_SNOWFLAKE_MACHINE_ID` | Unique explicit integer `0..1023` per active instance |

Spring Boot Flyway is disabled. RBAC3 configuration creates two Flyway runners
over the same DataSource:

- RBAC3 schema location with history table `flyway_schema_history_rbac3`;
- Transactional Outbox PostgreSQL location with history table
  `flyway_schema_history_outbox`.

Both must succeed before the persistence adapters become ready. Never change the
existing RBAC3 V1 migration. A schema change uses exactly the next version.
Back up schema, both Flyway histories and outbox records in one database-consistent
snapshot.

### 2.2 Three independent Redisson clients

RBAC3 topology intentionally has three named clients; do not alias them to an
unqualified primary bean:

| Bean | Owner | Data |
| --- | --- | --- |
| `ddcRegistryRedissonClient` | DDC registry integration | Provider leases and DDC registry state |
| `gatewayRateLimitRedissonClient` | Gateway Engine | Gateway policies, release/rate-limit runtime state |
| `rbac3RuntimeRedissonClient` | RBAC3 | Session snapshots, versions, fences and projection checkpoints |

Admin configuration uses `RBAC3_RUNTIME_REDIS_ADDRESS`, database, timeout and a
password **file**. DDC registry uses its explicit mode/nodes or host/port,
password and database. Gateway owns its own corresponding rate-limit Redis
configuration. Do not silently reuse database numbers, credentials or client
beans across these roles.

### 2.3 JWT and audit keys

| Variable | Rule |
| --- | --- |
| `RBAC3_JWT_PRIVATE_KEY_FILE` | Deployment-owned PKCS#8 RSA private key, readable only by Admin |
| `RBAC3_JWT_PUBLIC_KEY_FILE` | Matching public key published through JWKS |
| `RBAC3_JWT_KID` | Stable active Key Ring identifier |
| `RBAC3_JWT_ISSUER` | Exact trusted issuer URI/name |
| `RBAC3_JWT_AUDIENCES` | Explicit audiences for Starter/Gateway consumers |
| `RBAC3_AUDIT_CURSOR_SECRET_FILE` | Independent secret for signed audit cursors |

Key Ring phases are `PREPARED -> SIGNING -> VERIFY_ONLY -> RETIRED`. Key
material rotation is cryptographic operations work, not a business role-rotation
workflow. Keep old public keys through the maximum access-token validation
window. Do not place access/refresh tokens or private keys in logs or evidence.

### 2.4 Gateway Definition reporting

| Variable | Rule |
| --- | --- |
| `GATEWAY_ADMIN_BASE_URL` | Explicit Gateway Admin URL |
| `DDC_BIZ_CODE` | Exact DDC v3 business domain used by the Definition and provider identity |
| `GATEWAY_REPORT_ACCESS_KEY` / `GATEWAY_REPORT_SECRET_KEY` | HMAC write credential scoped to `rbac3-admin` |
| `GATEWAY_REPORT_STATE_FILE` | Durable instance-local receipt state |
| `RBAC3_ARTIFACT_VERSION` / `RBAC3_BUILD_ID` | Traceable definition build identity |
| `RBAC3_DECLARED_HOSTS` | Explicit host allowlist included in report metadata |

Definition reporting is fail-fast at startup. Do not reuse the report HMAC
credential for status reads. Status reads use the OAuth token file configured by
`GATEWAY_STATUS_OAUTH_TOKEN_FILE`, with read-only Gateway capabilities.

### 2.5 DDC provider registration

| Variable | Rule |
| --- | --- |
| `DDC_ADMIN_ENDPOINT` | Explicit DDC Admin endpoint |
| `DDC_REPORT_ACCESS_KEY` / `DDC_REPORT_SECRET_KEY` | DDC signed reporting credential |
| `DDC_BIZ_CODE` | DDC v3 business domain; must match Gateway Definition and provider queries |
| `DEPLOYMENT_ENV` / `DEPLOYMENT_NAMESPACE` | Must match Gateway Definition and Release identity |
| `RBAC3_ADVERTISED_HOST` / `RBAC3_ADVERTISED_PORT` | Address reachable by Gateway Engine |
| `RBAC3_INSTANCE_ID` | Unique lease identity and Outbox node ID |

The service registers under `DDC_BIZ_CODE + rbac3-admin` as `HTTP_PROVIDER`,
protocol `http`, service name `rbac3-admin`, group `default`, and the deployed artifact version. Lease is 30
seconds with a 10-second heartbeat. DDC must not advertise the instance before
the provider port is actually reachable.

### 2.6 DDC configuration client and runtime policy

Configuration resource identity is `bizCode + appCode + env + resourceName`;
namespace bindings control visibility but are not part of that identity. It is
consumed through a `CONFIG_CLIENT` lease. The provider registration above uses a
different service scope: `bizCode + appCode + env + namespace + serviceKind +
protocol + serviceName + group + version` and an `HTTP_PROVIDER` lease. The two
leases have independent lease IDs, expiry, heartbeat, failure and recovery state.
Never infer one from the other.

| Key | Default | Range | Relationship/effect |
| --- | ---: | ---: | --- |
| `rbac3.access-token-ttl-seconds` | 900 | 300..1800 | New Access Tokens only |
| `rbac3.refresh-token-ttl-seconds` | 604800 | 86400..2592000 | Must be at least Absolute; new Refresh Tokens/Sessions only |
| `rbac3.session-idle-timeout-seconds` | 1800 | 300..28800 | Must not exceed Absolute; new/refreshed Sessions only |
| `rbac3.session-absolute-timeout-seconds` | 43200 | 3600..86400 | Between Idle and Refresh; new/refreshed Sessions only |
| `rbac3.maximum-active-roots` | 16 | 1..32 | New role-activation replacement commands only |

Each DDC message carries the complete YAML resource. Update all related policy
leaves in one valid document and publish it once. DDC replaces the dynamic
PropertySource transactionally; if any typed applier rejects the candidate, it
rolls back the property source and all already-applied leaves before returning a
failed ACK.

The declarations use `refreshable = false` to prevent reflective mutation;
exact typed appliers validate the whole immutable policy snapshot instead. An
invalid update produces a bounded FAILED ACK and records key/version/error code,
while the previous policy and repository version/resource checksum remain
last-known-good. Do not retry the same version with different content: DDC treats
that as a checksum conflict. Correct the YAML document and publish a higher
version. A successful higher version clears the failure for that resource.

Dynamic configuration never rewrites already issued token expiry, already
persisted Session expiry or already committed active-role sets. Reauthentication,
Session refresh/creation or a new activation command is required to consume the
new value.

These five keys are scalar policy values, not a secret channel. Never publish
passwords, access/secret keys, OAuth or refresh tokens, lease credentials,
private keys, hashes, bootstrap administrator passwords or bootstrap commands
through DDC. Do not put them in ACK evidence, status output, metrics or document
examples.

### 2.7 Gateway Interface Catalog and Release

Spring MVC mappings provide Method, Path, Consumes, Produces and parameters;
the existing `@EgonHttpService`, `@GatewayInterfaceGroup`, `@GatewayOperation`
and schema-field annotations provide business documentation. Gateway Interface
Catalog is the only API document center for RBAC3. Do not deploy a parallel
Swagger/Springdoc catalog or add real credentials as schema examples.

A Definition ACK proves only that Gateway Admin accepted the catalog. It does
not publish traffic rules. An authorized operator must explicitly create/publish
the intended Gateway Release, then verify Engine consistency and a routed
request. RBAC3 never auto-publishes a Release.

## 3. Startup sequence

1. Verify PostgreSQL and all three Redis roles are externally healthy.
2. Verify DDC Admin, Gateway Admin and Gateway Engine are already running.
3. Verify RSA/audit key files and OAuth/HMAC credentials are mounted with least
   privilege.
4. Start the first Admin instance through deployment tooling.
5. Confirm both Flyway history tables, liveness and persistence readiness.
6. Confirm the DDC `CONFIG_CLIENT` session is `READY`, all five startup versions
   are present, and there is no unresolved apply failure. The RBAC3 publication
   gate will not publish the root HTTP port before this point.
7. Confirm Definition acknowledgement independently.
8. Confirm its separate, unexpired DDC `HTTP_PROVIDER` lease independently.
9. Confirm the explicitly published Gateway Release and runtime consistency.
10. Route a request through Gateway and record the result.
11. Repeat for the second Admin instance using distinct port, instance ID,
    build ID and Snowflake machine ID.

Readiness describes the process's ability to serve safely; it must not report a
Gateway route as healthy merely because the JVM is alive.

## 4. Graceful shutdown

1. Remove the instance from external traffic through deployment tooling.
2. Wait for in-flight HTTP requests to drain.
3. Stop assignment/mutation polling and wait for claimed batches to finish or
   return to recoverable state.
4. Flush committed Outbox wakeups; do not claim delivery completion solely from
   task creation.
5. Deregister or allow the DDC provider lease to expire.
6. Close Gateway reporting/status clients.
7. Close `rbac3RuntimeRedissonClient`, then DDC-related clients owned by the
   process, then JPA/DataSource.
8. Let Spring graceful shutdown complete within the configured 30-second phase.

Never reuse an instance ID while a previous process may still own leases or
worker records.

## 5. First administrator bootstrap

Run the packaged Admin command with deployment-managed PostgreSQL, Flyway,
Outbox, ID generator and key configuration. The command selects a non-web
Spring context and exits after the transaction:

```bash
java -jar egon-cola-platform-rbac3-admin.jar \
  bootstrap-platform-admin \
  --tenant-code platform \
  --username <username>
```

Supply a 12-64 character password through interactive standard input or redirect
a controlled secret file descriptor to standard input. Never place it in argv,
environment variables, configuration files, evidence, or logs. The transaction
uses a PostgreSQL advisory lock and creates the Tenant, built-in application,
permissions, platform administrator role, user credential, assignment, audit and
Outbox event. A pre-existing active platform administrator or Tenant is a hard
stop. Account-loss recovery is a separate operator procedure requiring a reason,
ticket and critical audit; do not rerun bootstrap as recovery.

## 6. Mutation, Fence and Outbox recovery

- A control-plane mutation is not externally effective until its immutable
  Redis snapshot is published and the matching Fence is opened.
- `FAILED` mutation records remain visible through `/runtime/mutations`.
- Recovery accepts one stable mutation ID and is idempotent. Re-read the record
  after an uncertain response; never issue a broad retry loop.
- Check PostgreSQL transaction outcome before manipulating Redis. Redis state is
  a projection, not the source of truth.
- Outbox delivery uses its public component API and storage ownership. Do not
  query or modify internal Outbox tables from RBAC3 application code.
- Investigate repeated failure codes before retry. Bounded backoff prevents a
  thundering herd; a stuck Fence must remain visible and fail closed.

## 7. Backup and restore

Back up:

- the PostgreSQL database containing RBAC3 facts, mutation journal, audit,
  idempotency, Key Ring metadata, both Flyway histories and Outbox records;
- encrypted/private key files through the organization's secret backup process;
- Gateway Release/configuration and DDC configuration through their owners;
- deployment manifests containing instance/build/version identity, without
  plaintext secrets.

Redis authorization snapshots and leases are rebuildable runtime state. After a
database restore, keep traffic closed, run Flyway validation, restore required
keys, start one Admin instance, rebuild projections, verify Fences/outbox backlog,
then re-establish Definition, Lease, Release and routing in that order. Do not
restore Redis snapshots from a different database point-in-time and assume they
match PostgreSQL versions.

## 8. Host-local verification fixture

The scripts use only explicit configuration. `--check-config` performs no
network/database/Redis request. Preparation requires a strict run ID, schema
`rbac3_it_<runId>`, prefix `rbac3:it:<runId>:` and dedicated tenant. It records
the exact schema and keys in a mode-600 state file before mutation.

Cleanup revalidates the state file and deletes only the recorded schema and
exact keys. It never operates on an entire PostgreSQL database or Redis logical
database and never discovers cleanup targets by wildcard.

Live topology verification requires two already-running Admin processes. At
failover checkpoints it pauses and instructs the operator to change process
state externally. After both instances are unavailable it expects the explicitly
configured Gateway error status, then asks the operator to restore both and
verifies recovery.

## 9. Observability and incident checklist

Record independently:

- liveness/readiness and build identity for each Admin instance;
- RBAC3 and Outbox Flyway state;
- runtime Redis availability, projection checkpoint, mutation backlog and Fence;
- Outbox pending/retry/dead counts;
- DDC Config Client state, instance ID, lease fingerprint/expiry, five current
  config versions and last apply failure key/version/code;
- Definition status/set ID and warnings;
- DDC HTTP Provider lease instance ID, expiry and last heartbeat;
- Gateway Release ID/status, engine-observed version and consistency;
- routed request trace ID, response code and selected provider instance when
  available.

Fixed low-cardinality metrics are:

- `rbac3_ddc_config_apply_total{key,status}` where key is one of the five keys
  and status is only `success|failed`;
- `rbac3_ddc_config_snapshot_version{key}`;
- `rbac3_ddc_config_ready`;
- `rbac3_gateway_definition_operation_count`.

Raw values, lease IDs and instance IDs are not metric labels.

### 9.1 DDC/Gateway incident order and LKG recovery

Always inspect in this order, without skipping a fact:

```text
DDC Config Client state/session
  -> current five config versions / last apply error code
  -> Gateway Definition status
  -> DDC HTTP_PROVIDER lease and expiry
  -> Gateway Release / Engine consistency
  -> routed request evidence
```

- If the Config Client is not `READY` or lacks a `CONFIG_CLIENT` session, fix
  DDC registration/connectivity first. Provider publication is intentionally
  blocked; do not bypass the gate.
- If a version did not advance, compare only key, target/current version,
  checksum outcome and bounded error code. Do not print the raw value. Correct
  range/relationship errors and publish a higher valid version. The previous
  snapshot is still the LKG; a failed apply alone does not authorize changing
  readiness to UP or DOWN by hand.
- If Definition is rejected, fix the Mapping/annotation/schema contract and
  report a new Definition. Do not create a second documentation source.
- If the HTTP Provider lease is missing, recovering or expired, repair provider
  registration/heartbeat after Config Ready. A `REGISTERED` string with an
  expired lease is still `NOT_ROUTABLE`.
- If Release or consistency is not successful, publish/repair the explicit
  Release through Gateway operations. Definition acceptance and provider
  presence are not release evidence.
- Only a routed request with timestamp/trace and selected provider proves the
  complete path at that moment.

On an authorization incident, preserve signed/redacted audit evidence, mutation
ID, Session ID, tenant/application IDs and policy versions. Never capture raw
passwords, tokens, refresh cookies, private keys or unmasked sensitive fields.

## 10. Evidence boundary

Maven tests and static scans prove source-level contracts. Mocked Redis and
in-memory concurrency prove deterministic behavior but not external topology.
Host-local dependency checks prove only the endpoints observed at that moment.
The interactive topology script proves the particular two-instance route and
failover run recorded in its evidence; it does not prove all production nodes or
future availability. Use the evidence template and state these limits explicitly.
