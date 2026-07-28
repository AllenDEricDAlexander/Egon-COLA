# Snowflake ID Starter Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the `uuid-creator`-backed default ID capability with a pure-JDK, CAS-based Snowflake generator and one directly aggregated `egon-cola-component-common-id-starter` module while retaining deprecated pure-JDK UUIDv7 compatibility APIs.

**Architecture:** `egon-cola-component-common-id` owns all Spring-free contracts, fixed bit layout, parsing, stateful generation, time abstraction, and compatibility UUIDv7 code. A single direct child module, `egon-cola-component-common-id-starter`, owns Boot 3 properties, validation, conditional auto-configuration, registration metadata, module-local tests, and bilingual consumer documentation.

**Tech Stack:** Java 21, JDK `AtomicLong`/`Duration`/`Instant`/`LockSupport`/`SecureRandom`, JUnit Jupiter, AssertJ, Spring Boot 3.5.x `ApplicationContextRunner`, Maven reactor/BOM/Central Publishing profiles.

## Global Constraints

- Use exactly 1 sign bit, 41 elapsed-millisecond bits, 10 machine-ID bits, and 12 sequence bits.
- Fix Epoch at `2026-01-01T00:00:00Z`; do not expose Epoch, bit allocation, or algorithm structure as configuration.
- Generate positive Java `long` IDs; reject negative parse inputs.
- Support machine IDs `0..1023` and sequences `0..4095`.
- One generator instance must be thread-safe, duplicate-free, and strictly increasing at the successful CAS linearization point.
- Across correctly configured nodes, promise global uniqueness and time trend ordering only; never promise globally strict real-business ordering.
- Use one `AtomicLong` for timestamp-and-sequence state; do not add a global lock or static generator.
- Do not add any third-party ID algorithm dependency or Spring dependency to `egon-cola-component-common-id`.
- Keep `IdGenerator.nextId(): String`; add `LongIdGenerator.nextLongId(): long` and default string adaptation.
- Keep `UuidV7` and `UuidV7Generator` source-compatible, implement them with the JDK, and mark them `@Deprecated(forRemoval = true)`.
- Add exactly one Spring module: `egon-cola-component-common-id-starter`; keep all its tests in its own `src/test`.
- Use prefix `egon.cola.component.id`; `enabled=true`, explicit `machine-id`, and `max-clock-backward=5ms` defaults/requirements.
- Do not infer machine ID from host, network, process, random, or hash data.
- Do not modify existing Flyway migrations, UUID wire protocols, nonce/trace/event identities, or archetype UUIDv7 sharding contracts.
- Add accurate English Javadoc to every public class, interface, configuration property, and exception.
- Do not start an application, push, or create a pull request.

---

### Task 1: Add Long ID contracts and a fixed-layout parser

**Files:**
- Modify: `egon-cola-components/egon-cola-component-common/egon-cola-component-common-id/src/main/java/top/egon/cola/component/common/id/generator/IdGenerator.java`
- Create: `egon-cola-components/egon-cola-component-common/egon-cola-component-common-id/src/main/java/top/egon/cola/component/common/id/generator/LongIdGenerator.java`
- Create: `egon-cola-components/egon-cola-component-common/egon-cola-component-common-id/src/main/java/top/egon/cola/component/common/id/time/TimeSource.java`
- Create: `egon-cola-components/egon-cola-component-common/egon-cola-component-common-id/src/main/java/top/egon/cola/component/common/id/snowflake/SnowflakeIdLayout.java`
- Create: `egon-cola-components/egon-cola-component-common/egon-cola-component-common-id/src/main/java/top/egon/cola/component/common/id/snowflake/SnowflakeId.java`
- Create: `egon-cola-components/egon-cola-component-common/egon-cola-component-common-id/src/main/java/top/egon/cola/component/common/id/snowflake/SnowflakeIdParser.java`
- Test: `egon-cola-components/egon-cola-component-common/egon-cola-component-common-id/src/test/java/top/egon/cola/component/common/id/generator/LongIdGeneratorTest.java`
- Test: `egon-cola-components/egon-cola-component-common/egon-cola-component-common-id/src/test/java/top/egon/cola/component/common/id/snowflake/SnowflakeIdParserTest.java`

