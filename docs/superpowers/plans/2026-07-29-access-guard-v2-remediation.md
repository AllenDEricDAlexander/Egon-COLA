# Access Guard V2 Remediation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Rebuild the single Access Guard Starter around one AspectJ-free `GuardEngine` shared by Spring AOP, Bytecode Agent, and programmatic callers while enforcing the approved V2 policy, storage, failure, execution, privacy, and observability contracts.

**Architecture:** The Starter owns immutable API/core types, versioned plans, key contributors, a fixed admission chain, local and optional Redisson stores, execution protection, adapters, and observability. The existing Bytecode JDK-only bridge remains generic; only Bytecode Starter adapts bridge invocations into Access Guard core calls. The breaking public/configuration migration is released on the repository-wide 6.0.0 line.

**Tech Stack:** Java 21, Spring Boot 3.5.16, Spring AOP, Jackson, Redisson, Micrometer, optional Reactor, optional Actuator, JUnit Jupiter, AssertJ, Mockito, ApplicationContextRunner, Testcontainers Redis, Maven Wrapper.

## Global Constraints

- Keep exactly one Access Guard artifact: `egon-cola-component-access-guard-starter`; do not add aggregate, core, store, admin, or test modules.
- Keep every Access Guard test under the Starter's `src/test`; Bytecode-specific tests stay in the existing Bytecode modules.
- Use one `GuardEngine` for AOP, Agent, and programmatic calls; core packages must not import AspectJ or Bytecode Bridge types.
- Fix the built-in admission order as `DenyList -> AllowList -> PenaltyBox -> RateLimit`; configuration and custom Beans must not reorder it.
- Remove `BYPASS_GUARD`; AllowList may bypass only the explicitly named later policies and can never bypass DenyList.
- Separate manual DenyList state from automatic PenaltyBox counters/bans.
- Keep LOCAL, REDISSON, and custom implementations behaviorally equivalent at the policy contract.
- Treat real policy rejection as terminal; failure policies apply only to infrastructure/configuration failures.
- Separate `GuardOutcome.decision` (root cause) from `GuardOutcome.resolution` (final handling).
- Never place raw access keys in Redis keys, logs, metrics, events, exceptions, or `toString()` output.
- Make invalid and unknown configuration fail at startup; each declared property needs a production reader and a behavior assertion.
- Remove `DoWhiteList`, `DoRateLimiter`, and `DoHystrix` without compatibility forwarders.
- Replace V1 dedicated annotations with thin `AllowListGuard`, `RateLimitGuard`, and `TimeLimitGuard` rule bindings.
- Reuse the existing Bytecode generic bridge and keep the Access Guard adapter in Bytecode Starter.
- Make governed constructors fail closed before the Access Guard runtime is ready; constructors never support TimeLimiter, fallback, `returnJson`, `returnNull`, or instance state.
- Do not implement or configure ConcurrencyLimit in V2.
- Keep Reactor, Redisson, Micrometer, and Actuator optional; absence of an unselected capability must not break Starter loading.
- Do not add JPA, database, Flyway, MQ, Hystrix, or direct DDC/Nacos dependencies.
- Do not edit existing Flyway migrations or start an application/service during validation.
- Preserve unrelated worktree changes. Each task ends with one scoped commit; do not push or create a pull request.
- Treat Maven/static evidence as process-local proof only; report real Redis, multi-JVM, proxy, and cancellation evidence separately.
- During Tasks 1-12, V1 classes may coexist only as unmodified compatibility scaffolding needed to keep intermediate commits compiling. Exactly one auto-configuration path is registered at every commit; Task 13 deletes the entire V1 surface and gives temporary V2 classes their final names.

---

### Task 1: Introduce the V2 public API and immutable outcome model

**Files:**
- Create: `egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/api/AccessGuard.java`
- Create: `egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/api/GuardKey.java`
- Create: `egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/api/AllowListGuard.java`
- Create: `egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/api/RateLimitGuard.java`
- Create: `egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/api/TimeLimitGuard.java`
- Create: `egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/api/AccessGuardClient.java`
- Create: `egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/api/GuardRequest.java`
- Create: `egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/api/GuardedOperation.java`
- Create: `egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/api/AccessGuardAgentIntegration.java`
- Create: `egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/api/AccessGuardRejectedException.java`
- Create: `egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/core/GuardEntryType.java`
- Create: `egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/core/GuardInvocationKind.java`
- Create: `egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/core/GuardOutcomeType.java`
- Create: `egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/core/GuardDecision.java`
- Create: `egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/core/GuardResolution.java`
- Create: `egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/core/GuardFailure.java`
- Create: `egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/core/GuardOutcome.java`
- Create: `egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/core/GuardInvocation.java`
- Test: `egon-cola-components/egon-cola-component-access-guard-starter/src/test/java/top/egon/cola/component/accessguard/api/AccessGuardApiTest.java`
- Test: `egon-cola-components/egon-cola-component-access-guard-starter/src/test/java/top/egon/cola/component/accessguard/core/GuardOutcomeTest.java`

**Interfaces:**
- Consumes: Java reflection `Executable`, immutable argument/attribute snapshots, and `Duration`.
- Produces: V2 annotations, `AccessGuardClient.evaluate/execute`, immutable `GuardRequest`/`GuardInvocation`, and the final outcome vocabulary used by every later task.

- [ ] **Step 1: Write failing API-shape and immutability tests**

Add literal contract tests:

```java
@Test
void accessGuardBindsOnlyRuleAndOptionalKey() throws Exception {
    assertThat(AccessGuard.class.getDeclaredMethods())
            .extracting(Method::getName)
            .containsExactlyInAnyOrder("value", "key");
    assertThat(AccessGuard.class.getMethod("value").getDefaultValue()).isNull();
    assertThat(AccessGuard.class.getMethod("key").getDefaultValue()).isEqualTo("");
}

@Test
void rejectedFallbackPreservesRootDecision() {
    GuardOutcome outcome = GuardOutcome.of(
            GuardOutcomeType.DEGRADED,
            GuardDecision.RATE_LIMITED,
            GuardResolution.FALLBACK,
            "draw", "rate-limit", 7L, Duration.ofMillis(3));
    assertThat(outcome.decision()).isEqualTo(GuardDecision.RATE_LIMITED);
    assertThat(outcome.resolution()).isEqualTo(GuardResolution.FALLBACK);
}

@Test
void requestDefensivelyCopiesArgumentsAndAttributes() {
    Object[] arguments = {"user-1"};
    Map<String, Object> attributes = new HashMap<>(Map.of("tenant", "t1"));
    GuardRequest request = new GuardRequest("draw", arguments, attributes, String.class, null);
    arguments[0] = "changed";
    attributes.put("tenant", "changed");
    assertThat(request.arguments()).containsExactly("user-1");
    assertThat(request.attributes()).containsEntry("tenant", "t1");
}
```

Also assert annotation targets: `AccessGuard` supports TYPE/METHOD/CONSTRUCTOR, `GuardKey` supports PARAMETER/FIELD/RECORD_COMPONENT, and the three thin annotations support METHOD only.

- [ ] **Step 2: Run focused tests and verify RED**

```bash
./mvnw -B -ntp \
  -pl egon-cola-components/egon-cola-component-access-guard-starter -am \
  -Dtest=AccessGuardApiTest,GuardOutcomeTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: test compilation fails because the V2 API/core types do not exist.

- [ ] **Step 3: Implement the exact public contracts**

Use these signatures:

```java
public interface GuardedOperation<T> {
    T execute() throws Throwable;
}

public interface AccessGuardClient {
    GuardOutcome evaluate(GuardRequest request);
    <T> T execute(GuardRequest request, GuardedOperation<T> operation) throws Throwable;
}

public record GuardRequest(
        String ruleId,
        Object[] arguments,
        Map<String, Object> attributes,
        Class<?> returnType,
        GuardedOperation<?> fallback
) { }
```

`GuardRequest` and `GuardInvocation` compact constructors validate nonblank rule IDs, clone arrays, and copy maps. `GuardFailure` contains only stable `category` and `code`; it never stores an exception message. `GuardOutcome.toString()` therefore cannot leak a raw key or throwable message.

Use the exact enum values approved by the Spec:

```text
GuardOutcomeType: ALLOWED, REJECTED, DEGRADED, FAILED
GuardDecision: PASS, DENY_LIST_HIT, ALLOW_LIST_MISS, PENALTY_ACTIVE,
               RATE_LIMITED, KEY_RESOLUTION_FAILED, STORE_FAILED,
               CONFIG_FAILED, TIME_LIMIT_EXCEEDED, EXECUTOR_REJECTED,
               BUSINESS_EXCEPTION
GuardResolution: NONE, THROWN, FALLBACK, RETURN_JSON, RETURN_NULL,
                 FAIL_OPEN, LOCAL_FALLBACK
GuardEntryType: AOP, AGENT, PROGRAMMATIC
GuardInvocationKind: METHOD, CONSTRUCTOR, OPERATION
```

`AccessGuardRejectedException` exposes stable error code `ACCESS_GUARD_REJECTED` and a `GuardOutcome`; its message contains rule ID, decision, and resolution only.

- [ ] **Step 4: Run focused and complete Starter tests for GREEN**

```bash
./mvnw -B -ntp \
  -pl egon-cola-components/egon-cola-component-access-guard-starter -am test
