package top.egon.cola.component.bytecode.runtime.executor;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExecutorNameResolverTest {

    private final Executor executor = Runnable::run;

    @Test
    void remapsTheNameProducedByASource() {
        ExecutorNameResolver resolver = new ExecutorNameResolver(
                List.of(ignored -> "applicationTaskExecutor"),
                Map.of("applicationTaskExecutor", "application"));

        assertEquals("application", resolver.resolve(executor));
    }

    @Test
    void keepsTheSourceNameWhenNoAliasIsConfigured() {
        ExecutorNameResolver resolver = new ExecutorNameResolver(
                List.of(ignored -> "applicationTaskExecutor"),
                Map.of("other", "alias"));

        assertEquals("applicationTaskExecutor", resolver.resolve(executor));
    }

    @Test
    void remapsByClassNameWhenNoSourceProducesAName() {
        ExecutorNameResolver resolver = new ExecutorNameResolver(
                List.of(ignored -> null),
                Map.of(executor.getClass().getName(), "inline"));

        assertEquals("inline", resolver.resolve(executor));
    }

    @Test
    void fallsBackToIdentityWhenNothingMatches() {
        ExecutorNameResolver resolver = new ExecutorNameResolver(List.of(), Map.of());

        assertTrue(resolver.resolve(executor).startsWith(executor.getClass().getName() + "@"));
    }
}