**Interfaces:**
- Consumes: Existing `IdGenerator.nextId(): String`.
- Produces: `LongIdGenerator.nextLongId(): long`, `TimeSource.currentTimeMillis(): long`, `SnowflakeIdParser.parse(long): SnowflakeId`, and the shared package-private fixed layout.

- [ ] **Step 1: Write failing contract and parser tests**

```java
@Test
void nextIdReturnsDecimalRepresentationOfLongId() {
    LongIdGenerator generator = () -> 9_223_372_036_854L;
    assertThat(generator.nextId()).isEqualTo("9223372036854");
}

@Test
void parseReturnsTimestampMachineAndSequence() {
    long id = (123L << 22) | (17L << 12) | 4095L;
    SnowflakeId parsed = SnowflakeIdParser.parse(id);
    assertThat(parsed.generatedAt()).isEqualTo(Instant.parse("2026-01-01T00:00:00.123Z"));
    assertThat(parsed.elapsedMillis()).isEqualTo(123L);
    assertThat(parsed.machineId()).isEqualTo(17);
    assertThat(parsed.sequence()).isEqualTo(4095);
}

@Test
void parseRejectsNegativeId() {
    assertThatIllegalArgumentException().isThrownBy(() -> SnowflakeIdParser.parse(-1L));
}
```

- [ ] **Step 2: Run the focused tests and verify RED**

Run:

```bash
./mvnw -B -ntp -pl egon-cola-components/egon-cola-component-common/egon-cola-component-common-id -am \
  -Dtest=LongIdGeneratorTest,SnowflakeIdParserTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: test compilation fails because `LongIdGenerator`, `SnowflakeId`, and `SnowflakeIdParser` do not exist.

- [ ] **Step 3: Implement the contracts and parser**

Implement `LongIdGenerator` exactly as:

```java
public interface LongIdGenerator extends IdGenerator {
    long nextLongId();

    @Override
    default String nextId() {
        return Long.toString(nextLongId());
    }
}
```

Implement `SnowflakeIdLayout` with `EPOCH_MILLIS`, shifts, masks, maximum values, `compose`, `packState`, and state-decode methods. Implement parser extraction with unsigned shifts and return:

```java
public record SnowflakeId(long id, Instant generatedAt, long elapsedMillis,
                          int machineId, int sequence) {
}
```

Add English Javadoc that defines the fixed Epoch and “single-instance strict / cross-node trend ordered” semantics without promising cross-node strict ordering.

- [ ] **Step 4: Run focused tests and the common-id suite for GREEN**

```bash
./mvnw -B -ntp -pl egon-cola-components/egon-cola-component-common/egon-cola-component-common-id -am test
```

Expected: all common-core and common-id tests pass.

- [ ] **Step 5: Commit Task 1**

```bash
git add egon-cola-components/egon-cola-component-common/egon-cola-component-common-id/src
git diff --cached --check
git commit -m "feat(id): add long ID contracts and parser"
```

---

### Task 2: Implement basic stateful Snowflake generation

**Files:**
- Create: `egon-cola-components/egon-cola-component-common/egon-cola-component-common-id/src/main/java/top/egon/cola/component/common/id/snowflake/SnowflakeIdGenerator.java`
- Test: `egon-cola-components/egon-cola-component-common/egon-cola-component-common-id/src/test/java/top/egon/cola/component/common/id/snowflake/SnowflakeIdGeneratorTest.java`

**Interfaces:**
- Consumes: Task 1 `LongIdGenerator`, `TimeSource`, `SnowflakeIdLayout`, and parser.
- Produces: stateful `SnowflakeIdGenerator` constructors and `nextLongId()` for normal time, same-millisecond sequence allocation, machine boundaries, and timestamp bounds.

- [ ] **Step 1: Write failing basic generation tests**

Add literal-behavior tests:

```java
@Test
void generatesStrictlyIncreasingIdsInOneInstance() {
    MutableTimeSource time = new MutableTimeSource(EPOCH_MILLIS + 100);
    SnowflakeIdGenerator generator = new SnowflakeIdGenerator(7, Duration.ofMillis(5), time);
    long first = generator.nextLongId();
    long second = generator.nextLongId();
    assertThat(second).isGreaterThan(first);
    assertThat(SnowflakeIdParser.parse(first).sequence()).isZero();
    assertThat(SnowflakeIdParser.parse(second).sequence()).isEqualTo(1);
}