```

Expected: the new tests pass and the existing V1 tests still compile until Task 13 removes them.

- [ ] **Step 5: Commit Task 1**

```bash
git add egon-cola-components/egon-cola-component-access-guard-starter/src
git diff --cached --check
git commit -m "feat(access-guard): add v2 core contracts"
```

---

### Task 2: Replace the flat rule with validated, versioned GuardPlan snapshots

**Files:**
- Create: `egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/core/plan/GuardPlanProperties.java`
- Create: `egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/core/plan/GuardPlan.java`
- Create: `egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/core/plan/KeyConfig.java`
- Create: `egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/core/plan/AdmissionConfig.java`
- Create: `egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/core/plan/ExecutionConfig.java`
- Create: `egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/core/plan/FailurePolicies.java`
- Create: `egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/core/plan/ObservabilityConfig.java`
- Create: `egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/core/plan/GuardPlanSnapshot.java`
- Create: `egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/core/plan/GuardPlanSource.java`
- Create: `egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/core/plan/GuardPlanResolver.java`
- Create: `egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/core/plan/DefaultGuardPlanResolver.java`
- Create: `egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/core/plan/PropertiesGuardPlanSource.java`
- Create: `egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/core/plan/GuardPlanValidator.java`
- Create: `egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/core/plan/GuardPlanChangedEvent.java`
- Create: `egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/policy/PolicyConfig.java`
- Create: `egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/policy/allow/AllowListMode.java`
- Create: `egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/core/failure/FailurePoint.java`
- Create: `egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/core/failure/FailurePolicy.java`
- Create: `egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/execution/TimeLimitMode.java`
- Create: `egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/execution/TimeLimiterType.java`
- Create: `egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/execution/RejectionMode.java`
- Test: `egon-cola-components/egon-cola-component-access-guard-starter/src/test/java/top/egon/cola/component/accessguard/core/plan/DefaultGuardPlanResolverTest.java`
- Test: `egon-cola-components/egon-cola-component-access-guard-starter/src/test/java/top/egon/cola/component/accessguard/autoconfigure/AccessGuardPropertiesBindingTest.java`

**Interfaces:**
- Consumes: Task 1 outcome vocabulary and the approved YAML shape.
- Produces: immutable per-policy configs, `GuardPlanSource`, monotonic `GuardPlanSnapshot`, last-known-good dynamic updates, and strict property binding for later policy/auto-configuration tasks.

- [ ] **Step 1: Write failing property and snapshot tests**

```java
@Test
void bindsRulesAsMapAndRejectsUnknownFields() {
    contextRunner.withPropertyValues(
            "egon.cola.component.access-guard.rules.draw.rate-limit.enabled=true",
            "egon.cola.component.access-guard.rules.draw.rate-limit.capacity=100",
            "egon.cola.component.access-guard.rules.draw.unknown-option=true")
            .run(context -> assertThat(context).hasFailed());
}

@Test
void keepsLastValidSnapshotAfterInvalidNewerUpdate() {
    MutableGuardPlanSource source = new MutableGuardPlanSource(100);
    DefaultGuardPlanResolver resolver = resolver(source);
    source.publish(snapshot("draw", 1, validPlan()));
    source.publish(snapshot("draw", 2, invalidCapacityPlan()));
    assertThat(resolver.resolve("draw").version()).isEqualTo(1L);
    assertThat(resolver.lastFailure("draw")).isPresent();
}

@Test
void rejectsNonMonotonicDynamicVersion() {
    MutableGuardPlanSource source = new MutableGuardPlanSource(100);
    DefaultGuardPlanResolver resolver = resolver(source);
    source.publish(snapshot("draw", 2, validPlan()));
    assertThatThrownBy(() -> source.publish(snapshot("draw", 1, validPlan())))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("monotonic");
}
```

Also test duplicate/equal source priority failure, static fallback before the first dynamic snapshot, and no fallback to static after a dynamic source has published one valid snapshot.

- [ ] **Step 2: Run focused tests and verify RED**

```bash
./mvnw -B -ntp \
  -pl egon-cola-components/egon-cola-component-access-guard-starter -am \
  -Dtest=DefaultGuardPlanResolverTest,AccessGuardPropertiesBindingTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: compilation fails for the missing plan/source types; existing list-based rule binding cannot satisfy the map assertions.

- [ ] **Step 3: Implement decomposed plans and strict properties**

Keep the V1 `autoconfigure.AccessGuardProperties` temporarily so the repository remains compilable
until the one-time deletion in Task 13. Introduce the V2 binding model as:

```java
@Validated
@ConfigurationProperties(
        prefix = GuardPlanProperties.PREFIX,
        ignoreInvalidFields = false,
        ignoreUnknownFields = false)
public class GuardPlanProperties {
    public static final String PREFIX = "egon.cola.component.access-guard";
    private boolean enabled = true;
    private AccessGuardEngine engine = AccessGuardEngine.AOP;
    private Storage storage = Storage.LOCAL;
    private Defaults defaults = new Defaults();
    private Key key = new Key();
    private Redisson redisson = new Redisson();
    private Local local = new Local();
    private ThreadPool threadPool = new ThreadPool();
    private Map<String, Rule> rules = new LinkedHashMap<>();
}
```

Use nested property objects whose fields exactly cover the Spec YAML: key contributors/trusted proxies/HMAC secret; deny-list; allow-list mode; penalty threshold/violation TTL/penalty TTL; token bucket capacity/refill/requested tokens; time-limit mode/executor/timeout; rejection mode/fallback/JSON/null; per-failure-point policies; observability toggles. Do not retain V1 white-list/rate-limiter/blacklist/circuit-breaker globals.

`AdmissionConfig`'s four nested policy records implement the Task 2 `PolicyConfig` marker so Task 4
can type `GuardPolicy<C extends PolicyConfig>` without changing plan types later.
Define final enum values now: AllowList `GATE/BYPASS_RATE_LIMIT/BYPASS_RATE_LIMIT_AND_PENALTY`,
FailurePolicy `FAIL_OPEN/FAIL_CLOSED/LOCAL_FALLBACK`, TimeLimitMode
`DISABLED/OBSERVE_ONLY/ENFORCE`, TimeLimiterType
`CALLER_THREAD/THREAD_POOL/VIRTUAL_THREAD`, and RejectionMode
`THROW/FALLBACK/RETURN_JSON/RETURN_NULL`. No `GLOBAL_DEFAULT` value is introduced.

`GuardPlanSource` is:

```java
public interface GuardPlanSource {
    String name();
    int priority();
    Optional<GuardPlanSnapshot> current(String ruleId);
    AutoCloseable subscribe(Consumer<GuardPlanSnapshot> listener);
}
```

`DefaultGuardPlanResolver` validates before `AtomicReference` replacement, rejects old versions, records bounded failure metadata, and emits `GuardPlanChangedEvent` only after a successful replacement. `PropertiesGuardPlanSource` uses version `0`, source `properties`, and a stable SHA-256 configuration fingerprint.

- [ ] **Step 4: Run property, plan, and Starter tests for GREEN**

```bash
./mvnw -B -ntp \
  -pl egon-cola-components/egon-cola-component-access-guard-starter -am test
```

Expected: all plan/property tests and existing V1 tests pass. V1 properties remain registered only by
the V1 auto-configuration until Task 8 switches `AutoConfiguration.imports`; they are deleted and the
V2 type receives the final `AccessGuardProperties` name in Task 13.

- [ ] **Step 5: Commit Task 2**

```bash
git add egon-cola-components/egon-cola-component-access-guard-starter/src
git diff --cached --check
git commit -m "refactor(access-guard): add versioned guard plans"
```

---

### Task 3: Build the contributor-based key pipeline and privacy boundary

**Files:**
- Create: `egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/key/GuardKeyPart.java`
- Create: `egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/key/GuardKeyScope.java`
- Create: `egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/key/GuardKeyResolution.java`
- Create: `egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/key/GuardKeyResolver.java`
- Create: `egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/key/CompositeGuardKeyResolver.java`
- Create: `egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/key/KeyHasher.java`
- Create: `egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/key/HmacSha256KeyHasher.java`
- Create: `egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/key/TrustedProxyMatcher.java`
- Create: `egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/key/contributor/GuardKeyContributor.java`
- Create: `egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/key/contributor/ArgumentKeyContributor.java`
- Create: `egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/key/contributor/AttributeKeyContributor.java`
- Create: `egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/key/contributor/PrincipalKeyContributor.java`
- Create: `egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/key/contributor/HttpHeaderKeyContributor.java`
- Create: `egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/key/contributor/ClientIpKeyContributor.java`
- Create: `egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/key/contributor/GlobalKeyContributor.java`
- Create: `egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/key/GuardKeyResolutionException.java`
- Test: `egon-cola-components/egon-cola-component-access-guard-starter/src/test/java/top/egon/cola/component/accessguard/key/CompositeGuardKeyResolverTest.java`
- Test: `egon-cola-components/egon-cola-component-access-guard-starter/src/test/java/top/egon/cola/component/accessguard/key/HmacSha256KeyHasherTest.java`
- Test: `egon-cola-components/egon-cola-component-access-guard-starter/src/test/java/top/egon/cola/component/accessguard/key/ClientIpKeyContributorTest.java`

**Interfaces:**
- Consumes: Task 1 `GuardInvocation`, Task 2 `KeyConfig`, Java crypto, and optional Spring web request context.
- Produces: stable normalized parts plus one hashed storage identity; raw normalized content is confined to the resolver call and never retained in core outcome/state.

- [ ] **Step 1: Write failing key ordering, required, proxy, and privacy tests**

```java
@Test
void ordersCompositePartsByOrderThenDeclaration() {
    GuardKeyResolution resolution = resolver.resolve(invocation("tenant", "user"), keyConfig());
    assertThat(resolution.parts())
            .extracting(GuardKeyPart::name)
            .containsExactly("tenant", "user");
    assertThat(resolution.keyHash()).matches("[0-9a-f]{64}");
    assertThat(resolution.toString()).doesNotContain("tenant-1", "user-1");
}

@Test
void requiredMissingPartNeverFallsBackToGlobal() {
    assertThatThrownBy(() -> resolver.resolve(invocationWithNullUser(), keyConfig()))
            .isInstanceOf(GuardKeyResolutionException.class)
            .hasMessageNotContaining("user-1");
}

@Test
void ignoresForwardedForFromUntrustedRemoteAddress() {
    request.setRemoteAddr("203.0.113.7");
    request.addHeader("X-Forwarded-For", "10.0.0.8");
    assertThat(contributor.contribute(requestContext()))
            .contains(new GuardKeyPart("ip", "203.0.113.7", 0));
}
```

Test deterministic HMAC output with a fixed secret, header allowlist enforcement, maximum input length, record component access, explicit GLOBAL scope, and absence of raw values from exceptions and `toString()`.

- [ ] **Step 2: Run key tests and verify RED**

