# Snowflake ID Test and Benchmark Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add deterministic Snowflake boundary tests, platform/virtual-thread concurrency tests, and an independently runnable JMH capacity benchmark to the existing common ID Starter.

**Architecture:** Keep production code unchanged. Place a thread-safe fake clock and JUnit Jupiter tests in `src/test`, place JMH code in a profile-enabled `src/jmh` source set, and package an attached executable benchmark jar without affecting the default Maven lifecycle.

**Tech Stack:** JDK 21, Spring Boot Starter Test/JUnit Jupiter, Java virtual threads, JMH 1.37, Maven Compiler/Build Helper/Shade plugins.

## Global Constraints

- Keep all changes inside `egon-cola-component-common-id-starter` plus this plan.
- Do not change `SnowflakeIdGenerator`, its public API, the `1/41/10/12` layout, or the fixed Epoch.
- Use one shared generator instance in every concurrent test and every JMH platform-thread method.
- Verify exact sequences `0..4095` in a fixed millisecond and wait for the 4097th call.
- Protect every potentially blocking JUnit operation with a timeout.
- Use `Executors.newVirtualThreadPerTaskExecutor()` for virtual-thread correctness and throughput scenarios.
- Do not put fixed throughput, latency, thread-count capacity, or GC thresholds in JUnit assertions.
- Treat `4,096,000 IDs/s` as the algorithmic ceiling per machine ID, not a promised measured throughput.
- Preserve all unrelated staged and unstaged workspace changes.
- Do not start an application, push, or create a pull request.

---

### Task 1: Deterministic clock, sequence, waiting, interruption, and boundary tests

**Files:**
- Create: `egon-cola-components/egon-cola-component-common/egon-cola-component-common-id-starter/src/test/java/top/egon/cola/component/common/id/snowflake/ControllableTimeSource.java`
- Modify: `egon-cola-components/egon-cola-component-common/egon-cola-component-common-id-starter/src/test/java/top/egon/cola/component/common/id/snowflake/SnowflakeIdGeneratorTest.java`

**Interfaces:**
- Consumes: `TimeSource.currentTimeMillis()`, `SnowflakeIdGenerator.nextLongId()`, and `SnowflakeIdParser.parse(long)`.
- Produces: package-private `ControllableTimeSource(long)`, `setCurrentTimeMillis(long)`, `advanceMillis(long)`, `readCount()`, and deterministic generator contract tests.

- [x] **Step 1: Write tests against the missing controllable clock**

Add a class-level JUnit timeout and use the desired clock API in tests such as:

```java
@Timeout(value = 20, unit = TimeUnit.SECONDS)
class SnowflakeIdGeneratorTest {

    @Test
    void generatesAllSequencesThenWaitsForNextMillisecond() {
        ControllableTimeSource timeSource = new ControllableTimeSource(TEST_TIME);
        SnowflakeIdGenerator generator = generator(5, timeSource);
        List<Long> ids = LongStream.range(0, 4_096)
                .map(ignored -> generator.nextLongId())
                .boxed()
                .toList();

        for (int sequence = 0; sequence < ids.size(); sequence++) {
            SnowflakeId parsed = SnowflakeIdParser.parse(ids.get(sequence));
            assertEquals(sequence, parsed.sequence());
            assertTrue(ids.get(sequence) > 0L);
            if (sequence > 0) {
                assertTrue(ids.get(sequence) > ids.get(sequence - 1));
            }
        }

        assertEquals(4_096, new HashSet<>(ids).size());
    }
}
```

Add separate tests for the 4097th virtual-thread wait/resume, interrupted wait, small rollback recovery,
large rollback diagnostics, both timestamp range failures, different machine-ID disjointness, and same
machine-ID independent-instance duplication.

- [x] **Step 2: Run focused test compilation and verify RED**

Run:

```bash
mvn -f egon-cola-components/egon-cola-component-common/egon-cola-component-common-id-starter/pom.xml \
  -Dtest=SnowflakeIdGeneratorTest test
```