@ParameterizedTest
@ValueSource(longs = {0L, 1023L})
void acceptsMachineIdBoundaries(long machineId) {
    SnowflakeIdGenerator generator = new SnowflakeIdGenerator(machineId, Duration.ofMillis(5),
            () -> EPOCH_MILLIS + 1);
    assertThat(SnowflakeIdParser.parse(generator.nextLongId()).machineId()).isEqualTo((int) machineId);
}

@ParameterizedTest
@ValueSource(longs = {-1L, 1024L})
void rejectsMachineIdOutsideTenBits(long machineId) {
    assertThatIllegalArgumentException().isThrownBy(() -> new SnowflakeIdGenerator(machineId));
}
```

Also test different machines in the same millisecond, positive ID at exact Epoch, final 41-bit millisecond acceptance, and pre-Epoch/overflow rejection.

- [ ] **Step 2: Run `SnowflakeIdGeneratorTest` and verify RED**

```bash
./mvnw -B -ntp -pl egon-cola-components/egon-cola-component-common/egon-cola-component-common-id -am \
  -Dtest=SnowflakeIdGeneratorTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: test compilation fails because `SnowflakeIdGenerator` does not exist.

- [ ] **Step 3: Implement normal-time CAS generation**

Use these state fields, not separate atomics:

```java
private static final long UNINITIALIZED_STATE = -1L;
private final int machineId;
private final TimeSource timeSource;
private final AtomicLong state = new AtomicLong(UNINITIALIZED_STATE);
```

Each loop reads time, validates elapsed range, decodes one previous state, chooses sequence 0 or previous+1, and performs `state.compareAndSet(previous, candidate)`. Return the composed ID only after successful CAS. At exact Epoch/machine 0, skip the all-zero ID by starting at sequence 1.

Define public `DEFAULT_MAX_CLOCK_BACKWARD = Duration.ofMillis(5)` and the three approved constructors. Validate machine ID, non-null time source, and non-negative/finite-nanosecond duration.

- [ ] **Step 4: Run generator and parser suites for GREEN**

```bash
./mvnw -B -ntp -pl egon-cola-components/egon-cola-component-common/egon-cola-component-common-id -am test
```

Expected: all current tests pass; sequence-overflow and clock-recovery behavior remain for Task 3.

- [ ] **Step 5: Commit Task 2**

```bash
git add egon-cola-components/egon-cola-component-common/egon-cola-component-common-id/src
git diff --cached --check
git commit -m "feat(id): implement CAS Snowflake generator"
```

---

### Task 3: Add sequence exhaustion, rollback, interruption, and concurrency guarantees

