# Egon COLA Access Guard Starter

[中文](README.zh-CN.md)

`egon-cola-component-access-guard-starter` is a Spring Boot starter for rule-based admission control and guarded
business execution.

It provides one unified runtime model for:

- Spring AOP method interception
- Programmatic guard execution
- `CompletionStage` lifecycle governance
- Reactor `Mono` / `Flux` lifecycle governance
- Optional Bytecode Agent enhancement
- Local or Redisson-backed guard state
- Metrics, structured events, logging, and an Actuator endpoint

The component is intended for business entry points such as coupon claims, lotteries, login attempts, payment
operations, risk checks, expensive queries, and hot API protection.

> Current document target: Egon COLA `5.3.2`, Java 21+, Spring Boot 3.5.x.

---

## 1. What Access Guard Solves

Business access control is often scattered across controllers, service methods, Redis scripts, filters, and exception
handlers. That fragmentation creates several problems:

- different entry points implement different rules;
- rate limiting and deny-list logic drift over time;
- timeout and fallback behavior are inconsistent;
- sensitive identifiers may accidentally appear in Redis keys or metric tags;
- local and distributed execution paths behave differently;
- asynchronous and reactive methods are frequently treated as completed too early.

Access Guard centralizes these concerns around a named rule.

```text
method / constructor / programmatic request
                    |
                    v
              resolve rule
                    |
                    v
             build guard key
                    |
                    v
DenyList -> AllowList -> PenaltyBox -> RateLimit
                    |
                    v
       TimeLimit -> business operation
                    |
                    v
       rejection / fallback / final event
```

The policy order is fixed and is part of the public contract.

---

## 2. Main Capabilities

| Capability                | Description                                                                                 |
|---------------------------|---------------------------------------------------------------------------------------------|
| Unified rule engine       | AOP, programmatic, async, reactive, and Agent entries share the same rule semantics.        |
| Deny list                 | Reject known blocked identities before all bypass decisions.                                |
| Allow list                | Work as a gate or bypass only selected downstream policies.                                 |
| Penalty box               | Escalate repeated rate-limit violations into a temporary penalty.                           |
| Token-bucket rate limit   | Protect hot entry points with local or Redisson-backed atomic state.                        |
| Time limit                | Observe or enforce execution duration using caller thread, bounded pool, or virtual thread. |
| Rejection resolution      | Throw, invoke fallback, deserialize JSON, or return `null`.                                 |
| Privacy-safe keys         | Normalize key parts and hash them with HMAC-SHA-256 before storage.                         |
| Failure policies          | Configure fail-closed, fail-open, or local fallback by failure point.                       |
| Async lifecycle           | Track `CompletionStage` completion, timeout, cancellation, and rejection.                   |
| Reactive lifecycle        | Apply rules lazily at subscription and emit one terminal outcome.                           |
| Agent mode                | Govern private/static/self-invoked methods and explicitly annotated constructors.           |
| Observability             | Publish final/stage events, Micrometer metrics, logs, and a read-only endpoint.             |
| Strict startup validation | Unknown properties and invalid rule combinations fail application startup.                  |

---

## 3. Requirements

- Java 21 or later
- Spring Boot 3.x
- Spring AOP for the default `AOP` engine
- A non-empty HMAC secret when at least one rule exists
- A `RedissonClient` when `storage: REDISSON` is selected
- Reactor only when guarding `Mono` or `Flux`
- `egon-cola-component-bytecode-starter` and the Java Agent when using `AGENT`

---

## 4. Dependency

### 4.1 Direct dependency

```xml
<dependency>
    <groupId>top.egon</groupId>
    <artifactId>egon-cola-component-access-guard-starter</artifactId>
    <version>5.3.2</version>
</dependency>
```

### 4.2 Using the Egon COLA BOM

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>top.egon</groupId>
            <artifactId>egon-cola-components-bom</artifactId>
            <version>5.3.2</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>

<dependencies>
    <dependency>
        <groupId>top.egon</groupId>
        <artifactId>egon-cola-component-access-guard-starter</artifactId>
    </dependency>
</dependencies>
```

Optional integrations such as Redisson, Actuator, Micrometer, Reactor, and Bytecode Agent support must still be
available in the application when selected.

---

## 5. Five-Minute Quick Start

This example applies a per-user token-bucket rate limit to a service method.

### 5.1 Configure the rule

```yaml
egon:
  cola:
    component:
      access-guard:
        enabled: true
        engine: AOP
        storage: LOCAL

        key:
          contributors:
            - ARGUMENT
          hmac-secret: ${ACCESS_GUARD_HMAC_SECRET}

        rules:
          draw:
            enabled: true

            key:
              contributors:
                - ARGUMENT

            rate-limit:
              enabled: true
              algorithm: TOKEN_BUCKET
              capacity: 10
              refill-tokens: 10
              refill-period: 1s
              requested-tokens: 1

            rejection:
              mode: THROW
```

Set the secret through the environment:

```bash
export ACCESS_GUARD_HMAC_SECRET='replace-with-a-long-random-secret'
```

### 5.2 Annotate the method

```java
import top.egon.cola.component.accessguard.api.AccessGuard;
import top.egon.cola.component.accessguard.api.GuardKey;

@Service
public class DrawApplicationService {

    @AccessGuard("draw")
    public DrawResult draw(@GuardKey("userId") String userId) {
        return executeDraw(userId);
    }

    private DrawResult executeDraw(String userId) {
        return new DrawResult(true, userId);
    }
}
```

The annotation value is the rule ID. It must match a key under:

```text
egon.cola.component.access-guard.rules
```

### 5.3 Handle rejections

With `rejection.mode: THROW`, the starter throws:

```java
top.egon.cola.component.accessguard.api.AccessGuardRejectedException
```

The exception contains a structured `GuardOutcome`:

```java
try {
    drawApplicationService.draw(userId);
} catch (AccessGuardRejectedException exception) {
    GuardOutcome outcome = exception.outcome();

    log.warn(
        "Access rejected: rule={}, decision={}, retryAfter={}",
        outcome.ruleId(),
        outcome.decision(),
        outcome.retryAfter()
    );
}
```

---

## 6. Annotation Model

### 6.1 `@AccessGuard`

The aggregate annotation is the default choice.

```java
@AccessGuard("payment-submit")
public PaymentResult submit(@GuardKey("customer") Long customerId) {
    return paymentService.submit(customerId);
}
```

Targets:

- type
- method
- explicit constructor

Attributes:

| Attribute | Meaning                                                                     |
|-----------|-----------------------------------------------------------------------------|
| `value`   | Required rule ID.                                                           |
| `key`     | Optional explicit binding key made available to the key-resolution context. |

A method annotation overrides a type-level annotation.

```java
@AccessGuard("customer-api")
@Service
public class CustomerService {

    public Customer find(Long id) {
        return repository.find(id);
    }