Expected: test compilation fails because `ControllableTimeSource` does not exist.

- [x] **Step 3: Implement the minimal thread-safe test clock**

Implement exactly one test utility with atomic time and read count:

```java
final class ControllableTimeSource implements TimeSource {
    private final AtomicLong currentTimeMillis;
    private final AtomicLong readCount = new AtomicLong();

    ControllableTimeSource(long currentTimeMillis) {
        this.currentTimeMillis = new AtomicLong(currentTimeMillis);
    }

    @Override
    public long currentTimeMillis() {
        long observedTimeMillis = currentTimeMillis.get();
        readCount.incrementAndGet();
        return observedTimeMillis;
    }

    void setCurrentTimeMillis(long currentTimeMillis) {
        this.currentTimeMillis.set(currentTimeMillis);
    }

    long advanceMillis(long millis) {
        if (millis < 0L) {
            throw new IllegalArgumentException("millis must not be negative: " + millis);
        }
        return currentTimeMillis.addAndGet(millis);
    }

    long readCount() {
        return readCount.get();
    }
}
```

For blocking tests, use `assertTimeoutPreemptively`, virtual `Thread`, bounded `join`, and a bounded
`awaitReadCountAtLeast` helper. Require two reads beyond the pre-call baseline: the first is the outer
generation read and the second proves the generator has reread the clock inside its wait loop. Always
terminate or interrupt a surviving thread in `finally`.

- [x] **Step 4: Run focused and full Starter tests for GREEN**

Run:

```bash
mvn -f egon-cola-components/egon-cola-component-common/egon-cola-component-common-id-starter/pom.xml \
  -Dtest=SnowflakeIdGeneratorTest test
mvn -f egon-cola-components/egon-cola-component-common/egon-cola-component-common-id-starter/pom.xml test
```

Expected: all Snowflake tests and all Starter tests pass without a hung thread.

- [x] **Step 5: Commit Task 1 only**

```bash
git add \
  egon-cola-components/egon-cola-component-common/egon-cola-component-common-id-starter/src/test/java/top/egon/cola/component/common/id/snowflake/ControllableTimeSource.java \
  egon-cola-components/egon-cola-component-common/egon-cola-component-common-id-starter/src/test/java/top/egon/cola/component/common/id/snowflake/SnowflakeIdGeneratorTest.java
git diff --cached --check -- \
  egon-cola-components/egon-cola-component-common/egon-cola-component-common-id-starter/src/test
git commit --only \
  egon-cola-components/egon-cola-component-common/egon-cola-component-common-id-starter/src/test/java/top/egon/cola/component/common/id/snowflake/ControllableTimeSource.java \
  egon-cola-components/egon-cola-component-common/egon-cola-component-common-id-starter/src/test/java/top/egon/cola/component/common/id/snowflake/SnowflakeIdGeneratorTest.java \
  -m "test(id): cover Snowflake sequence boundaries"
```

---

### Task 2: Platform and large virtual-thread correctness tests

**Files:**
- Create: `egon-cola-components/egon-cola-component-common/egon-cola-component-common-id-starter/src/test/java/top/egon/cola/component/common/id/snowflake/SnowflakeIdGeneratorConcurrencyTest.java`

**Interfaces:**
- Consumes: the production system-clock constructor and Java 21 executor factories.
- Produces: bounded correctness coverage for shared-generator platform and virtual-thread callers.

- [x] **Step 1: Add shared-generator concurrency tests**

Use a common helper that submits tasks, starts them through a latch when appropriate, obtains each Future
with a timeout, and inserts every returned primitive ID into `ConcurrentHashMap.newKeySet()`.

```java
@Test
void platformThreadsGenerateOnlyPositiveUniqueIds() throws Exception {
    assertConcurrentGeneration(Executors.newFixedThreadPool(32), 32, 8_192);
}

@Test
void manyVirtualThreadsGenerateOnlyPositiveUniqueIds() throws Exception {
    assertConcurrentGeneration(Executors.newVirtualThreadPerTaskExecutor(), 20_000, 16);
}
```