**Files:**
- Modify: `egon-cola-components/egon-cola-component-common/egon-cola-component-common-id/src/main/java/top/egon/cola/component/common/id/snowflake/SnowflakeIdGenerator.java`
- Create: `egon-cola-components/egon-cola-component-common/egon-cola-component-common-id/src/main/java/top/egon/cola/component/common/id/exception/ClockMovedBackwardException.java`
- Create: `egon-cola-components/egon-cola-component-common/egon-cola-component-common-id/src/main/java/top/egon/cola/component/common/id/exception/IdGenerationInterruptedException.java`
- Create: `egon-cola-components/egon-cola-component-common/egon-cola-component-common-id/src/main/java/top/egon/cola/component/common/id/exception/SnowflakeTimestampOutOfRangeException.java`
- Modify: `egon-cola-components/egon-cola-component-common/egon-cola-component-common-id/src/test/java/top/egon/cola/component/common/id/snowflake/SnowflakeIdGeneratorTest.java`

**Interfaces:**
- Consumes: Task 2 generator CAS loop.
- Produces: bounded rollback recovery, immediate large-rollback failure, sequence rollover waiting, interruption semantics, timestamp exception, and deterministic stress proof.

- [ ] **Step 1: Write failing temporal and concurrency tests**

Add deterministic cases with scripted/step time sources:

```java
@Test
void waitsForSmallRollbackToRecover() {
    ScriptedTimeSource time = new ScriptedTimeSource(T, T - 3, T - 2, T);
    SnowflakeIdGenerator generator = new SnowflakeIdGenerator(3, Duration.ofMillis(5), time);
    long first = generator.nextLongId();
    long second = generator.nextLongId();
    assertThat(second).isGreaterThan(first);
}

@Test
void failsImmediatelyForLargeRollback() {
    ScriptedTimeSource time = new ScriptedTimeSource(T, T - 6);
    SnowflakeIdGenerator generator = new SnowflakeIdGenerator(9, Duration.ofMillis(5), time);
    generator.nextLongId();
    assertThatThrownBy(generator::nextLongId)
            .isInstanceOfSatisfying(ClockMovedBackwardException.class, exception -> {
                assertThat(exception.currentTimeMillis()).isEqualTo(T - 6);
                assertThat(exception.lastTimeMillis()).isEqualTo(T);
                assertThat(exception.backwardMillis()).isEqualTo(6);
                assertThat(exception.machineId()).isEqualTo(9);
            });
}
```

Add tests for all 4096 sequences then next-millisecond rollover, stalled small rollback timeout, interrupt preservation, 100,000 deterministic IDs without duplicates, and 16-thread deterministic generation whose sorted results are pairwise strictly increasing.

- [ ] **Step 2: Run generator tests and verify RED**

```bash
./mvnw -B -ntp -pl egon-cola-components/egon-cola-component-common/egon-cola-component-common-id -am \
  -Dtest=SnowflakeIdGeneratorTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: rollback, rollover, interruption, and exception assertions fail because Task 2 does not implement them.

- [ ] **Step 3: Implement wait and exception paths**

For rollback, compare absolute milliseconds with the state timestamp. If delta exceeds the configured maximum, throw immediately. Otherwise, use a `System.nanoTime()` deadline plus short `LockSupport.parkNanos` intervals; reread `TimeSource` until it reaches the last timestamp, then restart the CAS loop. A deadline miss throws `ClockMovedBackwardException` rather than advancing logical time.

For sequence 4095, park in short intervals until a later source millisecond appears, then restart CAS. Both wait paths check interruption before and after parking, preserve the interrupt flag, and throw `IdGenerationInterruptedException` before state mutation.

`ClockMovedBackwardException` exposes `currentTimeMillis()`, `lastTimeMillis()`, `backwardMillis()`, and `machineId()`. `SnowflakeTimestampOutOfRangeException` includes current time and the fixed supported range.

- [ ] **Step 4: Run all common-id tests repeatedly for GREEN and stability**

```bash
./mvnw -B -ntp -pl egon-cola-components/egon-cola-component-common/egon-cola-component-common-id -am test
./mvnw -B -ntp -pl egon-cola-components/egon-cola-component-common/egon-cola-component-common-id \
  -Dtest=SnowflakeIdGeneratorTest -Dsurefire.rerunFailingTestsCount=0 test