    @AccessGuard("customer-export")
    public byte[] export() {
        return exporter.export();
    }
}
```

### 6.2 Dedicated annotations

The starter also exposes:

```java
@AllowListGuard("partner-api")
@RateLimitGuard("search")
@TimeLimitGuard("report")
```

A dedicated annotation is valid only when the bound rule enables exactly one matching policy.

Examples:

```yaml
rules:
  search:
    rate-limit:
      enabled: true
```

```java
@RateLimitGuard("search")
public SearchResult search(@GuardKey String keyword) {
    return searchService.search(keyword);
}
```

The following configuration is invalid for `@RateLimitGuard` because more than one policy is enabled:

```yaml
rules:
  search:
    deny-list:
      enabled: true
    rate-limit:
      enabled: true
```

Use `@AccessGuard("search")` for multi-policy rules.

### 6.3 One binding per method

Do not place multiple guard annotations on the same method.

```java
// Invalid
@AccessGuard("search")
@RateLimitGuard("search")
public Result search(String text) {
    return doSearch(text);
}
```

Startup validation rejects ambiguous bindings.

---

## 7. Complete Configuration Shape

```yaml
egon:
  cola:
    component:
      access-guard:
        enabled: true

        # AOP, AGENT, DISABLED
        engine: AOP

        # LOCAL, REDISSON
        storage: LOCAL

        defaults:
          # THROW, FALLBACK, RETURN_JSON, RETURN_NULL
          rejection: THROW

        key:
          # ARGUMENT, CLIENT_IP, PRINCIPAL, HTTP_HEADER,
          # ATTRIBUTE:<name>, GLOBAL
          contributors:
            - ARGUMENT

          # IP addresses or CIDR blocks.
          trusted-proxies: []

          # Required header names when HTTP_HEADER is used.
          headers: []

          hmac-secret: ${ACCESS_GUARD_HMAC_SECRET}
          max-part-length: 1024

        redisson:
          client-bean-name: redissonClient
          key-prefix: egon:access-guard
          application: ${spring.application.name:}

        local:
          max-entries: 100000
          cleanup-interval: 1m
          idle-ttl: 10m

        thread-pool:
          name: access-guard
          core-pool-size: 4
          max-pool-size: 16
          queue-capacity: 1024
          keep-alive: 60s

        rules:
          draw:
            enabled: true

            key:
              contributors:
                - ARGUMENT

            deny-list:
              enabled: false
              data-version: v1

            allow-list:
              enabled: false
              # GATE, BYPASS_RATE_LIMIT,
              # BYPASS_RATE_LIMIT_AND_PENALTY
              mode: GATE
              data-version: v1

            penalty-box:
              enabled: false
              threshold: 5
              violation-ttl: 1m
              penalty-ttl: 10m

            rate-limit:
              enabled: true
              algorithm: TOKEN_BUCKET
              capacity: 100
              refill-tokens: 100
              refill-period: 1s
              requested-tokens: 1

            time-limit:
              enabled: false
              # DISABLED, OBSERVE_ONLY, ENFORCE
              mode: DISABLED
              # CALLER_THREAD, THREAD_POOL, VIRTUAL_THREAD
              executor: CALLER_THREAD
              timeout: 1s
              cancel-running-task: true

            rejection:
              # Null means inherit defaults.rejection.
              mode: THROW
              fallback-method: ""
              return-json: ""

            failure-policies:
              key-resolution: FAIL_CLOSED
              deny-list-store: FAIL_CLOSED
              allow-list-store: FAIL_CLOSED
              penalty-store: LOCAL_FALLBACK
              rate-limit-backend: LOCAL_FALLBACK
              execution: FAIL_CLOSED
              observability: FAIL_OPEN

            observability:
              final-events: true
              stage-events: false
              metrics: true
              logging: true
              endpoint: true
```

Configuration binding is strict:

- unknown fields fail startup;
- invalid enum values fail startup;
- invalid durations or numeric limits fail startup;
- invalid annotation/rule combinations fail startup;
- invalid fallback or return JSON definitions fail startup.

This behavior is intentional. Access governance should fail during deployment, not during the first production request.

---

## 8. Global Configuration Reference

### 8.1 Top-level properties

| Property             | Default | Description                                                    |
|----------------------|--------:|----------------------------------------------------------------|
| `enabled`            |  `true` | Enables the starter.                                           |
| `engine`             |   `AOP` | Selects `AOP`, `AGENT`, or `DISABLED`.                         |
| `storage`            | `LOCAL` | Selects local or Redisson-backed policy state.                 |
| `defaults.rejection` | `THROW` | Default rejection behavior for rules without an explicit mode. |

### 8.2 Key properties

| Property              |      Default | Description                                                 |
|-----------------------|-------------:|-------------------------------------------------------------|
| `key.contributors`    | `[ARGUMENT]` | Global ordered contributor list.                            |
| `key.trusted-proxies` |         `[]` | Direct peers allowed to supply forwarded client IP headers. |
| `key.headers`         |         `[]` | Header names read by `HTTP_HEADER`.                         |
| `key.hmac-secret`     |        empty | Required when rules are configured.                         |
| `key.max-part-length` |       `1024` | Maximum normalized key-part name/value length.              |

Rule-level contributors behave as follows:

- an empty list inherits the global contributor list;
- a non-empty list replaces the global contributor list.

### 8.3 Local state properties

| Property                 |  Default | Description                       |
|--------------------------|---------:|-----------------------------------|
| `local.max-entries`      | `100000` | Maximum bounded local state size. |
| `local.cleanup-interval` |     `1m` | Cleanup cadence.                  |
| `local.idle-ttl`         |    `10m` | Idle eviction TTL.                |

### 8.4 Thread-pool properties

| Property                     |        Default | Description                 |
|------------------------------|---------------:|-----------------------------|
| `thread-pool.name`           | `access-guard` | Managed executor name.      |
| `thread-pool.core-pool-size` |            `4` | Core thread count.          |
| `thread-pool.max-pool-size`  |           `16` | Maximum thread count.       |
| `thread-pool.queue-capacity` |         `1024` | Bounded work queue size.    |
| `thread-pool.keep-alive`     |          `60s` | Non-core thread keep-alive. |

The pool is validated:

- sizes must be positive;
- core size cannot exceed max size;
- queue capacity must be bounded and positive.

---

## 9. Guard Key Design

The guard key defines the identity against which allow-list, deny-list, penalty, and rate-limit state is evaluated.

A correct key design is more important than the rate-limit number itself. A perfect limit applied to the wrong identity
is still wrong.

### 9.1 Key lifecycle

```text
contributors
    |
    v
ordered GuardKeyPart values
    |
    v
normalize and escape
    |
    v
name=value|name=value
    |
    v
HMAC-SHA-256(secret)
    |
    v
