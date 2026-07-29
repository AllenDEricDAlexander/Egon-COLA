package top.egon.cola.component.accessguard.execution;

import top.egon.cola.component.accessguard.api.GuardedOperation;
import top.egon.cola.component.accessguard.core.GuardInvocation;
import top.egon.cola.component.accessguard.core.GuardInvocationKind;
import top.egon.cola.component.accessguard.core.GuardOutcome;

import java.lang.reflect.Executable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public final class MethodHandleFallbackHandler implements FallbackHandler {

    public static final String PROGRAMMATIC_FALLBACK_ATTRIBUTE = "accessGuard.fallback";

    private final FallbackMethodCache cache;

    public MethodHandleFallbackHandler(FallbackMethodCache cache) {
        this.cache = Objects.requireNonNull(cache, "cache");
    }

    @Override
    public Object execute(GuardInvocation invocation, GuardOutcome outcome, String fallbackMethod) throws Throwable {
        Object programmatic = invocation.attributes().get(PROGRAMMATIC_FALLBACK_ATTRIBUTE);
        if (programmatic instanceof GuardedOperation<?> operation) {
            return operation.execute();
        }
        if (invocation.kind() == GuardInvocationKind.CONSTRUCTOR) {
            throw new IllegalArgumentException("constructors do not support fallback");
        }
        Executable executable = Objects.requireNonNull(invocation.executable(), "executable");
        FallbackMethodCache.Binding binding = cache.binding(executable, fallbackMethod);
        List<Object> arguments = new ArrayList<>();
        if (!binding.staticMethod()) {
            arguments.add(Objects.requireNonNull(invocation.target(), "instance fallback requires a target"));
        }
        if (binding.argumentMode() != FallbackMethodCache.ArgumentMode.NONE) {
            arguments.addAll(Arrays.asList(invocation.arguments()));
        }
        if (binding.argumentMode() == FallbackMethodCache.ArgumentMode.ARGUMENTS_AND_OUTCOME) {
            arguments.add(outcome);
        }
        return binding.handle().invokeWithArguments(arguments);
    }
}