```

Expected: every deterministic temporal and stress test passes twice with no sleep-based flakiness.

- [ ] **Step 5: Commit Task 3**

```bash
git add egon-cola-components/egon-cola-component-common/egon-cola-component-common-id/src
git diff --cached --check
git commit -m "feat(id): harden Snowflake time handling"
```

---

### Task 4: Replace uuid-creator with deprecated pure-JDK UUIDv7 compatibility

**Files:**
- Modify: `egon-cola-components/egon-cola-component-common/egon-cola-component-common-id/pom.xml`
- Modify: `egon-cola-components/egon-cola-component-common/egon-cola-component-common-id/src/main/java/top/egon/cola/component/common/id/uuid/UuidV7.java`
- Modify: `egon-cola-components/egon-cola-component-common/egon-cola-component-common-id/src/main/java/top/egon/cola/component/common/id/generator/UuidV7Generator.java`
- Modify: `egon-cola-components/egon-cola-component-common/egon-cola-component-common-id/src/test/java/top/egon/cola/component/common/id/uuid/UuidV7Test.java`

**Interfaces:**
- Consumes: Existing UUIDv7 signatures and Task 2 Snowflake migration target.
- Produces: source-compatible JDK UUIDv7 methods with no runtime dependency and clear removal guidance.

- [ ] **Step 1: Strengthen UUID compatibility tests before changing implementation**

Add assertions that catch a wrong version, variant, timestamp field, string length, or hyphen removal:

```java
@Test
void uuidV7UsesRfcVersionVariantAndCurrentTimestamp() {
    long before = System.currentTimeMillis();
    UUID uuid = UuidV7.generate();
    long after = System.currentTimeMillis();
    long encodedMillis = uuid.getMostSignificantBits() >>> 16;
    assertThat(uuid.version()).isEqualTo(7);
    assertThat(uuid.variant()).isEqualTo(2);
    assertThat(encodedMillis).isBetween(before, after);
}
```

- [ ] **Step 2: Temporarily remove uuid-creator and verify RED**

Remove the dependency from the module POM only, then run:

```bash
./mvnw -B -ntp -pl egon-cola-components/egon-cola-component-common/egon-cola-component-common-id -am clean test
```

Expected: production compilation fails at the existing `UuidCreator` import, proving the module depended on the removed library.

- [ ] **Step 3: Implement JDK UUIDv7 and deprecate compatibility APIs**

Use one thread-safe `SecureRandom`. Compose the UUID as:

```java
long mostSignificantBits = (unixMillis << 16) | 0x7000L | randomA12;
long leastSignificantBits = 0x8000_0000_0000_0000L | randomB62;
return new UUID(mostSignificantBits, leastSignificantBits);
```

Validate the 48-bit Unix millisecond range. Preserve `generate`, `string`, `simpleString`, and `UuidV7Generator.nextId`. Mark both public types with `@Deprecated(since = "5.3.1", forRemoval = true)` and English Javadoc pointing database-primary-key users to Snowflake.

Remove the unused `common-core` dependency too, making common-id a zero-runtime-dependency JAR.

- [ ] **Step 4: Run clean tests and dependency tree for GREEN**

```bash
./mvnw -B -ntp -pl egon-cola-components/egon-cola-component-common/egon-cola-component-common-id -am clean test
./mvnw -B -ntp -pl egon-cola-components/egon-cola-component-common/egon-cola-component-common-id dependency:tree
```

Expected: tests pass and the common-id dependency tree contains no `uuid-creator` or `common-core` runtime dependency.

- [ ] **Step 5: Commit Task 4**

```bash
git add egon-cola-components/egon-cola-component-common/egon-cola-component-common-id
git diff --cached --check
git commit -m "refactor(id): replace uuid-creator with JDK UUIDv7"
```

---

### Task 5: Add the single common ID Starter with module-local tests

**Files:**
- Modify: `egon-cola-components/pom.xml`
- Modify: `egon-cola-components/egon-cola-components-bom/pom.xml`
- Create: `egon-cola-components/egon-cola-component-common-id-starter/pom.xml`
- Create: `egon-cola-components/egon-cola-component-common-id-starter/src/main/java/top/egon/cola/component/common/id/autoconfigure/IdGeneratorProperties.java`
- Create: `egon-cola-components/egon-cola-component-common-id-starter/src/main/java/top/egon/cola/component/common/id/autoconfigure/IdGeneratorPropertiesValidator.java`
- Create: `egon-cola-components/egon-cola-component-common-id-starter/src/main/java/top/egon/cola/component/common/id/autoconfigure/IdGeneratorAutoConfiguration.java`
- Create: `egon-cola-components/egon-cola-component-common-id-starter/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
- Test: `egon-cola-components/egon-cola-component-common-id-starter/src/test/java/top/egon/cola/component/common/id/autoconfigure/IdGeneratorAutoConfigurationTest.java`

