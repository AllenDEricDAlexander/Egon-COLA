package top.egon.cola.component.bytecode.benchmark;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import top.egon.cola.component.bytecode.bridge.BridgeCapability;
import top.egon.cola.component.bytecode.bridge.DispatcherRegistration;
import top.egon.cola.component.bytecode.bridge.DispatcherRegistry;
import top.egon.cola.component.bytecode.bridge.EgonObservationBridge;
import top.egon.cola.component.bytecode.bridge.MethodMetadata;
import top.egon.cola.component.bytecode.bridge.ObservationMetadata;
import top.egon.cola.component.bytecode.bridge.ObservationToken;
import top.egon.cola.component.bytecode.runtime.DefaultBytecodeRuntimeDispatcher;
import top.egon.cola.component.bytecode.runtime.context.CompositeContextCarrier;
import top.egon.cola.component.bytecode.runtime.event.BoundedFailureStore;
import top.egon.cola.component.bytecode.runtime.event.RuntimeEventFanout;
import top.egon.cola.component.bytecode.runtime.executor.ExecutorNameResolver;
import top.egon.cola.component.bytecode.runtime.executor.ExecutorTaskDecorator;
import top.egon.cola.component.bytecode.runtime.executor.RuntimeTaskDetector;
import top.egon.cola.component.bytecode.runtime.observation.ObservationRuntime;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
public class MethodObservationBenchmark {

    private static final long SUCCESS_METHOD_ID = 81_001L;
    private static final long ERROR_METHOD_ID = 81_002L;
    private static final long SLOW_METHOD_ID = 81_003L;

    private DispatcherRegistration registration;

    @Setup(Level.Trial)
    public void setUp() {
        ClassLoader loader = getClass().getClassLoader();
        register(loader, SUCCESS_METHOD_ID, "success", Long.MAX_VALUE);
        register(loader, ERROR_METHOD_ID, "failure", Long.MAX_VALUE);
        register(loader, SLOW_METHOD_ID, "slow", 0L);
        BoundedFailureStore failures = new BoundedFailureStore(8);
        ObservationRuntime runtime = new ObservationRuntime(
                true, 1.0, -1L, event -> { }, failures);
        registration = DispatcherRegistry.register(
                loader,
                "benchmark",
                new DefaultBytecodeRuntimeDispatcher(
                        taskDecorator(failures), false, runtime)
        );
    }

    @TearDown(Level.Trial)
    public void tearDown() {
        registration.close();
    }

    @Benchmark
    public int directBaseline() {
        return businessValue();
    }

    @Benchmark
    public int disabledBridge() {
        ObservationToken token = EgonObservationBridge.enter(String.class, SUCCESS_METHOD_ID);
        try {
            int result = businessValue();
            EgonObservationBridge.success(token);
            return result;
        } finally {
            EgonObservationBridge.exit(token);
        }
    }

    @Benchmark
    public int enabledSuccess() {
        return observedSuccess(SUCCESS_METHOD_ID);
    }

    @Benchmark
    public int enabledException() {
        ObservationToken token = EgonObservationBridge.enter(
                MethodObservationBenchmark.class, ERROR_METHOD_ID);
        try {
            throw failure();
        } catch (IllegalStateException expected) {
            EgonObservationBridge.error(token, expected);
            return expected.getClass().hashCode();
        } finally {
            EgonObservationBridge.exit(token);
        }
    }

    @Benchmark
    public int slowEvent() {
        return observedSuccess(SLOW_METHOD_ID);
    }

    private int observedSuccess(long methodId) {
        ObservationToken token = EgonObservationBridge.enter(
                MethodObservationBenchmark.class, methodId);
        try {
            int result = businessValue();
            EgonObservationBridge.success(token);
            return result;
        } finally {
            EgonObservationBridge.exit(token);
        }
    }

    private int businessValue() {
        return 42;
    }

    private IllegalStateException failure() {
        return new IllegalStateException();
    }

    private void register(
            ClassLoader loader,
            long methodId,
            String methodName,
            long slowThresholdNanos
    ) {
        DispatcherRegistry.registerMethod(loader, new MethodMetadata(
                methodId,
                MethodObservationBenchmark.class.getName().replace('.', '/'),
                methodName,
                "()I",
                java.lang.reflect.Modifier.PUBLIC,
                false,
                Set.of(BridgeCapability.OBSERVATION)
        ));
        DispatcherRegistry.registerObservation(loader, new ObservationMetadata(
                methodId, "APPLICATION", Map.of("benchmark", "observation"),
                slowThresholdNanos));
    }

    private ExecutorTaskDecorator taskDecorator(BoundedFailureStore failures) {
        return new ExecutorTaskDecorator(
                new CompositeContextCarrier(List.of()),
                new RuntimeEventFanout(List.of(), failures),
                new RuntimeTaskDetector(),
                new ExecutorNameResolver(List.of(), Map.of())
        );
    }
}
