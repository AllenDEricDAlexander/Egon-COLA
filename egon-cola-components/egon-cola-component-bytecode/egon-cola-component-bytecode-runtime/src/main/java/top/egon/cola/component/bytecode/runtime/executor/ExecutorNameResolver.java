package top.egon.cola.component.bytecode.runtime.executor;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
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

    /**
     * Resolves a display name and applies any configured alias. The alias map is keyed by the name a
     * source produced (typically the Spring bean name) and falls back to the executor class name, so
     * both {@code names.applicationTaskExecutor} and {@code names.<class>} remap as documented.
     */
    public String resolve(Executor executor) {
        Objects.requireNonNull(executor, "executor");
        for (Function<Executor, String> source : nameSources) {
            String name = source.apply(executor);
            if (name != null && !name.isBlank()) {
                return configuredName(name).orElse(name);
            }
        }
        String className = executor.getClass().getName();
        return configuredName(className).orElseGet(() ->
                className + "@" + Integer.toHexString(System.identityHashCode(executor)));
    }

    private Optional<String> configuredName(String key) {
        String configured = configuredNames.get(key);
        return configured == null || configured.isBlank() ? Optional.empty() : Optional.of(configured);
    }
}