The helper must assert the literal expected count, reject non-positive IDs, reject duplicate insertions, get
all futures with bounded waits, and call `shutdownNow()` plus bounded `awaitTermination()` in `finally`.

- [x] **Step 2: Run the new concurrency class**

```bash
mvn -f egon-cola-components/egon-cola-component-common/egon-cola-component-common-id-starter/pom.xml \
  -Dtest=SnowflakeIdGeneratorConcurrencyTest test
```

Expected: both platform-thread and 20,000-virtual-thread correctness cases complete within the class-level
timeout with no duplicate or non-positive IDs.

- [x] **Step 3: Run the complete Starter suite**

```bash
mvn -f egon-cola-components/egon-cola-component-common/egon-cola-component-common-id-starter/pom.xml test
```

Expected: all existing and new tests pass.

- [x] **Step 4: Commit Task 2 only**

```bash
git add egon-cola-components/egon-cola-component-common/egon-cola-component-common-id-starter/src/test/java/top/egon/cola/component/common/id/snowflake/SnowflakeIdGeneratorConcurrencyTest.java
git diff --cached --check -- egon-cola-components/egon-cola-component-common/egon-cola-component-common-id-starter/src/test
git commit --only egon-cola-components/egon-cola-component-common/egon-cola-component-common-id-starter/src/test/java/top/egon/cola/component/common/id/snowflake/SnowflakeIdGeneratorConcurrencyTest.java \
  -m "test(id): verify shared generator concurrency"
```

---

### Task 3: Independent JMH capacity benchmark and runbook

**Files:**
- Modify: `egon-cola-components/egon-cola-component-common/egon-cola-component-common-id-starter/pom.xml`
- Create: `egon-cola-components/egon-cola-component-common/egon-cola-component-common-id-starter/src/jmh/java/top/egon/cola/component/common/id/benchmark/SnowflakeIdGeneratorBenchmark.java`
- Create: `egon-cola-components/egon-cola-component-common/egon-cola-component-common-id-starter/src/test/README.md`
- Modify: `docs/superpowers/plans/2026-07-30-snowflake-id-test-benchmark.md`

**Interfaces:**
- Consumes: public `SnowflakeIdGenerator(long)` and JMH annotations/runner.
- Produces: an optional `jmh` Maven Profile, an attached `benchmarks` executable jar, platform-thread
  throughput/latency methods, virtual-thread batch scaling, and reproducible commands.

- [x] **Step 1: Add benchmark source before the JMH profile**

Create a `@State(Scope.Benchmark)` class with one shared generator, common JMH settings, and six direct
methods:

```java
@Benchmark
@BenchmarkMode(Mode.Throughput)
@Threads(1)
public long throughputThreads1() {
    return generator.nextLongId();
}

@Benchmark
@BenchmarkMode(Mode.AverageTime)
@Threads(32)
public long averageTimeThreads32() {
    return generator.nextLongId();
}
```

Repeat the direct call for 2, 4, 8, and 16 threads. Apply both `Mode.Throughput` and `Mode.AverageTime`.

Add a nested `@State(Scope.Benchmark)` containing one shared generator and one
`Executors.newVirtualThreadPerTaskExecutor()`. Use fixed-size methods so JMH can normalize every result:

```java
@Benchmark
@OperationsPerInvocation(65_536)
public long virtualThreadBatch65536(VirtualThreadState state) throws Exception {
    return runVirtualThreadBatch(state, 65_536);
}
```

Provide fixed methods for 32 through 1,048,576 tasks. Each submits its literal task count against one
generator, waits for every Future, and returns a primitive checksum. `@OperationsPerInvocation` makes
throughput directly report IDs/s and `gc.alloc.rate.norm` directly report B/ID.

- [x] **Step 2: Verify benchmark compilation fails before Maven wiring**