**Interfaces:**
- Consumes: Task 2/3 `SnowflakeIdGenerator`, `LongIdGenerator`, and existing `IdGenerator`.
- Produces: Boot 3 conditional auto-configuration under `egon.cola.component.id` and a BOM-managed consumer artifact.

- [ ] **Step 1: Add module scaffolding and failing ApplicationContextRunner tests**

Add only `egon-cola-component-common-id-starter` to the components reactor. Its POM directly inherits `egon-cola-components-parent`, depends on common-id `${project.version}`, Spring Boot starter/autoconfigure, optional configuration processor, and test-scoped starter-test.

Write tests such as:

```java
private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(IdGeneratorAutoConfiguration.class));

@Test
void createsOneGeneratorForValidConfiguration() {
    contextRunner.withPropertyValues("egon.cola.component.id.machine-id=17")
            .run(context -> assertThat(context)
                    .hasSingleBean(SnowflakeIdGenerator.class)
                    .hasSingleBean(LongIdGenerator.class)
                    .hasSingleBean(IdGenerator.class));
}

@Test
void failsFastWhenMachineIdIsMissing() {
    contextRunner.run(context -> {
        assertThat(context).hasFailed();
        assertThat(context.getStartupFailure())
                .hasRootCauseMessage("egon.cola.component.id.machine-id must be configured when enabled=true");
    });
}
```

Also cover machine `0/1023`, `-1/1024`, `enabled=false`, custom `IdGenerator`, custom `LongIdGenerator`, negative rollback duration, and default/bound `Duration` behavior.

- [ ] **Step 2: Run the Starter test and verify RED**

```bash
./mvnw -B -ntp -pl egon-cola-components/egon-cola-component-common-id-starter -am test
```

Expected: test compilation fails because properties and auto-configuration classes do not exist.

- [ ] **Step 3: Implement properties, validator, auto-configuration, and imports**

Properties fields:

```java
private boolean enabled = true;
private Long machineId;
private Duration maxClockBackward = SnowflakeIdGenerator.DEFAULT_MAX_CLOCK_BACKWARD;
```

Validator messages must contain full property names and ranges. Auto-configuration:

```java
@AutoConfiguration
@EnableConfigurationProperties(IdGeneratorProperties.class)
@ConditionalOnProperty(prefix = IdGeneratorProperties.PREFIX, name = "enabled",
        havingValue = "true", matchIfMissing = true)
public class IdGeneratorAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(IdGenerator.class)
    public SnowflakeIdGenerator snowflakeIdGenerator(IdGeneratorProperties properties) {
        IdGeneratorPropertiesValidator.validate(properties);
        return new SnowflakeIdGenerator(properties.getMachineId(), properties.getMaxClockBackward());
    }
}
```

Register exactly that class in `AutoConfiguration.imports`; do not use component scanning.