```bash
./mvnw -B -ntp \
  -pl egon-cola-components/egon-cola-component-access-guard-starter -am \
  -Dtest=CompositeGuardKeyResolverTest,HmacSha256KeyHasherTest,ClientIpKeyContributorTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: compilation fails because the contributor/key types do not exist.

- [ ] **Step 3: Implement deterministic normalization and HMAC hashing**

Use this contributor contract:

```java
public interface GuardKeyContributor {
    String id();
    List<GuardKeyPart> contribute(GuardInvocation invocation, KeyConfig config);
}
```

Normalize scalar values with stable UTF-8 string conversion, trim whitespace, reject control characters, cap each part at the validated configured limit, escape `|`, `=`, and `\`, then build `name=value` segments. Hash only the normalized byte sequence using `HmacSHA256` and the configured secret. GLOBAL hashes a reserved namespaced token; do not expose the token as a magic application key.

`TrustedProxyMatcher` supports explicit IPv4/IPv6 addresses and CIDR ranges. `ClientIpKeyContributor` reads `Forwarded`/`X-Forwarded-For` only when the direct remote address matches one configured trusted proxy. `HttpHeaderKeyContributor` reads only configured header names.

- [ ] **Step 4: Run all key and Starter tests for GREEN**

```bash
./mvnw -B -ntp \
  -pl egon-cola-components/egon-cola-component-access-guard-starter -am test
```

Expected: all key tests pass without sleep or request-global leakage.

- [ ] **Step 5: Commit Task 3**

```bash
git add egon-cola-components/egon-cola-component-access-guard-starter/src
git diff --cached --check
git commit -m "feat(access-guard): secure guard key resolution"
```

---

### Task 4: Add policy/store contracts plus DenyList and AllowList

**Files:**
- Create: `egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/policy/GuardContext.java`
- Create: `egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/policy/GuardPolicy.java`
- Create: `egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/policy/PolicyResult.java`
- Create: `egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/policy/allow/AllowListPolicy.java`
- Create: `egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/policy/deny/DenyListPolicy.java`
- Create: `egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/store/AllowListStore.java`
- Create: `egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/store/DenyListStore.java`
- Create: `egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/store/StoreOperationException.java`
- Create: `egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/store/local/LocalAllowListStore.java`
- Create: `egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/store/local/LocalDenyListStore.java`
- Test: `egon-cola-components/egon-cola-component-access-guard-starter/src/test/java/top/egon/cola/component/accessguard/policy/allow/AllowListPolicyTest.java`
- Test: `egon-cola-components/egon-cola-component-access-guard-starter/src/test/java/top/egon/cola/component/accessguard/policy/deny/DenyListPolicyTest.java`
- Create test fixture: `egon-cola-components/egon-cola-component-access-guard-starter/src/test/java/top/egon/cola/component/accessguard/store/GuardStoreContract.java`

**Interfaces:**
- Consumes: Task 2 policy configs and Task 3 hashed identity.
- Produces: small `GuardPolicy<C>`/`PolicyResult`, explicit list stores, and precise AllowList bypass flags; Task 5 completes the fixed built-in list consumed by Task 6 Engine.

- [ ] **Step 1: Write failing safety-order and list-policy tests**

```java
@Test
void allowListBypassNeverIncludesDenyList() {
    PolicyResult result = policy.evaluate(contextOnAllowList(),
            allowConfig(AllowListMode.BYPASS_RATE_LIMIT_AND_PENALTY));
    assertThat(result.bypassedPolicies())
            .containsExactlyInAnyOrder("penalty-box", "rate-limit")
            .doesNotContain("deny-list");
}

@Test
void denyListHitIsARealTerminalDecision() {
    when(store.contains(any())).thenReturn(true);
    assertThat(policy.evaluate(context(), denyConfig()).decision())
            .isEqualTo(GuardDecision.DENY_LIST_HIT);
}
```

Also test GATE miss/hit, disabled policies, local list data versions, and `StoreOperationException` propagation rather than conversion into reject/allow.

- [ ] **Step 2: Run policy tests and verify RED**

```bash
./mvnw -B -ntp \
  -pl egon-cola-components/egon-cola-component-access-guard-starter -am \
  -Dtest=AllowListPolicyTest,DenyListPolicyTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: compilation fails for the missing policy/store types.

- [ ] **Step 3: Implement the contracts and two list policies**

Use:

```java
public interface GuardPolicy<C extends PolicyConfig> {
    String id();
    PolicyResult evaluate(GuardContext context, C config);
}

public record PolicyResult(
        boolean allowed,
        GuardDecision decision,
        Set<String> bypassedPolicies,
        Duration retryAfter,
        long remainingTokens
) { }
```

`PolicyResult` factories validate that PASS is allowed and list/rate decisions are rejected. Stores accept only `ruleId`, `dataVersion`, and hashed key; no Store method accepts raw key text. They expose `contains`, `add`, `remove`, and atomic `replace` operations so static initialization and custom management code use the same contract. Task 5 creates the immutable complete built-in list after all four policy types exist.

`GuardStoreContract` defines reusable hashed-key membership, version isolation, TTL, real decision,
and `StoreOperationException` cases. Tasks 5 and 9 run this same contract against LOCAL and Redisson
fixtures rather than maintaining unrelated semantic assertions.

- [ ] **Step 4: Run policy and Starter tests for GREEN**

```bash
./mvnw -B -ntp \
  -pl egon-cola-components/egon-cola-component-access-guard-starter -am test
```

Expected: the policy tests pass and no Store catches its own infrastructure exception.

- [ ] **Step 5: Commit Task 4**

```bash
git add egon-cola-components/egon-cola-component-access-guard-starter/src
git diff --cached --check
git commit -m "feat(access-guard): add ordered list policies"
```

---

### Task 5: Implement bounded LOCAL PenaltyBox and Token Bucket semantics

**Files:**
- Create: `egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/store/PenaltyStore.java`
- Create: `egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/store/RateLimitBackend.java`
- Create: `egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/store/PenaltyKey.java`
- Create: `egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/store/PenaltyState.java`
- Create: `egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/store/RateLimitRequest.java`
- Create: `egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/store/RateLimitDecision.java`
- Create: `egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/store/local/LocalStateCleaner.java`
- Create: `egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/store/local/LocalPenaltyStore.java`
- Create: `egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/store/local/LocalRateLimitBackend.java`
- Create: `egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/policy/penalty/PenaltyService.java`
- Create: `egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/policy/penalty/DefaultPenaltyService.java`
- Create: `egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/policy/penalty/PenaltyBoxPolicy.java`
- Create: `egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/policy/ratelimit/RateLimitPolicy.java`
- Create: `egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/policy/AdmissionPolicies.java`
- Test: `egon-cola-components/egon-cola-component-access-guard-starter/src/test/java/top/egon/cola/component/accessguard/store/local/LocalPenaltyStoreTest.java`
- Test: `egon-cola-components/egon-cola-component-access-guard-starter/src/test/java/top/egon/cola/component/accessguard/store/local/LocalRateLimitBackendTest.java`
- Test: `egon-cola-components/egon-cola-component-access-guard-starter/src/test/java/top/egon/cola/component/accessguard/store/local/LocalStateCleanerTest.java`
- Test: `egon-cola-components/egon-cola-component-access-guard-starter/src/test/java/top/egon/cola/component/accessguard/policy/penalty/PenaltyBoxPolicyTest.java`
- Test: `egon-cola-components/egon-cola-component-access-guard-starter/src/test/java/top/egon/cola/component/accessguard/policy/AdmissionOrderContractTest.java`
- Test: `egon-cola-components/egon-cola-component-access-guard-starter/src/test/java/top/egon/cola/component/accessguard/store/local/LocalStoreContractTest.java`

**Interfaces:**
- Consumes: Task 2 penalty/rate configs and Task 4 policy/store contracts.
- Produces: concurrent LOCAL stores with injectable `Clock`/ticker, precise Token Bucket results, atomic in-process penalty transitions, capacity limits, and closable cleanup lifecycle.

- [ ] **Step 1: Write deterministic failing local-state tests**

```java
@Test
void refillsTokensProportionallyInsteadOfResettingWholeCapacity() {
    MutableTicker ticker = new MutableTicker();
    LocalRateLimitBackend backend = backend(ticker, 100);
    assertThat(acquire(backend, 10, 2, Duration.ofSeconds(1), 10).allowed()).isTrue();
    ticker.advance(Duration.ofSeconds(1));
    RateLimitDecision decision = acquire(backend, 10, 2, Duration.ofSeconds(1), 3);
    assertThat(decision.allowed()).isFalse();
    assertThat(decision.retryAfter()).isEqualTo(Duration.ofSeconds(1));
}

@Test
void thresholdTransitionAndTtlAreAtomic() {
    MutableClock clock = new MutableClock(INSTANT);
    LocalPenaltyStore store = store(clock, 100);
    assertThat(store.recordViolation(key(), 3, ONE_MINUTE, TEN_MINUTES).active()).isFalse();
    assertThat(store.recordViolation(key(), 3, ONE_MINUTE, TEN_MINUTES).active()).isFalse();
    assertThat(store.recordViolation(key(), 3, ONE_MINUTE, TEN_MINUTES).active()).isTrue();
    clock.advance(TEN_MINUTES.plusMillis(1));
    assertThat(store.current(key())).isEmpty();
}

@Test
void rejectsNewEntryWhenBoundedCapacityIsFull() {
    LocalPenaltyStore store = store(new MutableClock(INSTANT), 1);
    store.recordViolation(key("first"), 3, ONE_MINUTE, TEN_MINUTES);
    assertThatThrownBy(() -> store.recordViolation(key("second"), 3, ONE_MINUTE, TEN_MINUTES))
            .isInstanceOf(StoreOperationException.class)
            .hasMessageContaining("capacity");
}

@Test
void builtInOrderIsFixedAndComplete() {
    assertThat(AdmissionPolicies.builtIns(deny, allow, penalty, rate))
            .extracting(GuardPolicy::id)
            .containsExactly("deny-list", "allow-list", "penalty-box", "rate-limit");
}
```

Also test stateVersion isolation, idle eviction, counter TTL before threshold, concurrent threshold crossing, cleaner shutdown, and no background thread after `close()`.

- [ ] **Step 2: Run local-state tests and verify RED**