keyHash used by stores and policies
```

Raw key material is not used directly as:

- Redis keys;
- metric tags;
- Actuator output;
- structured event fields.

### 9.2 `ARGUMENT`

Use `@GuardKey` on:

- method parameters;
- fields of an argument object;
- record components.

Parameter example:

```java
@AccessGuard("draw")
public DrawResult draw(
        @GuardKey(value = "tenant", order = 0) String tenantId,
        @GuardKey(value = "user", order = 10) String userId
) {
    return drawService.draw(tenantId, userId);
}
```

Record example:

```java
public record DrawCommand(
        @GuardKey(value = "tenant", order = 0) String tenantId,
        @GuardKey(value = "user", order = 10) String userId,
        String activityId
) {
}

@AccessGuard("draw")
public DrawResult draw(DrawCommand command) {
    return drawService.draw(command);
}
```

Class-field example:

```java
public class PaymentCommand {

    @GuardKey(value = "merchant", order = 0)
    private String merchantId;

    @GuardKey(value = "customer", order = 10)
    private String customerId;

    private BigDecimal amount;
}
```

`required` defaults to `true`:

```java
@GuardKey(value = "device", required = false)
String deviceId
```

A missing required value produces key-resolution failure.

### 9.3 `GLOBAL`

```yaml
key:
  contributors:
    - GLOBAL
```

All calls share one guard identity.

Typical use cases:

- protect one expensive batch operation;
- cap global traffic to a downstream dependency;
- limit application-wide access to a maintenance task.

Do not use `GLOBAL` when fairness between users or tenants matters.

### 9.4 `CLIENT_IP`

```yaml
key:
  contributors:
    - CLIENT_IP
  trusted-proxies:
    - 10.0.0.0/8
    - 192.168.0.0/16
```

The resolver:

1. reads the direct remote address;
2. checks whether that peer matches `trusted-proxies`;
3. only then trusts `Forwarded` or `X-Forwarded-For`;
4. otherwise uses the direct peer address.

Never trust forwarded headers from every source. Without a trusted-proxy boundary, clients can forge the protected
identity.

The invocation context must contain an HTTP request under:

```text
accessGuard.httpRequest
```

Applications using a custom adapter must provide an object exposing compatible `getRemoteAddr()` and `getHeader(String)`
methods.

### 9.5 `HTTP_HEADER`

```yaml
key:
  contributors:
    - HTTP_HEADER
  headers:
    - X-Tenant-Id
    - X-Client-Id
```

Each configured header is required.

Only use authenticated, sanitized, and trusted headers. A client-controlled header is not an identity boundary by
itself.

### 9.6 `PRINCIPAL`

```yaml
key:
  contributors:
    - PRINCIPAL
```

The invocation context must contain a principal under:

```text
accessGuard.principal
```

When the value implements `java.security.Principal`, `getName()` is used. Otherwise, its string value is used.

### 9.7 `ATTRIBUTE:<name>`

This contributor is especially useful for the programmatic API.

```yaml
key:
  contributors:
    - ATTRIBUTE:tenantId
    - ATTRIBUTE:userId
```

```java
GuardRequest request = new GuardRequest(
        "draw",
        new Object[0],
        Map.of(
            "tenantId", tenantId,
            "userId", userId
        ),
        DrawResult.class,
        null
);
```

Missing attributes fail key resolution.

### 9.8 Composite keys

```yaml
key:
  contributors:
    - ARGUMENT
    - CLIENT_IP
```

The key parts are sorted by their explicit order and then normalized. Delimiters are escaped before hashing.

Use composite keys when one dimension alone is too broad, for example:

- tenant + user;
- merchant + customer;
- account + device;
- user + IP for credential-abuse protection.

Avoid high-cardinality dimensions that do not improve enforcement.

---

## 10. Fixed Policy Order

The admission order cannot be changed:

1. DenyList
2. AllowList
3. PenaltyBox
4. RateLimit
5. TimeLimit and business execution
6. Rejection resolution
7. Final observability event

### Why DenyList is first

DenyList always wins. An allow-list hit cannot bypass a deny-list match.

This avoids accidental privilege escalation such as:

```text
blocked user + old allow-list entry -> allowed
```

### Why PenaltyBox is before RateLimit

An active penalty should reject immediately without consuming or recalculating token-bucket state.

### Why TimeLimit is after admission

Rejected calls should not consume executor capacity or start business work.

---

## 11. Deny List

```yaml
deny-list:
  enabled: true
  data-version: v1
```

A matching key produces:

```text
GuardDecision.DENY_LIST_HIT
```

`data-version` is part of the storage namespace. Change it when replacing a complete logical dataset without deleting
old keys immediately.

### Store API

```java
public interface DenyListStore {

    boolean contains(String ruleId, String dataVersion, String keyHash);

    void add(String ruleId, String dataVersion, String keyHash, Duration ttl);

    void remove(String ruleId, String dataVersion, String keyHash);

    void replace(
        String ruleId,
        String dataVersion,
        Set<String> keyHashes,
        Duration ttl
    );
}
```

The local and Redisson implementations support writes. A custom read-only implementation may keep the default write
methods, which throw `StoreOperationException`.

### Important operational note

Stores operate on the hashed key, not the raw business identity. Administration tools must use the same normalization
and HMAC process, or expose a trusted service that converts an identity to the correct hash.

---

## 12. Allow List

```yaml
allow-list:
  enabled: true
  mode: GATE
  data-version: v1
```

A miss in `GATE` mode produces:

```text
GuardDecision.ALLOW_LIST_MISS
```

### Modes

| Mode                            | Behavior                                                      |
|---------------------------------|---------------------------------------------------------------|
| `GATE`                          | Miss rejects. Hit continues through PenaltyBox and RateLimit. |
| `BYPASS_RATE_LIMIT`             | Hit skips RateLimit but never DenyList or PenaltyBox.         |
| `BYPASS_RATE_LIMIT_AND_PENALTY` | Hit skips PenaltyBox and RateLimit but never DenyList.        |

Example:

```yaml
rules:
  partner-api:
    deny-list:
      enabled: true

    allow-list:
      enabled: true
      mode: BYPASS_RATE_LIMIT

    penalty-box:
      enabled: true

    rate-limit:
      enabled: true
```

The behavior is:

```text
deny-list hit        -> reject
allow-list miss      -> continue to penalty and rate limit
allow-list hit       -> skip only rate limit
active penalty       -> reject
```

The bypass modes are deliberately narrow. There is no bypass-all mode.

---

## 13. Penalty Box

PenaltyBox converts repeated rate-limit violations into a temporary penalty.

```yaml
penalty-box:
  enabled: true
  threshold: 5
  violation-ttl: 1m
  penalty-ttl: 10m
