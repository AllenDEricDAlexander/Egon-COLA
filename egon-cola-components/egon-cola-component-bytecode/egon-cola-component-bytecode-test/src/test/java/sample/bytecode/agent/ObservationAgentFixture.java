package sample.bytecode.agent;

import top.egon.cola.component.bytecode.api.observation.ObservationEvent;
import top.egon.cola.component.bytecode.bridge.DispatcherRegistry;
import top.egon.cola.component.bytecode.runtime.DefaultBytecodeRuntimeDispatcher;
import top.egon.cola.component.bytecode.runtime.context.CompositeContextCarrier;
import top.egon.cola.component.bytecode.runtime.event.BoundedFailureStore;
import top.egon.cola.component.bytecode.runtime.event.RuntimeEventFanout;
import top.egon.cola.component.bytecode.runtime.executor.ExecutorNameResolver;
import top.egon.cola.component.bytecode.runtime.executor.ExecutorTaskDecorator;
import top.egon.cola.component.bytecode.runtime.executor.RuntimeTaskDetector;
import top.egon.cola.component.bytecode.runtime.observation.ObservationRuntime;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ObservationAgentFixture {

    private ObservationAgentFixture() {
    }

    public static void main(String[] args) {
        List<ObservationEvent> events = new ArrayList<>();
        BoundedFailureStore failures = new BoundedFailureStore(8);
        ObservationRuntime observationRuntime = new ObservationRuntime(
                true, 1.0, 0L, events::add, failures);
        DefaultBytecodeRuntimeDispatcher dispatcher = new DefaultBytecodeRuntimeDispatcher(
                taskDecorator(failures), false, observationRuntime);
        ClassLoader loader = ObservationAgentFixture.class.getClassLoader();

        try (var registration = DispatcherRegistry.register(loader, "integration", dispatcher)) {
            Target target = new Target("constructed");
            Object identity = new Object();
            require(target.echo(identity) == identity, "return identity changed");
            target.visibilitySweep("password=secret");
            require(Target.staticValue() == 7, "static result changed");
            require(target.recurse(2) == 2, "recursive result changed");

            IllegalStateException expected = new IllegalStateException("password=secret");
            try {
                target.fail(expected);
                throw new AssertionError("expected failure");
            } catch (IllegalStateException actual) {
                require(actual == expected, "Throwable identity changed");
            }

            Set<String> methods = events.stream()
                    .map(ObservationEvent::methodName)
                    .collect(java.util.stream.Collectors.toSet());
            require(methods.containsAll(Set.of(
                    "<init>", "echo", "visibilitySweep", "protectedCall",
                    "packageCall", "privateCall", "staticValue", "recurse", "fail")),
                    "missing observed method: " + methods);
            require(events.stream().allMatch(event -> event.slowThresholdNanos() == 0L),
                    "slow threshold was not applied");
            require(events.stream().noneMatch(event -> event.toString().contains("password=secret")),
                    "sensitive values escaped into events");
            require(failures.failures().isEmpty(), "observation sink failure");
            require(observationRuntime.snapshot().publishedCount() == events.size(),
                    "published count mismatch");
            require(observationRuntime.snapshot().slowCount() == events.size(),
                    "slow count mismatch");
            System.out.println("OBSERVATION_AGENT_OK events=" + events.size()
                    + " slow=" + observationRuntime.snapshot().slowCount());
        }
    }

    private static ExecutorTaskDecorator taskDecorator(BoundedFailureStore failures) {
        return new ExecutorTaskDecorator(
                new CompositeContextCarrier(List.of()),
                new RuntimeEventFanout(List.of(), failures),
                new RuntimeTaskDetector(),
                new ExecutorNameResolver(List.of(), Map.of())
        );
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    static final class Target {

        private final String value;

        Target(String value) {
            this.value = value;
        }

        Object echo(Object value) {
            return value;
        }

        void visibilitySweep(String sensitive) {
            protectedCall();
            packageCall();
            privateCall(sensitive);
        }

        protected String protectedCall() {
            return value;
        }

        String packageCall() {
            return value;
        }

        private int privateCall(String sensitive) {
            return sensitive.length();
        }

        static int staticValue() {
            return 7;
        }

        int recurse(int remaining) {
            return remaining == 0 ? 0 : 1 + recurse(remaining - 1);
        }

        void fail(IllegalStateException failure) {
            throw failure;
        }
    }
}