```bash
./mvnw -B -ntp \
  -pl egon-cola-components/egon-cola-component-access-guard-starter -am \
  -Dtest=LocalPenaltyStoreTest,LocalRateLimitBackendTest,LocalStateCleanerTest,PenaltyBoxPolicyTest,AdmissionOrderContractTest,LocalStoreContractTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: compilation fails because the LOCAL V2 stores do not exist.

- [ ] **Step 3: Implement bounded state and exact Token Bucket math**

Use one atomic `ConcurrentHashMap.compute` transition per key. Token refill is:

```java
long periods = elapsedNanos / refillPeriodNanos;
long added = Math.multiplyExact(periods, refillTokens);
long tokens = Math.min(capacity, previousTokens + added);
```

Preserve unused partial elapsed time; calculate `retryAfter` from missing tokens and the next refill boundary. Reject nonpositive capacity/refill/requested values during plan validation rather than normalizing them.

`LocalStateCleaner` uses one named daemon scheduler owned by the auto-configuration and invokes store eviction. It is `AutoCloseable`; tests call cleanup deterministically and only the lifecycle test exercises scheduler shutdown.

- [ ] **Step 4: Run local-state and Starter tests for GREEN**

```bash
./mvnw -B -ntp \
  -pl egon-cola-components/egon-cola-component-access-guard-starter -am test
```

Expected: deterministic tests pass with no `Thread.sleep`, unbounded map, or static executor.

- [ ] **Step 5: Commit Task 5**

```bash
git add egon-cola-components/egon-cola-component-access-guard-starter/src
git diff --cached --check
git commit -m "feat(access-guard): implement local guard stores"
```

---

### Task 6: Implement failure resolution and the single GuardEngine

**Files:**
- Create: `egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/core/GuardEngine.java`
- Create: `egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/core/DefaultGuardEngine.java`
- Create: `egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/core/GuardExecutionState.java`
- Create: `egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/core/GuardExecutionResult.java`
- Create: `egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/core/failure/FailurePolicyResolver.java`
- Create: `egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/core/failure/DefaultFailurePolicyResolver.java`
- Create: `egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/core/failure/FailureResolution.java`
- Create: `egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/adapter/programmatic/DefaultAccessGuardClient.java`
- Test: `egon-cola-components/egon-cola-component-access-guard-starter/src/test/java/top/egon/cola/component/accessguard/core/DefaultGuardEngineTest.java`
- Test: `egon-cola-components/egon-cola-component-access-guard-starter/src/test/java/top/egon/cola/component/accessguard/core/failure/DefaultFailurePolicyResolverTest.java`
- Test: `egon-cola-components/egon-cola-component-access-guard-starter/src/test/java/top/egon/cola/component/accessguard/adapter/programmatic/DefaultAccessGuardClientTest.java`

**Interfaces:**
- Consumes: Tasks 1-5 invocation, plan, key, policies, and local stores.
- Produces: one fixed-chain admission engine, policy-specific infrastructure fallback, and the first complete programmatic path. Task 7 adds execution resolution; Task 11 adds final events without creating another engine.

- [ ] **Step 1: Write failing chain, terminal-rejection, and failure-policy tests**

```java
@Test
void allowListNeverBypassesDenyList() {
    engine = engine(denyHit(), allowBypassAllLater(), penaltyPass(), ratePass());
    GuardOutcome outcome = engine.evaluate(invocation());
    assertThat(outcome.decision()).isEqualTo(GuardDecision.DENY_LIST_HIT);
    verifyNoInteractions(allowPolicy, penaltyPolicy, ratePolicy);
}

@Test
void allowListCanBypassPenaltyAndRateOnly() {
    engine = engine(denyPass(), allowBypassPenaltyAndRate(), penaltyPolicy, ratePolicy);
    assertThat(engine.evaluate(invocation()).type()).isEqualTo(GuardOutcomeType.ALLOWED);
    verifyNoInteractions(penaltyPolicy, ratePolicy);
}

@Test
void failOpenIsDegradedNotPass() {
    when(denyPolicy.evaluate(any(), any())).thenThrow(new StoreOperationException("redis"));
    when(failureResolver.resolve(any())).thenReturn(FailureResolution.failOpen());
    GuardOutcome outcome = engine.evaluate(invocation());
    assertThat(outcome.type()).isEqualTo(GuardOutcomeType.DEGRADED);
    assertThat(outcome.decision()).isEqualTo(GuardDecision.STORE_FAILED);
    assertThat(outcome.resolution()).isEqualTo(GuardResolution.FAIL_OPEN);
}
```

Cover defaults: key/deny/allow FAIL_CLOSED, penalty/rate LOCAL_FALLBACK, observability FAIL_OPEN. Prove a real RATE_LIMITED result never consults FailurePolicyResolver and that a failed local fallback uses its configured terminal policy.

- [ ] **Step 2: Run Engine tests and verify RED**

```bash
./mvnw -B -ntp \
  -pl egon-cola-components/egon-cola-component-access-guard-starter -am \
  -Dtest=DefaultGuardEngineTest,DefaultFailurePolicyResolverTest,DefaultAccessGuardClientTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: compilation fails for Engine/failure/client types.

- [ ] **Step 3: Implement one immutable chain and concrete failure matrix**

Use:

```java
public interface GuardEngine {
    GuardOutcome evaluate(GuardInvocation invocation);
    Object execute(GuardInvocation invocation) throws Throwable;
}
```

The Engine resolves exactly one snapshot, resolves one hashed key, then iterates the immutable built-in list. It carries bypass policy IDs forward, stops on the first real rejection, and never invokes business code from `evaluate`.

`DefaultFailurePolicyResolver` converts every configured default into one runtime value; no `GLOBAL_DEFAULT` exists. LOCAL_FALLBACK calls the matching local backend through a separate failure resolution path and returns `DEGRADED + STORE_FAILED + LOCAL_FALLBACK` when successful.

For this task, `execute` invokes the operation directly only after allowed/degraded admission and throws `AccessGuardRejectedException` for rejection. A package-private `executeWithOutcome` returns `GuardExecutionResult(value, outcome)` for deterministic Engine tests and later adapters; the public API still unwraps only the value. Task 7 replaces only execution-resolution collaborators, not the admission chain.

- [ ] **Step 4: Run Engine and complete Starter tests for GREEN**

```bash
./mvnw -B -ntp \
  -pl egon-cola-components/egon-cola-component-access-guard-starter -am test
```

Expected: all Engine/programmatic tests pass and business invocation counts prove rejections are terminal.

- [ ] **Step 5: Commit Task 6**

```bash
git add egon-cola-components/egon-cola-component-access-guard-starter/src
git diff --cached --check
git commit -m "feat(access-guard): implement unified guard engine"
```

---

### Task 7: Add TimeLimiter, validated fallback, and explicit rejection handling

**Files:**
- Create: `egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/execution/TimeLimiter.java`
- Create: `egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/execution/CallerThreadTimeLimiter.java`
- Create: `egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/execution/ThreadPoolTimeLimiter.java`
- Create: `egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/execution/VirtualThreadTimeLimiter.java`
- Create: `egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/execution/TimeLimitExceededException.java`
- Create: `egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/execution/ExecutorRejectedException.java`
- Create: `egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/execution/FallbackHandler.java`
- Create: `egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/execution/MethodHandleFallbackHandler.java`
- Create: `egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/execution/FallbackMethodCache.java`
- Create: `egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/execution/RejectionHandler.java`
- Create: `egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/execution/DefaultRejectionHandler.java`
- Create: `egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/execution/JsonRejectValueParser.java`
- Modify: `egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/core/DefaultGuardEngine.java`
- Modify: `egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/core/plan/GuardPlanValidator.java`
- Test: `egon-cola-components/egon-cola-component-access-guard-starter/src/test/java/top/egon/cola/component/accessguard/execution/TimeLimiterContractTest.java`
- Test: `egon-cola-components/egon-cola-component-access-guard-starter/src/test/java/top/egon/cola/component/accessguard/execution/MethodHandleFallbackHandlerTest.java`
- Test: `egon-cola-components/egon-cola-component-access-guard-starter/src/test/java/top/egon/cola/component/accessguard/execution/DefaultRejectionHandlerTest.java`
- Modify: `egon-cola-components/egon-cola-component-access-guard-starter/src/test/java/top/egon/cola/component/accessguard/core/DefaultGuardEngineTest.java`

**Interfaces:**
- Consumes: Task 1 invocation/outcome, Task 2 execution config, Task 6 Engine, and Spring-managed Jackson.
- Produces: caller observe-only, bounded platform pool, managed virtual-thread execution, startup-validated MethodHandles, THROW/FALLBACK/RETURN_JSON/RETURN_NULL, and accurate timeout/fallback outcomes.

- [ ] **Step 1: Write failing execution-resolution tests**

```java
@Test
void timeoutFallbackKeepsTimeoutDecision() throws Throwable {
    GuardExecutionResult result = engine.executeWithOutcome(timedOutInvocationWithFallback());
    GuardOutcome outcome = result.outcome();
    assertThat(outcome.type()).isEqualTo(GuardOutcomeType.DEGRADED);
    assertThat(outcome.decision()).isEqualTo(GuardDecision.TIME_LIMIT_EXCEEDED);
    assertThat(outcome.resolution()).isEqualTo(GuardResolution.FALLBACK);
}

@Test
void rejectRendererFailureNeverRunsBusinessOperation() {
    AtomicInteger calls = new AtomicInteger();
    when(rejectionHandler.resolve(any(), any())).thenThrow(new IllegalStateException("render"));
    assertThatThrownBy(() -> engine.execute(rejectedInvocation(calls::incrementAndGet)))
            .isInstanceOf(AccessGuardRejectedException.class);
    assertThat(calls).hasValue(0);
}

@Test
void validatesFallbackAndCachesMethodHandleBeforeExecution() {
    FallbackMethodCache cache = validator.validate(targetMethod(), "fallback", GuardPlanFixture.plan());
    MethodHandle handle = cache.lookup(targetMethod(), "fallback").orElseThrow();
    assertThat(cache.lookup(targetMethod(), "fallback")).containsSame(handle);
}
```

Cover matching arguments, arguments plus `GuardOutcome`, no arguments, static fallback, incompatible return type, fallback exception, primitive/constructor RETURN_NULL rejection, invalid JSON, Spring `ObjectMapper` custom module use, thread-pool saturation, virtual-thread lifecycle, and caller-thread observe-only validation.

- [ ] **Step 2: Run execution tests and verify RED**

```bash
./mvnw -B -ntp \
  -pl egon-cola-components/egon-cola-component-access-guard-starter -am \
  -Dtest=TimeLimiterContractTest,MethodHandleFallbackHandlerTest,DefaultRejectionHandlerTest,DefaultGuardEngineTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: compilation fails for the new execution contracts or assertions fail against direct Task 6 execution.

- [ ] **Step 3: Implement execution protection without a second admission path**

Use:

```java
public interface TimeLimiter {
    Object execute(GuardInvocation invocation, ExecutionConfig.TimeLimit config) throws Throwable;
}