```

Meaning:

- count rate-limit violations within `violation-ttl`;
- when count reaches `threshold`, activate a penalty;
- reject the key until `penalty-ttl` expires.

An active penalty produces:

```text
GuardDecision.PENALTY_ACTIVE
```

### Example timeline

```text
00:00  rate limited, violations=1
00:10  rate limited, violations=2
00:20  rate limited, violations=3
00:30  rate limited, violations=4
00:40  rate limited, violations=5 -> penalty activated
00:45  rejected by PenaltyBox
10:40  penalty expires
```

If the configured violation TTL expires before the threshold is reached, the counter window resets according to the
selected store implementation.

---

## 14. Rate Limit

The rate-limit strategy is selected by the rule and defaults to
`TOKEN_BUCKET`. The starter supports three algorithms:

| Algorithm | State semantics | Parameter interpretation |
|-----------|-----------------|--------------------------|
| `TOKEN_BUCKET` | Tokens accumulate up to `capacity`; an allowed call consumes `requested-tokens`. | `refill-tokens` are added every `refill-period`. |
| `LEAKY_BUCKET` | Water level drains at a fixed rate; a call is admitted only when the new level fits. | `refill-tokens/refill-period` define the drain rate; `requested-tokens` is the water cost. |
| `SLIDING_WINDOW` | Accepted timestamps are retained for one window and counted exactly. | `capacity` is the maximum calls; `refill-period` is the window; `requested-tokens` must be `1`. |

Configuration:

```yaml
rate-limit:
  enabled: true
  algorithm: TOKEN_BUCKET
  capacity: 100
  refill-tokens: 100
  refill-period: 1s
  requested-tokens: 1
```

### Parameter meaning

| Property           | Meaning                              |
|--------------------|--------------------------------------|
| `capacity`         | Maximum tokens in the bucket.        |
| `refill-tokens`    | Tokens added every refill period.    |
| `refill-period`    | Refill interval.                     |
| `requested-tokens` | Tokens consumed by one guarded call. |

Validation rules include:

- all numeric values must be positive;
- `requested-tokens` cannot exceed `capacity`;
- `refill-period` must be positive.
- `SLIDING_WINDOW` requires `requested-tokens=1` and `capacity<=100000`.

Local storage uses monotonic time and a bounded in-memory entry map. Redisson
storage uses Redis server time and atomic single-key scripts. Existing Token
Bucket deployments retain the legacy HASH key; Leaky Bucket and Sliding Window
use lazy `:leaky-bucket` and `:sliding-window` suffix keys with idle TTL cleanup.
No migration or broad deletion is required. Changing algorithm parameters starts
new normalized state according to the configured rule version. Storage errors
follow `failurePolicies.rateLimitBackend` (`FAIL_OPEN`, `LOCAL_FALLBACK`, or
`FAIL_CLOSED`). `retryAfter` is an operational hint in `GuardOutcome`; the Guard
does not queue or sleep a rejected call.

For RPC Provider methods, add the RPC Starter and Access Guard Starter explicitly
and put `@RateLimitGuard(ruleId, key)` on the implementation method. RPC maps only
`GuardDecision.RATE_LIMITED` to Provider-stage gRPC `UNAVAILABLE`; the target method
is not entered. Other Guard decisions retain their normal exception semantics.

### Examples

#### 10 requests per second with burst 10

```yaml
capacity: 10
refill-tokens: 10
refill-period: 1s
requested-tokens: 1
```

#### 60 requests per minute with burst 20

```yaml
capacity: 20
refill-tokens: 60
refill-period: 1m
requested-tokens: 1
```

#### Expensive operation consuming five units

```yaml
capacity: 100
refill-tokens: 100
refill-period: 1m
requested-tokens: 5
```

A rejected request produces:

```text
GuardDecision.RATE_LIMITED
```

The outcome may include `retryAfter`.

---

## 15. Time Limits

Time-limit configuration:

```yaml
time-limit:
  enabled: true
  mode: ENFORCE
  executor: VIRTUAL_THREAD
  timeout: 800ms
  cancel-running-task: true
```

### Supported combinations

| Mode           | Executor         | Behavior                                               |
|----------------|------------------|--------------------------------------------------------|
| `DISABLED`     | `CALLER_THREAD`  | No timeout behavior.                                   |
| `OBSERVE_ONLY` | `CALLER_THREAD`  | Execute on the caller thread and measure duration.     |
| `ENFORCE`      | `THREAD_POOL`    | Execute on a bounded managed pool and enforce timeout. |
| `ENFORCE`      | `VIRTUAL_THREAD` | Execute on a virtual thread and enforce timeout.       |

Invalid combinations fail startup.

### Caller-thread observation

```yaml
time-limit:
  enabled: true
  mode: OBSERVE_ONLY
  executor: CALLER_THREAD
  timeout: 1s
```

The operation stays on the original thread. The configured timeout is an observation threshold, not a forced
interruption boundary.

### Bounded thread pool

```yaml
time-limit:
  enabled: true
  mode: ENFORCE
  executor: THREAD_POOL
  timeout: 500ms
```

Use this when concurrency must be bounded independently from the caller.

A full pool or queue produces:

```text
GuardDecision.EXECUTOR_REJECTED
```

### Virtual thread

```yaml
time-limit:
  enabled: true
  mode: ENFORCE
  executor: VIRTUAL_THREAD
  timeout: 500ms
```

Virtual threads reduce the cost of blocked platform threads, but they do not make uninterruptible I/O cancellable.

### Timeout cancellation is cooperative

`cancel-running-task: true` attempts cancellation, but the underlying operation may continue when it:

- ignores interruption;
- is blocked in uninterruptible native I/O;
- delegates work to another system;
- has already committed a side effect.

A timeout protects caller latency. It is not a transaction rollback mechanism.

Use idempotency, deadlines, and downstream cancellation in addition to the guard.

---

## 16. Rejection Handling

Supported modes:

```text
THROW
FALLBACK
RETURN_JSON
RETURN_NULL
```

### 16.1 `THROW`

```yaml
rejection:
  mode: THROW
```

Throws `AccessGuardRejectedException`.

Use this when a global exception handler owns the API response.

### 16.2 `FALLBACK`

```yaml
rejection:
  mode: FALLBACK
  fallback-method: drawFallback
```

Original method:

```java
@AccessGuard("draw")
public DrawResult draw(String userId) {
    return drawService.draw(userId);
}
```

Supported fallback signatures:

```java
private DrawResult drawFallback(String userId) {
    return DrawResult.busy(userId);
}
```

```java
private DrawResult drawFallback(
        String userId,
        GuardOutcome outcome
) {
    return DrawResult.rejected(userId, outcome.decision().name());
}
```

```java
private DrawResult drawFallback() {
    return DrawResult.busy();
}
```

Rules:

- exactly one compatible fallback must exist;
- return type must be compatible;
- a static guarded method requires a static fallback;
- constructors do not support fallback;
- invalid or ambiguous fallback methods fail startup.

### 16.3 `RETURN_JSON`

```yaml
rejection:
  mode: RETURN_JSON
  return-json: >
    {"success":false,"code":"TOO_MANY_REQUESTS"}
```

The JSON is deserialized into the original method return type using a Spring-managed `ObjectMapper`.

```java
@AccessGuard("draw")
public DrawResult draw(String userId) {
    return drawService.draw(userId);
}
```

The JSON must be valid for `DrawResult`. Validation occurs at startup for discovered guarded methods.

### 16.4 `RETURN_NULL`

```yaml
rejection:
  mode: RETURN_NULL
