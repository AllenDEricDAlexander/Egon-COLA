package top.egon.cola.component.bytecode.runtime.executor;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.function.Function;

public final class ExecutorNameResolver {

    private final List<Function<Executor, String>> nameSources;
    private final Map<String, String> configuredNames;

    public ExecutorNameResolver(
            List<? extends Function<Executor, String>> nameSources,
            Map<String, String> configuredNames
    ) {
        this.nameSources = List.copyOf(nameSources);
        this.configuredNames = Map.copyOf(configuredNames);
    }

    public String resolve(Executor executor) {
        Objects.requireNonNull(executor, "executor");
        for (Function<Executor, String> source : nameSources) {
            String name = source.apply(executor);
            if (name != null && !name.isBlank()) {
                return name;
            }
        }
        String configured = configuredNames.get(executor.getClass().getName());
        if (configured != null && !configured.isBlank()) {
            return configured;
        }
        return executor.getClass().getName() + "@"
                + Integer.toHexString(System.identityHashCode(executor));
    }
}