public interface RejectionHandler {
    Object resolve(GuardInvocation invocation, GuardOutcome rejected) throws Throwable;
}
```

`CALLER_THREAD` is valid only for `OBSERVE_ONLY`: execute inline, measure elapsed, and never claim hard timeout. `THREAD_POOL` uses a named `ThreadPoolExecutor` with configured core/max/keepAlive/queue/rejection. `VIRTUAL_THREAD` uses one Spring-owned `ExecutorService` and closes it with the context. Timeout/cancel remains best effort.

`MethodHandleFallbackHandler` validates all annotated methods during Bean initialization and caches by executable plus fallback name. Programmatic calls use `GuardRequest.fallback` instead of reflective method lookup.

Engine maps timeout/rejection/business failure first, then applies rejection/fallback resolution, preserving the original decision. JSON uses the injected application `ObjectMapper`; no `new ObjectMapper()` remains.

- [ ] **Step 4: Run execution and Starter tests for GREEN**

```bash
./mvnw -B -ntp \
  -pl egon-cola-components/egon-cola-component-access-guard-starter -am test
```

Expected: execution tests pass; timeout/fallback/JSON results are never ordinary PASS.

- [ ] **Step 5: Commit Task 7**

```bash
git add egon-cola-components/egon-cola-component-access-guard-starter/src
git diff --cached --check
git commit -m "feat(access-guard): add time and rejection handling"
```

---

### Task 8: Replace the monolithic auto-configuration and add real Spring AOP/programmatic wiring

**Files:**
- Create: `egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/adapter/aop/GuardBinding.java`
- Create: `egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/adapter/aop/GuardBindingResolver.java`
- Create: `egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/adapter/aop/SpringAopGuardInvocation.java`
- Create: `egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/adapter/aop/SpringAopAccessGuardAdvisor.java`
- Create: `egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/autoconfigure/AccessGuardCoreAutoConfiguration.java`
- Create: `egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/autoconfigure/AccessGuardLocalStoreAutoConfiguration.java`
- Create: `egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/autoconfigure/AccessGuardAopAutoConfiguration.java`
- Create: `egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/autoconfigure/AccessGuardTimeLimitAutoConfiguration.java`
- Create: `egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/autoconfigure/AccessGuardStartupValidator.java`
- Modify: `egon-cola-components/egon-cola-component-access-guard-starter/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
- Test: `egon-cola-components/egon-cola-component-access-guard-starter/src/test/java/top/egon/cola/component/accessguard/adapter/aop/SpringAopAccessGuardAdvisorTest.java`
- Test: `egon-cola-components/egon-cola-component-access-guard-starter/src/test/java/top/egon/cola/component/accessguard/autoconfigure/AccessGuardAutoConfigurationV2Test.java`
- Test: `egon-cola-components/egon-cola-component-access-guard-starter/src/test/java/top/egon/cola/component/accessguard/autoconfigure/AccessGuardStartupValidatorTest.java`

**Interfaces:**
- Consumes: Tasks 1-7 complete local Engine and extension points.
- Produces: real proxy-based interception, rule-binding resolution, split conditional Beans, engine-mode exclusivity, AGENT integration startup validation, and a consumer-ready local Starter.

- [ ] **Step 1: Write failing real-proxy and context-matrix tests**

```java
@Test
void denyListRejectionThroughRealSpringProxyDoesNotCallTarget() {
    contextRunner.withUserConfiguration(GuardedServiceConfiguration.class)
            .withPropertyValues(drawDenyListProperties())
            .run(context -> {
                GuardedService service = context.getBean(GuardedService.class);
                assertThatThrownBy(() -> service.draw("user-1"))
                        .isInstanceOf(AccessGuardRejectedException.class);
                assertThat(service.calls()).isZero();
            });
}

@Test
void typeBindingIsOverriddenByMethodBinding() {
    assertThat(bindingResolver.resolve(TypeGuardedService.class.getMethod("draw")))
            .extracting(GuardBinding::ruleId)
            .isEqualTo("method-rule");
}

@Test
void agentModeWithoutIntegrationFailsAtStartup() {
    contextRunner.withPropertyValues("egon.cola.component.access-guard.engine=AGENT")
            .run(context -> assertThat(context).hasFailed()
                    .getFailure().hasMessageContaining("egon-cola-component-bytecode-starter"));
}
```

Also cover DISABLED (programmatic client remains, no advisor), `enabled=false` (no component Beans), dedicated annotation bound to a multi-policy plan fails, AOP constructor annotation on a Spring Bean fails, custom Store/TimeLimiter overrides, and context close releases executors/cleaners.

- [ ] **Step 2: Run AOP/auto-configuration tests and verify RED**

```bash
./mvnw -B -ntp \
  -pl egon-cola-components/egon-cola-component-access-guard-starter -am \
  -Dtest=SpringAopAccessGuardAdvisorTest,AccessGuardAutoConfigurationV2Test,AccessGuardStartupValidatorTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: tests fail because the V2 advisor/split auto-configurations do not exist.

- [ ] **Step 3: Implement one advisor and split auto-configurations**

Use a `StaticMethodMatcherPointcutAdvisor` plus `MethodInterceptor`; do not create seven annotation pointcuts. `GuardBindingResolver` recognizes exactly the four V2 annotations, applies direct method binding over type binding, and rejects multiple distinct bindings.

Register auto-configurations in dependency order:

```text
AccessGuardCoreAutoConfiguration
AccessGuardLocalStoreAutoConfiguration
AccessGuardTimeLimitAutoConfiguration
AccessGuardAopAutoConfiguration
```

Core provides properties, plan source/resolver, key resolver, failure resolver, Engine, and `AccessGuardClient`. AOP registers only for engine=AOP. Engine=DISABLED leaves programmatic Beans. Engine=AGENT registers no advisor and requires one `AccessGuardAgentIntegration` by `SmartInitializingSingleton`; missing or multiple integrations fail with actionable text.

`AccessGuardCoreAutoConfiguration` enables `GuardPlanProperties`. Replace the imports resource so it
contains only the V2 core/local/time/AOP classes at this stage; the old monolithic and old Redisson
auto-configurations remain compilable for their direct V1 tests but are no longer consumer imports.

- [ ] **Step 4: Run all Starter tests for GREEN**

```bash
./mvnw -B -ntp \
  -pl egon-cola-components/egon-cola-component-access-guard-starter -am test
```

Expected: the real proxy tests and conditional Bean matrix pass. No application is started.

- [ ] **Step 5: Commit Task 8**

```bash
git add egon-cola-components/egon-cola-component-access-guard-starter/src
git diff --cached --check
git commit -m "refactor(access-guard): wire v2 Spring adapters"
```

---

### Task 9: Implement fail-fast Redisson stores and atomic distributed state

**Files:**
- Modify: `egon-cola-components/egon-cola-component-access-guard-starter/pom.xml`
- Create: `egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/store/redisson/AccessGuardRedisKeyFactory.java`
- Create: `egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/store/redisson/RedissonAllowListStore.java`
- Create: `egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/store/redisson/RedissonDenyListStore.java`
- Create: `egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/store/redisson/RedissonPenaltyStore.java`
- Create: `egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/store/redisson/RedissonRateLimitBackend.java`
- Create: `egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/autoconfigure/AccessGuardV2RedissonAutoConfiguration.java`
- Modify: `egon-cola-components/egon-cola-component-access-guard-starter/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
- Test: `egon-cola-components/egon-cola-component-access-guard-starter/src/test/java/top/egon/cola/component/accessguard/store/redisson/AccessGuardRedisKeyFactoryTest.java`
- Test: `egon-cola-components/egon-cola-component-access-guard-starter/src/test/java/top/egon/cola/component/accessguard/store/redisson/RedissonStoreContractTest.java`
- Test: `egon-cola-components/egon-cola-component-access-guard-starter/src/test/java/top/egon/cola/component/accessguard/store/redisson/RedissonStoreIntegrationTest.java`
- Create: `egon-cola-components/egon-cola-component-access-guard-starter/src/test/java/top/egon/cola/component/accessguard/autoconfigure/AccessGuardRedissonAutoConfigurationV2Test.java`

**Interfaces:**
- Consumes: Tasks 2-6 storage contracts, local fallback, hashed key, and optional `RedissonClient`.
- Produces: versioned Redis keys, atomic penalty/token-bucket scripts, strict client selection, Store exceptions, runtime local fallback, and real Redis integration evidence gated from default builds.

- [ ] **Step 1: Write failing Redisson semantics and fail-fast tests**

```java
@Test
void redissonSelectionWithoutConfiguredClientFailsStartup() {
    contextRunner.withPropertyValues(
            "egon.cola.component.access-guard.storage=REDISSON",
            "egon.cola.component.access-guard.redisson.client-bean-name=guardRedis")
            .run(context -> assertThat(context).hasFailed()
                    .getFailure().hasMessageContaining("guardRedis"));
}

@Test
void keyFactoryNeverIncludesRawIdentity() {
    String key = factory.rateLimit("draw", "v2", HASH);
    assertThat(key).contains("draw", "v2", HASH).doesNotContain("raw-user-id");
}

@Test
void backendThrowsStoreFailureInsteadOfReturningRateLimited() {
    when(script.eval(any(), any(), any(), any())).thenThrow(new RedisException("down"));
    assertThatThrownBy(() -> backend.tryAcquire(request()))
            .isInstanceOf(StoreOperationException.class);
}
```

`RedissonStoreContractTest` extends the Task 4 `GuardStoreContract`, using mocked Redisson primitives
for default unit execution. The gated integration test uses
`@EnabledIfSystemProperty(named="egon.access.guard.redis.it", matches="true")` and proves multi-client shared rate state, atomic penalty threshold, TTL release, Redis outage local fallback, and recovery to Redis.

- [ ] **Step 2: Run unit/auto-config tests and verify RED**