```

Restrictions:

- not valid for primitive return types;
- not valid for constructors.

Prefer explicit result types over `RETURN_NULL` for public APIs.

---

## 17. Failure Policies

A failure policy answers a different question from a rejection mode.

- **Policy rejection**: the rule worked and decided not to admit the call.
- **Infrastructure failure**: the rule could not be evaluated normally.
- **Rejection mode**: how the terminal outcome is returned to the caller.

Supported failure policies:

```text
FAIL_CLOSED
FAIL_OPEN
LOCAL_FALLBACK
```

### Default matrix

| Failure point      | Default          |
|--------------------|------------------|
| Key resolution     | `FAIL_CLOSED`    |
| DenyList store     | `FAIL_CLOSED`    |
| AllowList store    | `FAIL_CLOSED`    |
| Penalty store      | `LOCAL_FALLBACK` |
| Rate-limit backend | `LOCAL_FALLBACK` |
| Execution          | `FAIL_CLOSED`    |
| Observability      | `FAIL_OPEN`      |

### `FAIL_CLOSED`

Reject or fail when governance cannot be evaluated safely.

Recommended for:

- key resolution;
- deny-list checks;
- high-risk allow-list gates;
- security-sensitive operations.

### `FAIL_OPEN`

Continue business execution and mark the outcome as degraded.

Example outcome:

```text
type       = DEGRADED
decision   = STORE_FAILED
resolution = FAIL_OPEN
```

Use only when availability is more important than enforcement.

### `LOCAL_FALLBACK`

Retry the policy using the bounded local implementation.

This is available for the built-in local fallback policies, especially:

- PenaltyBox
- RateLimit

It does not mean unconditional admission. The local policy still produces a pass or reject decision.

### Configure per rule

```yaml
failure-policies:
  key-resolution: FAIL_CLOSED
  deny-list-store: FAIL_CLOSED
  allow-list-store: FAIL_CLOSED
  penalty-store: LOCAL_FALLBACK
  rate-limit-backend: LOCAL_FALLBACK
  execution: FAIL_CLOSED
  observability: FAIL_OPEN
```

---

## 18. Outcome Model

`GuardOutcome` contains:

| Field         | Description                                           |
|---------------|-------------------------------------------------------|
| `type`        | `ALLOWED`, `REJECTED`, `DEGRADED`, or `FAILED`.       |
| `decision`    | Root decision or failure reason.                      |
| `resolution`  | How the outcome was resolved.                         |
| `ruleId`      | Rule ID.                                              |
| `policy`      | Policy that produced the terminal or degraded result. |
| `planVersion` | Active plan version.                                  |
| `storage`     | Active storage mode.                                  |
| `engine`      | Active execution engine.                              |
| `elapsed`     | Guarded lifecycle duration.                           |
| `retryAfter`  | Suggested retry delay when available.                 |
| `failure`     | Bounded failure category and code.                    |

### Decision values

```text
PASS
DENY_LIST_HIT
ALLOW_LIST_MISS
PENALTY_ACTIVE
RATE_LIMITED
KEY_RESOLUTION_FAILED
STORE_FAILED
CONFIG_FAILED
TIME_LIMIT_EXCEEDED
EXECUTOR_REJECTED
BUSINESS_EXCEPTION
CANCELLED
```

### Resolution values

```text
NONE
THROWN
FALLBACK
RETURN_JSON
RETURN_NULL
FAIL_OPEN
LOCAL_FALLBACK
```

Root cause and handling are intentionally separate.

For example:

```text
decision   = STORE_FAILED
resolution = LOCAL_FALLBACK
```

means the distributed store failed, but a local policy resolved the request.

---

## 19. Storage Modes

### 19.1 Local storage

```yaml
storage: LOCAL
```

Characteristics:

- no Redis dependency;
- bounded in-process state;
- suitable for development and single-instance services;
- each application instance has independent state;
- cannot enforce a cluster-wide quota.

Local implementations exist for:

- AllowList
- DenyList
- PenaltyBox
- RateLimit

### 19.2 Redisson storage

```yaml
storage: REDISSON

redisson:
  client-bean-name: redissonClient
  key-prefix: egon:access-guard
  application: ${spring.application.name}
```

Provide Redisson:

```xml
<dependency>
    <groupId>org.redisson</groupId>
    <artifactId>redisson</artifactId>
</dependency>
```

Example client:

```java
@Configuration
public class RedisConfiguration {

    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();
        config.useSingleServer()
              .setAddress("redis://127.0.0.1:6379");

        return Redisson.create(config);
    }
}
```

Selection rules:

- if `client-bean-name` is set, that bean must exist and be a `RedissonClient`;
- if it is blank, exactly one `RedissonClient` must exist;
- an application namespace must come from `redisson.application` or `spring.application.name`;
- invalid Redisson integration fails startup.

Redisson stores use atomic transitions for penalty and token-bucket state.

### 19.3 Key namespace

Distributed keys are separated by dimensions such as:

```text
key-prefix
application
rule
policy
data or state version
key hash
```

Use unique application names across independently governed services.

---

## 20. Programmatic API

Inject:

```java
private final AccessGuardClient accessGuardClient;
```

### 20.1 Admission-only evaluation

```java
GuardRequest request = new GuardRequest(
        "draw",
        new Object[]{userId},
        Map.of(),
        DrawResult.class,
        null
);

GuardOutcome outcome = accessGuardClient.evaluate(request);

if (outcome.type() != GuardOutcomeType.ALLOWED
        && outcome.type() != GuardOutcomeType.DEGRADED) {
    throw new IllegalStateException("Request was not admitted");
}
```

`evaluate` performs admission evaluation only. It does not execute the business operation and therefore does not
represent the complete time-limit/rejection lifecycle.

### 20.2 Guarded execution

```java
DrawResult result = accessGuardClient.execute(
        request,
        () -> drawService.draw(userId)
);
```

Use `execute` when you need:

- time limits;
- business exception handling;
- fallback;
- final lifecycle events.

### 20.3 Programmatic fallback

```java
GuardRequest request = new GuardRequest(
        "draw",
        new Object[]{userId},
        Map.of(),
        DrawResult.class,
        () -> DrawResult.busy(userId)
);
```

### 20.4 Attribute contributors

```yaml
rules:
  draw:
    key:
      contributors:
        - ATTRIBUTE:tenantId
        - ATTRIBUTE:userId