- [ ] **Step 4: Run Starter and components metadata tests for GREEN**

```bash
./mvnw -B -ntp -pl egon-cola-components/egon-cola-component-common-id-starter -am clean test
./mvnw -B -ntp -f egon-cola-components/pom.xml validate
```

Expected: all auto-configuration cases pass and the new reactor/BOM coordinates resolve.

- [ ] **Step 5: Commit Task 5**

```bash
git add egon-cola-components/pom.xml \
  egon-cola-components/egon-cola-components-bom/pom.xml \
  egon-cola-components/egon-cola-component-common-id-starter
git diff --cached --check
git commit -m "feat(id): add common ID Spring Boot starter"
```

---

### Task 6: Update bilingual documentation and migration boundaries

**Files:**
- Modify: `README.md`
- Modify: `README.zh-CN.md`
- Modify: `egon-cola-components/egon-cola-component-common/README.md`
- Modify: `egon-cola-components/egon-cola-component-common/README.zh-CN.md`
- Modify: `egon-cola-components/egon-cola-components-bom/README.md`
- Modify: `egon-cola-components/egon-cola-components-bom/README.zh-CN.md`
- Create: `egon-cola-components/egon-cola-component-common-id-starter/README.md`
- Create: `egon-cola-components/egon-cola-component-common-id-starter/README.zh-CN.md`
- Modify only if it enumerates consumer modules: `egon-cola-components/egon-cola-components-architecture.md`

**Interfaces:**
- Consumes: Final core and Starter API/property names.
- Produces: complete English/Chinese installation, operational, migration, Kubernetes, database, and capability-boundary documentation.

- [ ] **Step 1: Replace active UUID-only module descriptions**

Update module tables and examples so common-id is described as Snowflake plus deprecated UUIDv7 compatibility. Add the Starter artifact to root/BOM consumer tables. Do not rewrite historical `docs/superpowers` plans or migrate DDC/Gateway/RPC/archetype UUID protocols.

- [ ] **Step 2: Write complete Starter README pair**

Both files must include the exact Maven coordinate and YAML keys, Spring injection and direct-Java examples, SQL `BIGINT` example, bit table, fixed Epoch, capacity, rollback rules, machine assignment, StatefulSet ordinal mapping, ordinary Deployment warning, NTP requirement, duplicate machine-ID warning, restart/rollback limitation, UUIDv7 deprecation migration, JavaScript-number warning, and no-global-strict-order statement.

Use this StatefulSet shape without claiming automatic allocation inside the library:

```yaml
env:
  - name: POD_NAME
    valueFrom:
      fieldRef:
        fieldPath: metadata.name
# An entrypoint or deployment system must map the stable ordinal to:
# EGON_COLA_COMPONENT_ID_MACHINE_ID=0..1023
```

- [ ] **Step 3: Verify documentation consistency and repository references**

```bash
rg -n "uuid-creator|common-id only provides UUIDv7|只提供 UUIDv7" \
  README.md README.zh-CN.md egon-cola-components --glob '!**/target/**'
rg -n "egon-cola-component-common-id-starter|egon.cola.component.id.machine-id" \
  README.md README.zh-CN.md egon-cola-components --glob '!**/target/**'
git diff --check
```

Expected: no active dependency or UUID-only claim remains; Starter coordinates/configuration appear in both languages; any `uuid-creator` match is absent.

- [ ] **Step 4: Commit Task 6**

```bash
git add README.md README.zh-CN.md \
  egon-cola-components/egon-cola-component-common/README.md \
  egon-cola-components/egon-cola-component-common/README.zh-CN.md \
  egon-cola-components/egon-cola-components-bom/README.md \
  egon-cola-components/egon-cola-components-bom/README.zh-CN.md \
  egon-cola-components/egon-cola-component-common-id-starter/README.md \
  egon-cola-components/egon-cola-component-common-id-starter/README.zh-CN.md \
  egon-cola-components/egon-cola-components-architecture.md
git diff --cached --check
git commit -m "docs(id): document Snowflake ID integration"
```