```bash
./mvnw -B -ntp \
  -pl egon-cola-components/egon-cola-component-access-guard-starter -am \
  -Dtest=AccessGuardRedisKeyFactoryTest,RedissonStoreContractTest,AccessGuardRedissonAutoConfigurationV2Test \
  -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: compilation or assertions fail because V2 Redisson stores and fail-fast selection are absent.

- [ ] **Step 3: Implement atomic scripts and strict auto-configuration**

Keep Redisson optional. Add Testcontainers `1.21.4` dependencies at test scope only. Penalty Lua performs `INCR`, first-write counter expiry, threshold check, and penalty TTL write atomically. Token Bucket Lua obtains time from Redis `TIME`, stores tokens and last-refill timestamp in one hash, applies proportional refill, consumes requested tokens, and returns `{allowed, remaining, retryAfterMillis}`; JVM wall clocks never decide distributed token availability.

All scripts catch Redisson runtime failures only to wrap them in `StoreOperationException`; they do not read FailurePolicy. Manual list keys use `dataVersion`; penalty/rate keys use `stateVersion`.

Do not modify the unused V1 Redisson auto-configuration yet. In
`AccessGuardV2RedissonAutoConfiguration`, avoid `@ConditionalOnBean(RedissonClient.class)` at class
level. When storage=REDISSON, resolve the configured Bean explicitly and fail if missing, ambiguous,
or wrong type. LOCAL Beans must back off only after the selected storage has been validated. Task 13
deletes the V1 class and gives this class the final non-V2 name.

- [ ] **Step 4: Run unit tests, then gated Redis integration separately**

```bash
./mvnw -B -ntp \
  -pl egon-cola-components/egon-cola-component-access-guard-starter -am test

./mvnw -B -ntp \
  -pl egon-cola-components/egon-cola-component-access-guard-starter \
  -Degon.access.guard.redis.it=true \
  -Dtest=RedissonStoreIntegrationTest test
```

Expected: default tests pass without starting Redis/containers. The second command is run only where Docker/Redis integration is explicitly available; record SKIPPED rather than PASS when unavailable.

- [ ] **Step 5: Commit Task 9**

```bash
git add egon-cola-components/egon-cola-component-access-guard-starter
git diff --cached --check
git commit -m "feat(access-guard): add atomic Redisson stores"
```

---

### Task 10: Add optional Reactor and CompletionStage execution

**Files:**
- Modify: `egon-cola-components/egon-cola-component-access-guard-starter/pom.xml`
- Create: `egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/execution/async/CompletionStageGuardExecutor.java`
- Create: `egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/execution/reactive/ReactiveGuardExecutor.java`
- Create: `egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/execution/reactive/ReactorGuardExecutor.java`
- Create: `egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/autoconfigure/AccessGuardReactiveAutoConfiguration.java`
- Modify: `egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/adapter/aop/SpringAopAccessGuardAdvisor.java`
- Modify: `egon-cola-components/egon-cola-component-access-guard-starter/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
- Test: `egon-cola-components/egon-cola-component-access-guard-starter/src/test/java/top/egon/cola/component/accessguard/execution/async/CompletionStageGuardExecutorTest.java`
- Test: `egon-cola-components/egon-cola-component-access-guard-starter/src/test/java/top/egon/cola/component/accessguard/execution/reactive/ReactorGuardExecutorTest.java`
- Test: `egon-cola-components/egon-cola-component-access-guard-starter/src/test/java/top/egon/cola/component/accessguard/autoconfigure/AccessGuardReactiveAutoConfigurationTest.java`

**Interfaces:**
- Consumes: Task 6 Engine, Task 7 execution resolution, and Task 8 advisor.
- Produces: subscription-time Mono/Flux admission, Reactor `timeout`, nonblocking CompletionStage composition, and classpath-safe optional auto-configuration.

- [ ] **Step 1: Write failing lazy/reactive and nonblocking tests**

```java
@Test
void monoDoesNotEvaluateOrInvokeBeforeSubscription() {
    Mono<String> result = executor.guard(invocation(), () -> Mono.just("ok"));
    verifyNoInteractions(engine);
    StepVerifier.create(result).expectNext("ok").verifyComplete();
    verify(engine).evaluate(any());
}

@Test
void reactiveTimeoutUsesOperatorAndPreservesDecision() {
    StepVerifier.withVirtualTime(() -> executor.guard(timedInvocation(), Mono::never))
            .thenAwait(Duration.ofSeconds(1))
            .expectErrorSatisfies(error -> assertThat(outcome(error).decision())
                    .isEqualTo(GuardDecision.TIME_LIMIT_EXCEEDED))
            .verify();
}

@Test
void completionStageReturnsImmediatelyAndComposesTimeout() {
    CompletableFuture<String> pending = new CompletableFuture<>();
    CompletionStage<String> result = executor.guard(invocation(), () -> pending);
    assertThat(result.toCompletableFuture()).isNotDone();
}
```

Also test reactive fallback shape, rejection as error, Flux cancellation, business exception, final result only after termination, Reactor-absent context loading, and no submission to platform TimeLimiter executors.

- [ ] **Step 2: Run async/reactive tests and verify RED**

```bash
./mvnw -B -ntp \
  -pl egon-cola-components/egon-cola-component-access-guard-starter -am \
  -Dtest=CompletionStageGuardExecutorTest,ReactorGuardExecutorTest,AccessGuardReactiveAutoConfigurationTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: compilation fails before optional Reactor executor/dependencies are added.

- [ ] **Step 3: Implement optional nonblocking adapters**

Declare `reactor-core` optional and `reactor-test` test-scoped. `ReactorGuardExecutor` uses `Mono.defer`/`Flux.defer`, native `timeout`, `onErrorResume` for configured fallback, and `doOnEach`/`doFinally` only to finalize one outcome. It never calls `block`, `subscribe`, `Future.get`, or the platform/virtual TimeLimiter.

`CompletionStageGuardExecutor` composes `orTimeout`/`handle` without blocking. Advisor checks declared return type and delegates only when the relevant adapter Bean exists; an explicitly reactive rule without Reactor fails startup.

- [ ] **Step 4: Run Starter tests for GREEN and check optional dependency shape**

```bash
./mvnw -B -ntp \
  -pl egon-cola-components/egon-cola-component-access-guard-starter -am test

./mvnw -B -ntp \
  -pl egon-cola-components/egon-cola-component-access-guard-starter dependency:tree \
  -Dincludes=io.projectreactor:reactor-core
```

Expected: tests pass; Reactor appears only as the Starter's optional direct dependency and is not required by core class loading.

- [ ] **Step 5: Commit Task 10**

```bash
git add egon-cola-components/egon-cola-component-access-guard-starter
git diff --cached --check
git commit -m "feat(access-guard): add reactive guard execution"
```

---

### Task 11: Add one final event, bounded metrics, structured logging, and read-only Actuator

**Files:**
- Modify: `egon-cola-components/egon-cola-component-access-guard-starter/pom.xml`
- Create: `egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/observability/GuardEvent.java`
- Create: `egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/observability/GuardStageEvent.java`
- Create: `egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/observability/GuardEventListener.java`
- Create: `egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/observability/GuardEventPublisher.java`
- Create: `egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/observability/CompositeGuardEventPublisher.java`
- Create: `egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/observability/GuardInvocationFinalizer.java`
- Create: `egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/observability/LoggingGuardEventListener.java`
- Create: `egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/observability/MicrometerGuardEventListener.java`
- Create: `egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/observability/AccessGuardEndpoint.java`
- Create: `egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/autoconfigure/AccessGuardObservabilityAutoConfiguration.java`
- Modify: `egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/core/DefaultGuardEngine.java`
- Modify: `egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/execution/async/CompletionStageGuardExecutor.java`
- Modify: `egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/execution/reactive/ReactorGuardExecutor.java`
- Modify: `egon-cola-components/egon-cola-component-access-guard-starter/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
- Test: `egon-cola-components/egon-cola-component-access-guard-starter/src/test/java/top/egon/cola/component/accessguard/observability/GuardObservabilityTest.java`
- Test: `egon-cola-components/egon-cola-component-access-guard-starter/src/test/java/top/egon/cola/component/accessguard/observability/AccessGuardEndpointTest.java`
- Test: `egon-cola-components/egon-cola-component-access-guard-starter/src/test/java/top/egon/cola/component/accessguard/autoconfigure/AccessGuardObservabilityAutoConfigurationTest.java`

**Interfaces:**
- Consumes: Task 1 final outcome, Task 2 plan resolver, Task 5 local entry counts, and Task 6 Engine.
- Produces: exactly one final event by default, optional stage diagnostics, five stable metrics, safe logs, and `/actuator/accessguard` without changing governance decisions on observer failure.

- [ ] **Step 1: Write failing event/cardinality/privacy tests**

```java
@Test
void publishesExactlyOneFinalEventForFallback() throws Throwable {
    engine.execute(fallbackInvocation());
    assertThat(listener.finalEvents()).singleElement()
            .satisfies(event -> {
                assertThat(event.outcome().decision()).isEqualTo(GuardDecision.RATE_LIMITED);
                assertThat(event.outcome().resolution()).isEqualTo(GuardResolution.FALLBACK);
            });
}

@Test
void metricsNeverUseKeyOrMethodTags() {
    listener.onEvent(event());
    assertThat(registry.getMeters()).allSatisfy(meter ->
            assertThat(meter.getId().getTags())
                    .extracting(Tag::getKey)
                    .doesNotContain("key", "keyHash", "method", "exceptionMessage"));
}

@Test
void endpointDoesNotExposeListMembersOrKeys() {
    Map<String, Object> response = endpoint.accessguard();
    assertThat(response.toString()).contains("draw", "planVersion")
            .doesNotContain("user-1", "Authorization", "fallback arguments");
}
```

Also test observer exception FAIL_OPEN, debug stage events not incrementing final call count, logging without raw/fingerprint values, Micrometer absent/present, and Actuator absent/present.

- [ ] **Step 2: Run observability tests and verify RED**

```bash
./mvnw -B -ntp \
  -pl egon-cola-components/egon-cola-component-access-guard-starter -am \
  -Dtest=GuardObservabilityTest,AccessGuardEndpointTest,AccessGuardObservabilityAutoConfigurationTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: compilation fails for the V2 observability types.

- [ ] **Step 3: Implement safe finalization and stable names**

Register exactly:

```text
egon.access.guard.calls                 Counter
egon.access.guard.duration              Timer
egon.access.guard.store.failures        Counter
egon.access.guard.plan.reloads          Counter
egon.access.guard.local.entries         Gauge
```

Allowed tags are exactly `ruleId`, `policy`, `type`, `decision`, `resolution`, `engine`, and `storage`. `GuardInvocationFinalizer` owns an `AtomicBoolean` terminal guard and observer-failure isolation. The synchronous Engine closes it directly; `CompletionStageGuardExecutor` and `ReactorGuardExecutor` close the same invocation-scoped finalizer from their terminal completion/signal callbacks. This makes sync, async, reactive, timeout, rejection, fallback, cancellation, and observer failure publish at most one final event without treating asynchronous method return as completion.

Declare Actuator optional. `AccessGuardEndpoint` returns rule IDs, versions, source, enabled policies, storage, and bounded health only. Do not expose raw config secrets, list values, keys, parameters, or fallback arguments.

- [ ] **Step 4: Run Starter tests and optional dependency tree checks**

```bash
./mvnw -B -ntp \
  -pl egon-cola-components/egon-cola-component-access-guard-starter -am test