```

```java
GuardRequest request = new GuardRequest(
        "draw",
        new Object[0],
        Map.of(
            "tenantId", tenantId,
            "userId", userId
        ),
        DrawResult.class,
        null
);
```

The programmatic API is useful when:

- no Spring Bean proxy is involved;
- a workflow engine invokes operations dynamically;
- an adapter already owns execution;
- admission must be checked before building expensive request state.

---

## 21. `CompletionStage` Support

A guarded method returning `CompletionStage` is not considered complete when the stage object is returned.

```java
@AccessGuard("async-report")
public CompletionStage<Report> generate(String reportId) {
    return reportService.generate(reportId);
}
```

The starter tracks:

- admission;
- asynchronous success;
- asynchronous failure;
- timeout;
- cancellation;
- rejection resolution;
- one final outcome.

For enforced time limits, the returned stage receives a timeout using the configured duration.

Fallback values may be:

- a direct value;
- a compatible `CompletionStage`.

Cancellation is reported as:

```text
GuardDecision.CANCELLED
```

Do not wrap the stage manually only to make the AOP advice see a synchronous result. The starter already owns the async
lifecycle.

---

## 22. Reactor Support

When Reactor is present, `Mono` and `Flux` are guarded lazily.

```java
@AccessGuard("reactive-order")
public Mono<Order> findOrder(String orderId) {
    return orderRepository.findById(orderId);
}
```

Governance starts at subscription time:

```text
method call returns publisher
        |
        v
subscriber subscribes
        |
        v
resolve plan and admission
        |
        v
subscribe to business publisher
        |
        v
success / error / timeout / cancel
        |
        v
one terminal guard outcome
```

This preserves Reactor cold-publisher semantics.

### Supported return types

- `Mono`
- `Flux`

A guarded reactive method fails startup unless exactly one reactive adapter is available.

### Fallback compatibility

For a `Mono` method:

- fallback may return a value or `Mono`;
- returning `Flux` is invalid.

For a `Flux` method:

- fallback may return a value or `Flux`;
- returning `Mono` is invalid.

### Timeout

```yaml
time-limit:
  enabled: true
  mode: ENFORCE
  executor: VIRTUAL_THREAD
  timeout: 1s
```

For Reactor methods, timeout is applied to the publisher lifecycle. The Reactor adapter does not treat the publisher
object itself as completed work.

---

## 23. Execution Engines

### 23.1 AOP

```yaml
engine: AOP
```

Default mode.

Supports:

- Spring Bean methods;
- type-level bindings;
- method-level bindings;
- synchronous methods;
- `CompletionStage`;
- Reactor methods when Reactor is available.

Limitations:

- cannot intercept constructors;
- cannot intercept self-invocation that bypasses the Spring proxy;
- cannot govern objects not created or called through the Spring proxy.

Example self-invocation pitfall:

```java
@Service
public class OrderService {

    public void outer() {
        inner(); // bypasses Spring proxy
    }

    @AccessGuard("inner")
    public void inner() {
    }
}
```

Move the guarded method to another Bean, call through the proxy, use the programmatic client, or select Agent mode.

### 23.2 Disabled

```yaml
engine: DISABLED
```

Base infrastructure remains available, but AOP and Agent execution are not activated.

This can be useful for:

- controlled rollout;
- local diagnosis;
- applications using only selected programmatic infrastructure.

### 23.3 Agent

```yaml
engine: AGENT
```

Add:

```xml
<dependency>
    <groupId>top.egon</groupId>
    <artifactId>egon-cola-component-bytecode-starter</artifactId>
    <version>5.3.2</version>
</dependency>
```

Launch with the Egon Bytecode Agent:

```bash
java \
  "-javaagent:/opt/egon/egon-cola-component-bytecode-agent-5.3.2.jar=enabled=true,features=access-guard,include=com.example.*" \
  -jar application.jar
```

The Agent must be installed at JVM startup. Attach/retransform mode is not the normal runtime contract.

Agent mode supports additional bytecode paths such as:

- private methods;
- static methods;
- same-class calls;
- recursive calls;
- final methods;
- synchronized methods, subject to timeout restrictions;
- non-Spring objects;
- explicit constructors.

AOP and Agent engines are mutually exclusive.

---

## 24. Constructor Governance

Constructor interception requires Agent mode and an explicit constructor annotation.

```java
public class SecureClient {

    @AccessGuard("client-construction")
    public SecureClient(@GuardKey("tenant") String tenantId) {
        initialize(tenantId);
    }
}
```

Restrictions:

- only explicit constructor annotations are transformed;
- a type annotation does not automatically guard every constructor;
- governance runs before the first `this(...)` or `super(...)` call;
- no initialized receiver is available;
- only admission policies are supported;
- rejection mode must be `THROW`;
- time limit is not supported;
- fallback is not supported;
- JSON/null return replacement is not supported.

Use constructor governance carefully. Guarding infrastructure created before Spring runtime readiness can create startup
cycles or fail-closed behavior.

Prefer guarding factory or application-service methods unless constructor interception is truly required.

---

## 25. Synchronized and Static Methods in Agent Mode

### Static method

```java
@AccessGuard("static-task")
public static Result execute(String id) {
    return doExecute(id);
}
```

When fallback is used, the fallback must also be static.

### Synchronized method

The Agent preserves the original monitor boundary.

```java
@AccessGuard("critical-section")
public synchronized Result update(String id) {
    return doUpdate(id);
}
```

Time-limit execution that moves the method body to another thread is not valid for synchronized methods because that
would change monitor semantics.

---

## 26. Dynamic Rule Sources

Configuration properties are one `GuardPlanSource`.

The public source contract is:

```java
public interface GuardPlanSource {

    String name();

    int priority();

    Optional<GuardPlanSnapshot> current(String ruleId);

    AutoCloseable subscribe(Consumer<GuardPlanSnapshot> listener);
}
```

The resolver:

- requires unique source names;
- requires unique priorities;
- selects higher priority first;
- validates every candidate snapshot;
- requires monotonic versions per source and rule;
- keeps the last valid snapshot when a new candidate is invalid;
- records bounded load failures;
- publishes plan-change events.

This allows integration with a dynamic configuration center without replacing the guard engine.

A custom source should:

1. produce immutable snapshots;
2. increment versions monotonically;
3. avoid exposing secrets in `toString`;
4. publish updates only after complete parsing;
5. close subscriptions cleanly.

---

## 27. Extension Points

Most infrastructure Beans use conditional registration and can be replaced.

Important interfaces include:

| Interface               | Purpose                                                     |
|-------------------------|-------------------------------------------------------------|
| `GuardPlanSource`       | Supply static or dynamic rule snapshots.                    |
| `GuardPlanResolver`     | Resolve the active plan.                                    |
| `GuardKeyContributor`   | Add a new key dimension.                                    |
| `KeyHasher`             | Replace key hashing, while preserving the privacy contract. |
| `DenyListStore`         | Read/write deny-list state.                                 |
| `AllowListStore`        | Read/write allow-list state.                                |
| `PenaltyStore`          | Store violation and penalty state.                          |
| `RateLimitBackend`      | Implement rate-limit state transitions.                     |
| `FailurePolicyResolver` | Resolve infrastructure failure behavior.                    |
| `TimeLimiter`           | Execute time-limited operations.                            |
| `RejectionHandler`      | Resolve terminal rejection.                                 |
| `FallbackHandler`       | Invoke fallback behavior.                                   |
| `GuardEventListener`    | Consume final/stage events.                                 |
| `GuardEventPublisher`   | Publish observability events.                               |

### Custom key contributor

```java
@Component
public class RegionKeyContributor implements GuardKeyContributor {

