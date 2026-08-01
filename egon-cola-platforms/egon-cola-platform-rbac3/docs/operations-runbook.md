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

## 3. Startup sequence

1. Verify PostgreSQL and all three Redis roles are externally healthy.
2. Verify DDC Admin, Gateway Admin and Gateway Engine are already running.
3. Verify RSA/audit key files and OAuth/HMAC credentials are mounted with least
   privilege.
4. Start the first Admin instance through deployment tooling.
5. Confirm both Flyway history tables, liveness and persistence readiness.
6. Confirm Definition acknowledgement independently.
7. Confirm its DDC `HTTP_PROVIDER` lease independently.
8. Confirm the explicit Gateway Release and runtime consistency independently.
9. Route a request through Gateway and record the result.
10. Repeat for the second Admin instance using distinct port, instance ID,
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
- Definition status/set ID and warnings;
- DDC lease instance ID, expiry and last heartbeat;
- Gateway Release ID/status, engine-observed version and consistency;
- routed request trace ID, response code and selected provider instance when
  available.

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