./mvnw -B -ntp \
  -pl egon-cola-components/egon-cola-component-access-guard-starter dependency:tree \
  -Dincludes=io.micrometer:micrometer-core,org.springframework.boot:spring-boot-actuator
```

Expected: tests pass and both observability dependencies remain optional.

- [ ] **Step 5: Commit Task 11**

```bash
git add egon-cola-components/egon-cola-component-access-guard-starter
git diff --cached --check
git commit -m "feat(access-guard): add safe guard observability"
```

---

### Task 12: Migrate Bytecode Agent to the generic Bridge and unified Engine

**Files:**
- Modify: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-core/src/main/java/top/egon/cola/component/bytecode/core/enhance/accessguard/GovernanceAnnotationFilter.java`
- Modify: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-core/src/main/java/top/egon/cola/component/bytecode/core/enhance/accessguard/AccessGuardMatcher.java`
- Modify: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-core/src/main/java/top/egon/cola/component/bytecode/core/enhance/accessguard/AccessGuardPolicy.java`
- Modify: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-core/src/main/java/top/egon/cola/component/bytecode/core/enhance/accessguard/ConstructorGuardEnhancer.java`
- Modify: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-starter/src/main/java/top/egon/cola/component/bytecode/starter/accessguard/AccessGuardRuntimeAdapter.java`
- Modify: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-starter/src/main/java/top/egon/cola/component/bytecode/starter/accessguard/AccessGuardAgentAutoConfiguration.java`
- Modify: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-core/src/test/java/top/egon/cola/component/bytecode/core/enhance/accessguard/AccessGuardMatcherTest.java`
- Modify: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-core/src/test/java/top/egon/cola/component/bytecode/core/enhance/accessguard/ConstructorGuardEnhancerTest.java`
- Modify: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-starter/src/test/java/top/egon/cola/component/bytecode/starter/accessguard/AccessGuardRuntimeAdapterTest.java`
- Modify: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-test/src/test/java/sample/bytecode/agent/AccessGuardAgentFixture.java`
- Modify: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-test/src/test/java/top/egon/cola/component/bytecode/test/agent/AccessGuardAgentIntegrationTest.java`
- Modify: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-test/src/it/access-guard-spring/src/test/java/sample/accessguard/AccessGuardSpringAgentTest.java`

**Interfaces:**
- Consumes: Existing `BridgeGuardedInvocation`/`BridgeConstructorInvocation`, Task 1 Agent integration marker, Task 6 `GuardEngine`, and Task 8 AGENT startup validation.
- Produces: new annotation descriptors, type-level matching, direct Bridge-to-`GuardInvocation` adaptation, one method/constructor engine, and fail-closed pre-ready constructors without changing the JDK-only bridge dependency boundary.

- [ ] **Step 1: Rewrite tests first for V2 descriptors and Engine calls**

```java
@Test
void matchesOnlyV2GuardAnnotations() {
    assertThat(filter.isGovernance("Ltop/egon/cola/component/accessguard/api/AccessGuard;"))
            .isTrue();
    assertThat(filter.isGovernance("Ltop/egon/cola/component/accessguard/annotation/DoHystrix;"))
            .isFalse();
}

@Test
void constructorAlwaysEmitsFailClosedHint() {
    AccessGuardPolicy policy = matcher.match(owner, annotatedConstructor()).orElseThrow();
    assertThat(policy.constructorFailHint()).isEqualTo(BridgeFailHint.FAIL_CLOSED);
}

@Test
void runtimeAdapterCallsGuardEngineWithoutProceedingJoinPoint() throws Throwable {
    adapter.markReady();
    adapter.invokeGuarded(bridgeInvocation());
    verify(engine).execute(argThat(invocation ->
            invocation.entryType() == GuardEntryType.AGENT));
}
```

Also assert type annotation matching with method override, no transform-time reading of timeout/fallback fields, constructor plan validation, method/constructor outcome parity, and missing runtime constructor rejection.

- [ ] **Step 2: Run Bytecode core/starter tests and verify RED**

```bash
./mvnw -B -ntp \
  -pl egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-core,egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-starter \
  -am -Dtest=AccessGuardMatcherTest,ConstructorGuardEnhancerTest,AccessGuardRuntimeAdapterTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: matcher/adapter tests fail against V1 descriptors and `AgentProceedingJoinPoint` adaptation.

- [ ] **Step 3: Adapt the existing generic Bridge without adding Access Guard bridge types**

`GovernanceAnnotationFilter` contains exactly four descriptors: AccessGuard, AllowListGuard, RateLimitGuard, TimeLimitGuard under the V2 `api` package. `AccessGuardMatcher` validates bytecode-only constraints and class/method annotation presence; it does not parse rule options that now live in GuardPlan.

Constructors always embed `BridgeFailHint.FAIL_CLOSED`. Keep `BridgeGuardedInvocation`, `BridgeConstructorInvocation`, `EgonPolicyBridge`, and `GuardedInvocationEvaluator` JDK-only and otherwise unchanged.

`AccessGuardRuntimeAdapter` implements both `GuardedInvocationEvaluator` and `AccessGuardAgentIntegration`. It resolves reflection metadata and rule binding, creates `GuardInvocation` with `entryType=AGENT`, then calls the same `GuardEngine`; no AspectJ type, `AgentProceedingJoinPoint`, separate constructor service, or AccessGuard FailureHandler remains.

- [ ] **Step 4: Run Bytecode unit and Agent integration verification**

```bash
./mvnw -B -ntp \
  -pl egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-core,egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-starter \
  -am test

./mvnw -B -ntp \
  -pl egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-test \
  -am -Dinvoker.test=access-guard-spring verify
```

Expected: Bytecode unit tests and the forked `-javaagent` access-guard fixture pass; the process is test-scoped and exits.

- [ ] **Step 5: Commit Task 12**

```bash
git add egon-cola-components/egon-cola-component-bytecode
git diff --cached --check
git commit -m "refactor(bytecode): adapt access guard v2 engine"
```

---

### Task 13: Delete V1 APIs/paths, migrate documentation, and close cross-entry contracts

**Files:**
- Delete: `egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/agent/`
- Delete: `egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/annotation/`
- Delete: `egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/blacklist/`
- Delete: `egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/circuitbreaker/`
- Delete: `egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/config/`
- Delete: `egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/context/`
- Delete: `egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/event/`
- Delete: `egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/exception/`
- Delete: `egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/ratelimiter/`
- Delete: `egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/reject/`
- Delete: `egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/whitelist/`
- Delete: `egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/aop/AccessGuardAop.java`
- Delete: `egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/autoconfigure/AccessGuardAutoConfiguration.java`
- Delete: `egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/autoconfigure/AccessGuardProperties.java`
- Delete: `egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/autoconfigure/AccessGuardRedissonAutoConfiguration.java`
- Move: `egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/core/plan/GuardPlanProperties.java` to `egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/autoconfigure/AccessGuardProperties.java`
- Move: `egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/autoconfigure/AccessGuardV2RedissonAutoConfiguration.java` to `egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/autoconfigure/AccessGuardRedissonAutoConfiguration.java`
- Delete: `egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/execution/AccessGuardExecutionService.java`
- Delete: `egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/execution/AccessGuardFailureHandler.java`
- Delete: `egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/execution/ConstructorAccessGuardExecutionService.java`
- Delete: `egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/execution/ConstructorAccessGuardValidator.java`
- Delete: `egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/execution/ConstructorGuardResult.java`
- Delete: `egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/key/AccessKeyResolution.java`
- Delete: `egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/key/AccessGuardKeyGenerator.java`
- Delete: `egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/key/AccessKeyResolver.java`
- Delete: `egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/key/CompositeAccessKeyResolver.java`
- Delete: `egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/key/DefaultAccessKeyResolver.java`
- Delete: `egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/key/ExecutableAccessKeyResolver.java`
- Delete: `egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/support/`
- Delete: legacy V1 tests currently under `src/test/java/top/egon/cola/component/accessguard/{agent,annotation,aop,blacklist,circuitbreaker,ratelimiter,reject,whitelist}`
- Delete: V1 execution tests `AccessGuardExecutionServiceTest.java`, `AccessGuardFailureHandlerTest.java`, `ConstructorAccessGuardExecutionServiceTest.java`, `ConstructorAccessGuardValidatorTest.java`, and `StaticAccessGuardExecutionTest.java`
- Delete: V1 key test `DefaultAccessKeyResolverTest.java`
- Delete: V1 support test `AccessGuardRedisKeysTest.java`
- Delete: V1 auto-configuration tests `AccessGuardAutoConfigurationTest.java` and `AccessGuardRedissonAutoConfigurationTest.java`
- Move: `egon-cola-components/egon-cola-component-access-guard-starter/src/test/java/top/egon/cola/component/accessguard/autoconfigure/AccessGuardAutoConfigurationV2Test.java` to `egon-cola-components/egon-cola-component-access-guard-starter/src/test/java/top/egon/cola/component/accessguard/autoconfigure/AccessGuardAutoConfigurationTest.java`
- Move: `egon-cola-components/egon-cola-component-access-guard-starter/src/test/java/top/egon/cola/component/accessguard/autoconfigure/AccessGuardRedissonAutoConfigurationV2Test.java` to `egon-cola-components/egon-cola-component-access-guard-starter/src/test/java/top/egon/cola/component/accessguard/autoconfigure/AccessGuardRedissonAutoConfigurationTest.java`
- Modify: `egon-cola-components/egon-cola-component-access-guard-starter/src/test/java/top/egon/cola/component/accessguard/test/AccessGuardSampleTest.java`
- Create: `egon-cola-components/egon-cola-component-access-guard-starter/src/test/java/top/egon/cola/component/accessguard/contract/AccessGuardEntryContractTest.java`
- Create: `egon-cola-components/egon-cola-component-access-guard-starter/src/test/java/top/egon/cola/component/accessguard/contract/AccessGuardConfigurationSurfaceTest.java`
- Modify: `egon-cola-components/egon-cola-component-access-guard-starter/README.md`
- Modify: `egon-cola-components/egon-cola-component-access-guard-starter/README.zh-CN.md`
- Modify: `README.md`

