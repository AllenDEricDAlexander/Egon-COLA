package top.egon.cola.component.bytecode.benchmark;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import top.egon.cola.component.bytecode.agent.AgentConfiguration;
import top.egon.cola.component.bytecode.agent.AgentConfigurationLoader;
import top.egon.cola.component.bytecode.agent.ClassNameFilter;
import top.egon.cola.component.bytecode.core.enhance.executor.ExecutorCallSiteEnhancer;
import top.egon.cola.component.bytecode.runtime.context.CompositeContextCarrier;
import top.egon.cola.component.bytecode.runtime.event.BoundedFailureStore;
import top.egon.cola.component.bytecode.runtime.event.RuntimeEventFanout;
import top.egon.cola.component.bytecode.runtime.executor.ExecutorNameResolver;
import top.egon.cola.component.bytecode.runtime.executor.ExecutorTaskDecorator;
import top.egon.cola.component.bytecode.runtime.executor.RuntimeTaskDetector;
import top.egon.cola.component.bytecode.starter.metrics.MicrometerExecutorEventSink;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
public class ExecutorEnhancementBenchmark {

    private final Executor directExecutor = Runnable::run;
    private final Runnable task = () -> { };
    private final ClassLoader loader = new ClassLoader(getClass().getClassLoader()) { };
    private final ExecutorCallSiteEnhancer enhancer = new ExecutorCallSiteEnhancer();

    private byte[] fixture;
    private byte[] classHeader;
    private ClassNameFilter unmatchedFilter;
    private ExecutorTaskDecorator contextOnly;
    private ExecutorTaskDecorator contextWithMetrics;

    @Setup(Level.Trial)
    public void setUp() {
        fixture = executorFixture();
        classHeader = java.util.Arrays.copyOf(fixture, 8);
        AgentConfiguration configuration = new AgentConfigurationLoader()
                .load("enabled=true,include=application.*");
        unmatchedFilter = new ClassNameFilter(configuration);
        contextOnly = decorator(new RuntimeEventFanout(
                List.of(), new BoundedFailureStore(8)));
        contextWithMetrics = decorator(new RuntimeEventFanout(
                List.of(new MicrometerExecutorEventSink(new SimpleMeterRegistry(), 1.0)),
                new BoundedFailureStore(8)));
    }

    @Benchmark
    public boolean unmatchedFilter() {
        return unmatchedFilter.matches(loader, "other/Service", classHeader);
    }

    @Benchmark
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    public byte[] transformOneThousandClasses() {
        byte[] transformed = null;
        for (int index = 0; index < 1_000; index++) {
            transformed = enhancer.enhance(loader, fixture);
        }
        return transformed;
    }

    @Benchmark
    public void directSubmission() {
        directExecutor.execute(task);
    }

    @Benchmark
    public void contextOnlySubmission() {
        contextOnly.decorateRunnable(directExecutor, task, 1).run();
    }

    @Benchmark
    public void contextAndMetricsSubmission() {
        contextWithMetrics.decorateRunnable(directExecutor, task, 1).run();
    }

    private ExecutorTaskDecorator decorator(RuntimeEventFanout eventFanout) {
        return new ExecutorTaskDecorator(
                new CompositeContextCarrier(List.of()),
                eventFanout,
                new RuntimeTaskDetector(),
                new ExecutorNameResolver(List.of(), Map.of())
        );
    }

    private byte[] executorFixture() {
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        writer.visit(Opcodes.V21, Opcodes.ACC_PUBLIC, "application/BenchmarkCaller",
                null, "java/lang/Object", null);
        MethodVisitor method = writer.visitMethod(
                Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
                "execute",
                "(Ljava/util/concurrent/Executor;Ljava/lang/Runnable;)V",
                null,
                null
        );
        method.visitCode();
        method.visitVarInsn(Opcodes.ALOAD, 0);
        method.visitVarInsn(Opcodes.ALOAD, 1);
        method.visitMethodInsn(
                Opcodes.INVOKEINTERFACE,
                "java/util/concurrent/Executor",
                "execute",
                "(Ljava/lang/Runnable;)V",
                true
        );
        method.visitInsn(Opcodes.RETURN);
        method.visitMaxs(0, 0);
        method.visitEnd();
        writer.visitEnd();
        return writer.toByteArray();
    }
}