    @Override
    public String id() {
        return "REGION";
    }

    @Override
    public List<GuardKeyPart> contribute(
            GuardInvocation invocation,
            KeyConfig config
    ) {
        Object value = invocation.attributes().get("region");

        if (value == null) {
            throw new GuardKeyResolutionException("REGION_MISSING");
        }

        return List.of(new GuardKeyPart("region", value.toString(), 0));
    }
}
```

Configuration:

```yaml
key:
  contributors:
    - REGION
```

Contributor IDs are case-insensitive and must be unique.

---

## 28. Observability

### 28.1 Final and stage events

```yaml
observability:
  final-events: true
  stage-events: false
  metrics: true
  logging: true
  endpoint: true
```

One final event is published by default. Stage events are opt-in because they increase event volume.

### 28.2 Metrics

When a `MeterRegistry` is available, the starter emits metrics including:

```text
egon.access.guard.calls
egon.access.guard.duration
egon.access.guard.store.failures
egon.access.guard.plan.reloads
egon.access.guard.local.entries
```

Allowed bounded tags are:

```text
ruleId
policy
type
decision
resolution
engine
storage
```

The following are intentionally excluded:

- raw key;
- key hash as a tag;
- method arguments;
- headers;
- principal;
- exception message;
- fallback value;
- HMAC secret.

### 28.3 Logging

The default listener emits structured fields such as:

```text
ruleId
planVersion
policy
type
decision
resolution
engine
storage
elapsed
retryAfter
failureCategory
failureCode
```

Normal outcomes are logged at debug level. Outcomes with bounded failure metadata are logged at warn level.

### 28.4 Actuator endpoint

Add Actuator:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

Expose the endpoint:

```yaml
management:
  endpoints:
    web:
      exposure:
        include:
          - health
          - accessguard
```

Request:

```http
GET /actuator/accessguard
```

The endpoint reports:

- storage mode;
- plan health;
- plan failure count;
- bounded local PenaltyBox entry count;
- bounded local RateLimit entry count;
- non-sensitive rule summaries.

It does not expose raw identities, keys, secrets, headers, or arguments.

---

## 29. Security and Privacy Guidance

### HMAC secret

Requirements:

- must not be blank when rules exist;
- should be long and randomly generated;
- should come from a secret manager or environment variable;
- should not be committed to Git;
- should be rotated with an explicit migration plan.

Changing the secret changes every derived key hash. Existing allow-list, deny-list, penalty, and rate-limit entries will
no longer match.

Plan secret rotation as a data migration, not a simple configuration edit.

### Trusted proxy configuration

Only add actual reverse proxies and load balancers.

Bad:

```yaml
trusted-proxies:
  - 0.0.0.0/0
```

Better:

```yaml
trusted-proxies:
  - 10.10.0.0/16
  - 192.168.50.10
```

### Metric cardinality

Do not add user IDs, IPs, request IDs, or key hashes as custom metric tags.

### Failure policy selection

Security-sensitive examples should normally remain fail-closed:

- deny-list;
- authenticated principal resolution;
- payment submission;
- account recovery;
- credential validation.

Availability-sensitive read-only operations may choose fail-open for selected infrastructure failures, but the degraded
outcome must be monitored.

---

## 30. Recommended Rule Patterns

### 30.1 Per-user API limit

```yaml
rules:
  user-query:
    key:
      contributors:
        - ARGUMENT

    rate-limit:
      enabled: true
      capacity: 20
      refill-tokens: 20
      refill-period: 1s
      requested-tokens: 1
```

```java
@AccessGuard("user-query")
public Result query(@GuardKey("user") String userId) {
    return service.query(userId);
}
```

### 30.2 Global downstream protection

```yaml
rules:
  downstream-call:
    key:
      contributors:
        - GLOBAL

    rate-limit:
      enabled: true
      capacity: 100
      refill-tokens: 100
      refill-period: 1s

    time-limit:
      enabled: true
      mode: ENFORCE
      executor: VIRTUAL_THREAD
      timeout: 800ms
```

### 30.3 Partner allow-list gate

```yaml
rules:
  partner-api:
    key:
      contributors:
        - HTTP_HEADER

    allow-list:
      enabled: true
      mode: GATE
      data-version: v2

key:
  headers:
    - X-Partner-Id
```

Use this only when `X-Partner-Id` is supplied by a trusted authentication layer.

### 30.4 Login abuse protection

```yaml
rules:
  login:
    key:
      contributors:
        - ARGUMENT
        - CLIENT_IP

    deny-list:
      enabled: true

    penalty-box:
      enabled: true
      threshold: 5
      violation-ttl: 2m
      penalty-ttl: 15m

    rate-limit:
      enabled: true
      capacity: 5
      refill-tokens: 5
      refill-period: 1m

    rejection:
      mode: THROW
```

### 30.5 Graceful query fallback

```yaml
rules:
  recommendation:
    key:
      contributors:
        - ARGUMENT

    rate-limit:
      enabled: true
      capacity: 10
      refill-tokens: 10
      refill-period: 1s

    time-limit:
      enabled: true
      mode: ENFORCE
      executor: VIRTUAL_THREAD
      timeout: 300ms

    rejection:
      mode: FALLBACK
      fallback-method: recommendationFallback
```

---

## 31. Common Misconfigurations

### Missing HMAC secret

Symptom:

```text
Access Guard key HMAC secret must not be blank when rules are configured
```

Fix:

```yaml
key:
  hmac-secret: ${ACCESS_GUARD_HMAC_SECRET}