**Interfaces:**
- Consumes: the complete V2 implementation and Bytecode Agent path from Tasks 1-12.
- Produces: no V1 runtime surface, synchronized bilingual usage/migration docs, one AOP/programmatic contract in the Starter, Agent contract in Bytecode, and configuration-surface proof that every property is read or removed.

- [ ] **Step 1: Capture the RED cleanup inventory and add cross-entry contract assertions**

Run before deletion:

```bash
rg -n -g '!target/**' \
  -g '!docs/superpowers/specs/**' -g '!docs/superpowers/plans/**' \
  'DoWhiteList|DoRateLimiter|DoHystrix|BYPASS_GUARD|AgentProceedingJoinPoint|AccessGuardExecutionService|TimeoutCircuitBreaker' \
  egon-cola-components README.md
```

Expected: current V1 source/tests/README matches remain.

Add a parameterized contract that runs the same allow, deny, penalty, rate-limit, fail-open, local-fallback, timeout, and fallback cases through AOP and programmatic entry fixtures and compares:

```java
assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
assertThat(actual.businessCalls()).isEqualTo(expected.businessCalls());
assertThat(actual.finalEvents()).containsExactlyElementsOf(expected.finalEvents());
```

The Agent fixture in Task 12 uses the same literal scenario table and expected outcomes.

- [ ] **Step 2: Run contract tests and verify RED where old paths still leak**

```bash
./mvnw -B -ntp \
  -pl egon-cola-components/egon-cola-component-access-guard-starter -am \
  -Dtest=AccessGuardEntryContractTest,AccessGuardConfigurationSurfaceTest,AccessGuardSampleTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: the new V2 entry contract compiles; `AccessGuardConfigurationSurfaceTest` fails because the
legacy annotation/property/auto-configuration surface is still present. `AccessGuardSampleTest` is
rewritten in Step 3, after the failing inventory has proved the cleanup requirement.

- [ ] **Step 3: Delete V1 production/test paths and rewrite consumer docs**

Delete only the listed V1 paths; preserve all V2 files created in Tasks 1-12. Move the temporary V2
property/Redisson class names to their final names, update every V2 import and
`AutoConfiguration.imports`, then rename the two V2 auto-configuration tests as listed. Rewrite both
READMEs around:

```java
@AccessGuard("draw")
public DrawResult draw(@GuardKey("user") String userId) {
    return drawService.execute(userId);
}
```

Document the exact YAML map, fixed order, three AllowList modes, failure matrix, local/Redisson selection, trusted proxies/HMAC secret, TimeLimiter bounds, reactive behavior, Agent constructor limitations, metrics, endpoint, and evidence boundary. Add an explicit V1 -> V2 migration table and state that three `Do*` annotations have no compatibility artifact.

Historical approved specs/plans remain unchanged; searches exclude `docs/superpowers` rather than rewriting history.

- [ ] **Step 4: Run zero-reference, Starter, Bytecode, and Components Reactor verification**

```bash
test -z "$(rg -l -g '!target/**' \
  -g '!docs/superpowers/specs/**' -g '!docs/superpowers/plans/**' \
  'DoWhiteList|DoRateLimiter|DoHystrix|BYPASS_GUARD|AgentProceedingJoinPoint|AccessGuardExecutionService|TimeoutCircuitBreaker' \
  egon-cola-components README.md)"

./mvnw -B -ntp \
  -pl egon-cola-components/egon-cola-component-access-guard-starter -am clean test

./mvnw -B -ntp \
  -pl egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-test \
  -am -Dinvoker.test=access-guard-spring verify

./mvnw -B -ntp -f egon-cola-components/pom.xml test
```

Expected: zero current V1 references; all three commands exit 0. Report Redis integration separately if Task 9's gated command was not run.

- [ ] **Step 5: Commit Task 13**

```bash
git add README.md \
  egon-cola-components/egon-cola-component-access-guard-starter \
  egon-cola-components/egon-cola-component-bytecode
git diff --cached --check
git commit -m "refactor(access-guard): remove v1 compatibility surface"
```

---

### Task 14: Move the complete reactor to the 6.0.0 release line and run release gates

**Files:**
- Modify mechanically: every tracked `pom.xml` containing the exact current reactor version `5.3.1` (baseline inventory count: 66)
- Modify: `README.md`
- Modify: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/pom.xml`
- Modify: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/pom.xml`
- Modify: `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/pom.xml`

**Interfaces:**
- Consumes: Tasks 1-13 fully green V2 source and the approved breaking-release decision.
- Produces: one consistent `6.0.0` reactor/BOM/archetype consumer version and root release verification. This task is deliberately isolated because it changes repository-wide release metadata, not Access Guard behavior.

- [ ] **Step 1: Verify the precondition and exact mechanical inventory**

```bash
test "$(./mvnw help:evaluate -Dexpression=project.version -q -DforceStdout -N)" = "5.3.1"
test "$(rg -l '<version>5\.3\.1</version>' --glob 'pom.xml' | wc -l | tr -d ' ')" = "66"
git status --short
```

Expected: version is 5.3.1, inventory is 66 POMs, and the worktree is clean after Task 13.

- [ ] **Step 2: Apply the reactor-wide version with Maven Versions Plugin**

```bash
./mvnw -B -ntp versions:set \
  -DnewVersion=6.0.0 \
  -DprocessAllModules=true \
  -DgenerateBackupPoms=false
```

Expected: root and every reactor parent/dependency reference moves together; no `pom.xml.versionsBackup` file exists.

- [ ] **Step 3: Update non-reactor consumer examples and archetype templates**

Replace root README `archetypeVersion='5.3.1'` examples with `6.0.0`. Change the three archetype resource properties from:

```xml
<egon-cola.version>5.3.1</egon-cola.version>
```

to:

```xml
<egon-cola.version>6.0.0</egon-cola.version>
```

Do not rewrite historical specs/plans or `@Deprecated(since = "5.3.1")` declarations.

- [ ] **Step 4: Run version, release-shape, and root verification**

```bash
test "$(./mvnw help:evaluate -Dexpression=project.version -q -DforceStdout -N)" = "6.0.0"
test -z "$(rg -l '<version>5\.3\.1</version>' --glob 'pom.xml')"
test -z "$(rg -n "archetypeVersion='5\.3\.1'" README.md)"
test -z "$(rg -n '<egon-cola.version>5\.3\.1</egon-cola.version>' \
  egon-cola-archetypes/*/src/main/resources/archetype-resources/pom.xml)"

./mvnw -B -ntp clean integration-test
```

Expected: version assertions are empty/true and the full root lifecycle exits 0 without starting a long-lived service. If gated Redis or real multi-JVM validation was not run, state that limitation explicitly even when Maven is green.

- [ ] **Step 5: Audit the complete delivery against the approved Spec**

```bash
git diff --check
git status --short
rg -n -g '!target/**' \
  -g '!docs/superpowers/specs/**' -g '!docs/superpowers/plans/**' \
  'DoWhiteList|DoRateLimiter|DoHystrix|BYPASS_GUARD|AgentProceedingJoinPoint|AccessGuardExecutionService|TimeoutCircuitBreaker' \
  egon-cola-components README.md
```

Expected: no whitespace error and no current V1 reference. Check all 28 acceptance criteria in `docs/superpowers/specs/2026-07-29-access-guard-v2-remediation-design.md`; do not mark complete if any criterion lacks a test/result.

- [ ] **Step 6: Commit Task 14**

```bash
git add pom.xml README.md egon-cola-archetypes egon-cola-components
git diff --cached --check
git commit -m "chore(release): prepare 6.0.0"
```

---

## Spec Coverage Matrix

| Approved Spec area | Implementing tasks |
| --- | --- |
| Single Starter, tests under `src/test`, no new Access Guard modules | Global constraints, Tasks 1-13 |
| V2 annotations, programmatic API, immutable invocation/outcome | Task 1 |
| Decomposed plans, strict properties, dynamic last-known-good snapshots | Task 2 |
| Contributor key pipeline, trusted proxies, HMAC and privacy | Task 3 |
| DenyList/AllowList separation and safe bypass modes | Task 4 |
| PenaltyBox, Token Bucket, bounded LOCAL state and fixed order | Task 5 |
| Failure matrix, terminal rejection and one GuardEngine | Task 6 |
| TimeLimiter, fallback, JSON/null rejection and managed executors | Task 7 |
| AOP/programmatic wiring, engine modes and startup validation | Task 8 |
| Atomic Redisson stores, strict client selection and recovery | Task 9 |
| Optional Reactor/CompletionStage behavior | Task 10 |
| Final events, safe metrics/logging and read-only Actuator | Task 11 |
| Generic Bytecode Bridge reuse and constructor fail-closed | Task 12 |
| V1 deletion, migration docs and three-entry contracts | Task 13 |
| Repository-wide breaking version and release verification | Task 14 |
| No ConcurrencyLimit, database, Flyway, service startup or topology overclaim | Global constraints and final evidence report |

All 28 acceptance criteria in the approved Spec map to at least one row above; Task 14 repeats the
criterion-by-criterion audit as the final completion gate.

## Execution Checkpoints

1. After Task 2: confirm the final property metadata and dynamic snapshot API before policy implementation.
2. After Task 6: review the fixed chain and failure matrix before adding execution protection.
3. After Task 9: record whether real Redis integration ran or was skipped.
4. After Task 12: require the forked Agent test before deleting `AgentProceedingJoinPoint`.
5. After Task 13: compare all three entry outcomes and verify zero current V1 references.
6. Before Task 14: require a clean worktree and all functional tests green; version change remains a separate commit.

## Final Evidence Report

The implementation handoff must list, without overclaiming:

- commits for Tasks 1-14;
- focused and reactor Maven commands with pass/fail/skip results;
- exact Access Guard and Bytecode test counts;
- whether gated Redis/Testcontainers ran;
- whether the forked `-javaagent` fixture ran;
- whether Reactor classpath-present and classpath-absent cases ran;
- root `clean integration-test` result;
- remaining limitations for real multi-JVM Redis behavior, proxy topology, uninterruptible I/O, and user follow-up validation.
