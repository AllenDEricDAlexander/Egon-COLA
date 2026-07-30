package top.egon.cola.component.common.id.benchmark;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OperationsPerInvocation;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;
import top.egon.cola.component.common.id.snowflake.SnowflakeIdGenerator;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Shared-instance Snowflake benchmarks for platform-thread CAS contention and
 * end-to-end virtual-thread throughput. Run with {@code -prof gc} to include
 * allocation and collection statistics.
 */
@State(Scope.Benchmark)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(2)
public class SnowflakeIdGeneratorBenchmark {

    private SnowflakeIdGenerator generator;

    @Setup(Level.Trial)
    public void setUp() {
        generator = new SnowflakeIdGenerator(42);
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    @Threads(1)
    public long throughputThreads1() {
        return generator.nextLongId();
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    @Threads(2)
    public long throughputThreads2() {
        return generator.nextLongId();
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    @Threads(4)
    public long throughputThreads4() {
        return generator.nextLongId();
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    @Threads(8)
    public long throughputThreads8() {
        return generator.nextLongId();
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    @Threads(16)
    public long throughputThreads16() {
        return generator.nextLongId();
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    @Threads(32)
    public long throughputThreads32() {
        return generator.nextLongId();
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    @Threads(1)
    public long averageTimeThreads1() {
        return generator.nextLongId();
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    @Threads(2)
    public long averageTimeThreads2() {
        return generator.nextLongId();
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    @Threads(4)
    public long averageTimeThreads4() {
        return generator.nextLongId();
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    @Threads(8)
    public long averageTimeThreads8() {
        return generator.nextLongId();
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    @Threads(16)
    public long averageTimeThreads16() {
        return generator.nextLongId();
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    @Threads(32)
    public long averageTimeThreads32() {
        return generator.nextLongId();
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    @OperationsPerInvocation(32)
    @Threads(1)
    public long virtualThreadBatch32(VirtualThreadState state) throws Exception {
        return runVirtualThreadBatch(state, 32);
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    @OperationsPerInvocation(128)
    @Threads(1)
    public long virtualThreadBatch128(VirtualThreadState state) throws Exception {
        return runVirtualThreadBatch(state, 128);
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    @OperationsPerInvocation(512)
    @Threads(1)
    public long virtualThreadBatch512(VirtualThreadState state) throws Exception {
        return runVirtualThreadBatch(state, 512);
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    @OperationsPerInvocation(2_048)
    @Threads(1)
    public long virtualThreadBatch2048(VirtualThreadState state) throws Exception {
        return runVirtualThreadBatch(state, 2_048);
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    @OperationsPerInvocation(8_192)
    @Threads(1)
    public long virtualThreadBatch8192(VirtualThreadState state) throws Exception {
        return runVirtualThreadBatch(state, 8_192);
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    @OperationsPerInvocation(32_768)
    @Threads(1)
    public long virtualThreadBatch32768(VirtualThreadState state) throws Exception {
        return runVirtualThreadBatch(state, 32_768);
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    @OperationsPerInvocation(65_536)
    @Threads(1)
    public long virtualThreadBatch65536(VirtualThreadState state) throws Exception {
        return runVirtualThreadBatch(state, 65_536);
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    @OperationsPerInvocation(131_072)
    @Threads(1)
    public long virtualThreadBatch131072(VirtualThreadState state) throws Exception {
        return runVirtualThreadBatch(state, 131_072);
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    @OperationsPerInvocation(262_144)
    @Threads(1)
    public long virtualThreadBatch262144(VirtualThreadState state) throws Exception {
        return runVirtualThreadBatch(state, 262_144);
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    @OperationsPerInvocation(524_288)
    @Threads(1)
    public long virtualThreadBatch524288(VirtualThreadState state) throws Exception {
        return runVirtualThreadBatch(state, 524_288);
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    @OperationsPerInvocation(1_048_576)
    @Threads(1)
    public long virtualThreadBatch1048576(VirtualThreadState state) throws Exception {
        return runVirtualThreadBatch(state, 1_048_576);
    }

    private long runVirtualThreadBatch(VirtualThreadState state, int taskCount) throws Exception {
        long checksum = 0L;
        List<Future<Long>> futures = state.executor.invokeAll(
                Collections.nCopies(taskCount, state.generationTask));
        for (Future<Long> future : futures) {
            checksum ^= future.get();
        }
        return checksum;
    }

    /**
     * Trial-scoped virtual-thread executor and one shared Snowflake generator.
     */
    @State(Scope.Benchmark)
    public static class VirtualThreadState {

        private ExecutorService executor;
        private Callable<Long> generationTask;

        @Setup(Level.Trial)
        public void setUp() {
            SnowflakeIdGenerator sharedGenerator = new SnowflakeIdGenerator(43);
            executor = Executors.newVirtualThreadPerTaskExecutor();
            generationTask = sharedGenerator::nextLongId;
        }

        @TearDown(Level.Trial)
        public void tearDown() {
            executor.close();
        }
    }
}