```

### Unknown rule

Symptom:

```text
Unknown Access Guard rule: draw
```

Fix the mismatch between annotation and configuration.

### Dedicated annotation with multiple policies

Symptom:

```text
A dedicated guard annotation must bind a single matching policy
```

Use `@AccessGuard` or simplify the rule.

### AOP constructor annotation

Symptom:

```text
AOP mode does not support guarded constructor
```

Use Agent mode or guard a factory method.

### Missing Redisson client

Symptom:

```text
Configured RedissonClient bean 'redissonClient' was not found
```

Provide the Bean or change `client-bean-name`.

### Blank Redisson application name

Symptom:

```text
REDISSON storage requires spring.application.name
or access-guard.redisson.application
```

Set one of the two values.

### Reactive adapter missing

Symptom:

```text
Reactive Access Guard method requires exactly one Reactor adapter
```

Ensure Reactor is available and do not register duplicate adapters.

### Invalid fallback

Possible causes:

- method not found;
- incompatible parameter list;
- ambiguous overloads;
- incompatible return type;
- instance fallback for a static original method.

### `RETURN_NULL` on primitive

Symptom:

```text
primitive return types do not support RETURN_NULL
```

Use `THROW`, `FALLBACK`, or `RETURN_JSON`.

### Timeout does not stop downstream work

This is expected when the downstream operation does not cooperate with interruption. Add downstream deadlines and
idempotency.

---

## 32. Troubleshooting Checklist

When a rule appears not to execute:

1. Confirm `enabled: true`.
2. Confirm the engine is not `DISABLED`.
3. Confirm the annotation rule ID exists.
4. In AOP mode, confirm the target is a Spring Bean.
5. Check for self-invocation.
6. Confirm the method has only one guard binding.
7. Confirm the HMAC secret is present.
8. Confirm required key parts are non-null.
9. For HTTP contributors, confirm request/principal context is supplied.
10. Check `/actuator/accessguard`.
11. Enable debug logging for the Access Guard package.
12. Inspect `GuardOutcome` decision and resolution separately.

When local and production behavior differ:

1. Check `storage` mode.
2. Check `spring.application.name`.
3. Check Redisson Bean selection.
4. Check trusted proxy CIDRs.
5. Check HMAC secret consistency.
6. Check rule `data-version`.
7. Check whether multiple service instances are using local state.
8. Check clock and Redis latency.
9. Check whether an Agent is actually installed at JVM startup.

---

## 33. Testing

### 33.1 Module tests

From the repository root:

```bash
./mvnw -B -ntp \
  -pl egon-cola-components/egon-cola-component-access-guard-starter \
  -am test
```

### 33.2 Service-level test

```java
@SpringBootTest(properties = {
    "egon.cola.component.access-guard.key.hmac-secret=test-secret",
    "egon.cola.component.access-guard.rules.draw.rate-limit.enabled=true",
    "egon.cola.component.access-guard.rules.draw.rate-limit.capacity=1",
    "egon.cola.component.access-guard.rules.draw.rate-limit.refill-tokens=1",
    "egon.cola.component.access-guard.rules.draw.rate-limit.refill-period=1h"
})
class DrawGuardTest {

    @Autowired
    private DrawApplicationService service;

    @Test
    void shouldRejectSecondCall() {
        service.draw("user-1");

        assertThatThrownBy(() -> service.draw("user-1"))
            .isInstanceOf(AccessGuardRejectedException.class);
    }
}
```

### 33.3 Redisson integration

Validate:

- two application instances share one quota;
- atomic transitions behave under concurrency;
- Redis failures follow the configured failure policy;
- application namespaces do not collide;
- key TTL behavior matches production expectations.

### 33.4 Agent verification

Run a forked JVM with:

```bash
-Xverify:all
-javaagent:/path/to/egon-cola-component-bytecode-agent-5.3.2.jar=enabled=true,features=access-guard,include=com.example.*
```

Test:

- private method;
- same-class invocation;
- static method and static fallback;
- synchronized method restrictions;
- explicit constructor;
- runtime-not-ready behavior;
- duplicate execution does not occur through proxies.

---

## 34. Production Readiness Checklist

Before enabling a rule in production:

- [ ] The protected identity is clearly defined.
- [ ] HMAC secret is stored securely.
- [ ] Secret rotation impact is understood.
- [ ] Trusted proxy ranges are exact.
- [ ] Local versus distributed enforcement is intentional.
- [ ] Token-bucket numbers match real traffic.
- [ ] Penalty TTL is proportionate.
- [ ] Failure policies are reviewed by risk and availability owners.
- [ ] Fallback is side-effect free.
- [ ] Timeout does not hide duplicate side effects.
- [ ] Metrics and alerts exist for rejection and store failure.
- [ ] Actuator exposure is protected.
- [ ] Load tests include multiple instances.
- [ ] Redis degradation has been tested.
- [ ] Agent mode has been verified in the real launch command.

---

## 35. Migration from Access Guard V1

Version `5.3.2` is a source-breaking V2 model. It does not package a V1 compatibility facade.

| V1 concept                      | V2 replacement                                        |
|---------------------------------|-------------------------------------------------------|
| Legacy general guard annotation | `top.egon.cola.component.accessguard.api.AccessGuard` |
| Legacy `DoWhiteList`            | `AllowListGuard` or `AccessGuard`                     |
| Legacy `DoRateLimiter`          | `RateLimitGuard` or `AccessGuard`                     |
| Legacy `DoHystrix`              | `TimeLimitGuard` or `AccessGuard`                     |
| Annotation-embedded limits      | Named YAML rule                                       |
| Separate runtime paths          | Unified `GuardEngine`                                 |
| Raw/composite store key         | Ordered contributors plus HMAC-SHA-256                |
| Whitelist bypass-all            | Bounded `AllowListMode` values                        |
| Ad-hoc fallback                 | Startup-validated fallback or JSON resolution         |

Migrate annotations and configuration together. Mixed V1/V2 runtime operation is not supported.

---

## 36. Design Boundaries

The starter provides admission control and guarded execution. It is not:

- an API gateway replacement;
- a distributed transaction coordinator;
- a complete circuit breaker with rolling error-rate state;
- an authentication provider;
- an authorization policy engine;
- a WAF;
- a guarantee that timed-out work has stopped;
- a control-plane UI for allow/deny-list management.

It can be combined with those systems, but their responsibilities should remain explicit.

---

## 37. Verification Boundary

The module test suite covers the implementation contract, including:

- fixed policy order;
- failure-policy matrix;
- configuration binding and validation;
- AOP/programmatic parity;
- fallback validation;
- `CompletionStage` lifecycle;
- Reactor lifecycle;
- local bounded state;
- Redisson scripts under gated integration tests;
- test-scoped Java Agent processes.

Passing Maven tests does not by itself prove:

- real multi-JVM Redis behavior;
- production reverse-proxy topology;
- network partition behavior;
- clock skew behavior;
- cancellation of uninterruptible I/O;
- production Agent packaging and launch scripts.

Validate these properties in the target environment.

---

## 38. Minimal Examples Index

### Only rate limit

```yaml
rules:
  search:
    rate-limit:
      enabled: true
```

```java
@RateLimitGuard("search")
public Result search(@GuardKey String keyword) {
    return service.search(keyword);
}
```

### Multi-policy guard

```java
@AccessGuard("login")
public LoginResult login(LoginCommand command) {
    return loginService.login(command);
}
```

### Programmatic operation

```java
Result result = accessGuardClient.execute(
    new GuardRequest("task", args, attributes, Result.class, fallback),
    operation
);
```

### Global limiter

```yaml
key:
  contributors:
    - GLOBAL
```

### Redisson

```yaml
storage: REDISSON
redisson:
  application: order-service
```

### Agent

```yaml
engine: AGENT
```

```bash
-javaagent:egon-cola-component-bytecode-agent-5.3.2.jar=enabled=true,features=access-guard,include=com.example.*
```

---

## 39. License

See the repository root for the applicable MIT / LGPL-2.1 license files.