---

### Task 7: Run release-shape verification and perform final code review

**Files:**
- Modify only files from Tasks 1-6 when verification or review proves a defect.

**Interfaces:**
- Consumes: All implemented code, tests, Maven metadata, BOM, and documentation.
- Produces: fresh evidence for concurrency correctness, dependency removal, release completeness, and repository integration.

- [ ] **Step 1: Run focused clean tests**

```bash
./mvnw -B -ntp -pl \
  egon-cola-components/egon-cola-component-common/egon-cola-component-common-id,\
egon-cola-components/egon-cola-component-common-id-starter -am clean test
```

- [ ] **Step 2: Run all common component tests**

```bash
./mvnw -B -ntp -f egon-cola-components/egon-cola-component-common/pom.xml clean test
```

- [ ] **Step 3: Prove dependency and BOM boundaries**

```bash
./mvnw -B -ntp -pl egon-cola-components/egon-cola-component-common-id-starter -am dependency:tree
./mvnw -B -ntp -f egon-cola-components/egon-cola-components-bom/pom.xml validate
rg -n "com\.github\.f4b6a3|uuid-creator" --glob 'pom.xml' --glob '!**/target/**' .
```

Expected: Starter depends on common-id and Boot only as designed; no POM references `uuid-creator`; BOM validates.

- [ ] **Step 4: Verify Javadoc/source/Central release shape without publishing**

```bash
./mvnw -B -ntp -f egon-cola-components/pom.xml \
  -Prelease -Dgpg.skip=true -DskipTests verify
```

Expected: source and Javadoc artifacts build for common-id and Starter, and Central Publishing configuration resolves without deployment.

- [ ] **Step 5: Run components and root integration verification**

```bash
./mvnw -B -ntp -f egon-cola-components/pom.xml test
./mvnw -B -ntp clean integration-test
```

Do not interpret these commands as proof of real multi-JVM clocks, Kubernetes allocation, NTP, or external topology.

- [ ] **Step 6: Review concurrency and capability claims**

Review the final diff against these concrete mutations:

- Split timestamp and sequence into separate atomic operations: concurrency tests must fail.
- Allow sequence 4096 to wrap in the same millisecond: rollover test must fail.
- Continue with logical time after a large rollback: rollback test must fail.
- Remove `@ConditionalOnMissingBean(IdGenerator.class)`: custom-bean tests must fail.
- Default machine ID to zero: missing-property context test must fail.
- Claim cross-node strict ordering: README boundary review must reject the text.

Request a code-reviewer subagent against the design and commit range; independently inspect every Important/Critical finding before accepting it.

- [ ] **Step 7: Apply verified fixes and create one final fix commit if needed**

```bash
git add egon-cola-components/egon-cola-component-common/egon-cola-component-common-id \
  egon-cola-components/egon-cola-component-common-id-starter \
  egon-cola-components/pom.xml \
  egon-cola-components/egon-cola-components-bom/pom.xml \
  README.md README.zh-CN.md \
  egon-cola-components/egon-cola-component-common/README.md \
  egon-cola-components/egon-cola-component-common/README.zh-CN.md \
  egon-cola-components/egon-cola-components-bom/README.md \
  egon-cola-components/egon-cola-components-bom/README.zh-CN.md \
  egon-cola-components/egon-cola-components-architecture.md
git diff --cached --check
git commit -m "fix(id): address Snowflake ID review findings"
```

Skip this commit when no file changes are required.

- [ ] **Step 8: Final clean-state and scope check**

```bash
git status --short
git log --oneline --decorate 85f93a95..HEAD
git diff --check 85f93a95..HEAD
```

Expected: worktree clean; commits contain only the design, common-id, single Starter, reactor/BOM, tests, and documentation requested by the user.