```bash
mvn -f egon-cola-components/egon-cola-component-common/egon-cola-component-common-id-starter/pom.xml \
  -Pjmh -DskipTests package
```

Expected: the benchmark source is not compiled and no `benchmarks` classifier jar is produced because the
profile does not yet exist.

- [x] **Step 3: Add the optional JMH Maven profile**

Add `jmh.version=1.37`, profile-scoped `jmh-core` and provided `jmh-generator-annprocess`,
`build-helper-maven-plugin` test-source registration, test annotation processor path, a benchmark-only
test jar, and `maven-shade-plugin` with `shadedArtifactAttached=true`, classifier `benchmarks`, and
`org.openjdk.jmh.Main` manifest transformer. Keep benchmark classes out of the ordinary Starter jar.

- [x] **Step 4: Add exact run commands and interpretation notes**

Document:

```bash
mvn -f egon-cola-components/egon-cola-component-common/egon-cola-component-common-id-starter/pom.xml test
mvn -f egon-cola-components/egon-cola-component-common/egon-cola-component-common-id-starter/pom.xml -Pjmh -DskipTests package
java -jar egon-cola-components/egon-cola-component-common/egon-cola-component-common-id-starter/target/egon-cola-component-common-id-starter-5.3.2-benchmarks.jar -prof gc
```

Also document quick smoke overrides `-wi 0 -i 1 -r 200ms -f 1`, virtual-only filtering, the per-machine
algorithmic ceiling, and that no tested virtual-thread count is an absolute JVM maximum.

- [x] **Step 5: Build and smoke-test JMH**

```bash
mvn -f egon-cola-components/egon-cola-component-common/egon-cola-component-common-id-starter/pom.xml \
  -Pjmh -DskipTests package
java -jar egon-cola-components/egon-cola-component-common/egon-cola-component-common-id-starter/target/egon-cola-component-common-id-starter-5.3.2-benchmarks.jar \
  '.*(throughputThreads(1|32)|averageTimeThreads(1|32))$' -wi 0 -i 1 -r 200ms -f 1 -prof gc
java -jar egon-cola-components/egon-cola-component-common/egon-cola-component-common-id-starter/target/egon-cola-component-common-id-starter-5.3.2-benchmarks.jar \
  '.*virtualThreadBatch(32|512|2048|8192|32768|65536)$' -wi 0 -i 1 -r 200ms -f 1 -prof gc
```

Expected: the jar starts JMH; platform methods produce throughput, average-time and GC rows; all feasible
virtual-thread sizes finish, raw throughput is IDs/s, and infeasible sizes are reported with their actual failure.

- [x] **Step 6: Run final verification and commit Task 3**

```bash
mvn -f egon-cola-components/egon-cola-component-common/egon-cola-component-common-id-starter/pom.xml clean test
mvn -f egon-cola-components/egon-cola-component-common/egon-cola-component-common-id-starter/pom.xml -Pjmh -DskipTests package
git diff --check -- \
  docs/superpowers/plans/2026-07-30-snowflake-id-test-benchmark.md \
  egon-cola-components/egon-cola-component-common/egon-cola-component-common-id-starter
git add \
  docs/superpowers/plans/2026-07-30-snowflake-id-test-benchmark.md \
  egon-cola-components/egon-cola-component-common/egon-cola-component-common-id-starter/pom.xml \
  egon-cola-components/egon-cola-component-common/egon-cola-component-common-id-starter/src/jmh \
  egon-cola-components/egon-cola-component-common/egon-cola-component-common-id-starter/src/test
git commit --only \
  docs/superpowers/plans/2026-07-30-snowflake-id-test-benchmark.md \
  egon-cola-components/egon-cola-component-common/egon-cola-component-common-id-starter/pom.xml \
  egon-cola-components/egon-cola-component-common/egon-cola-component-common-id-starter/src/jmh \
  egon-cola-components/egon-cola-component-common/egon-cola-component-common-id-starter/src/test \
  -m "perf(id): add Snowflake JMH capacity benchmark"
```
