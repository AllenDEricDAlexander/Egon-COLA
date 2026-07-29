package top.egon.cola.component.accessguard.adapter.programmatic;

import top.egon.cola.component.accessguard.api.AccessGuardClient;
import top.egon.cola.component.accessguard.api.GuardRequest;
import top.egon.cola.component.accessguard.api.GuardedOperation;
import top.egon.cola.component.accessguard.core.GuardEngine;
import top.egon.cola.component.accessguard.core.GuardEntryType;
import top.egon.cola.component.accessguard.core.GuardInvocation;
import top.egon.cola.component.accessguard.core.GuardInvocationKind;
import top.egon.cola.component.accessguard.core.GuardOutcome;
import top.egon.cola.component.accessguard.execution.MethodHandleFallbackHandler;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class DefaultAccessGuardClient implements AccessGuardClient {

    private final GuardEngine engine;

    public DefaultAccessGuardClient(GuardEngine engine) {
        this.engine = Objects.requireNonNull(engine, "engine");
    }

    @Override
    public GuardOutcome evaluate(GuardRequest request) {
        return engine.evaluate(invocation(request, () -> null));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T execute(GuardRequest request, GuardedOperation<T> operation) throws Throwable {
        return (T) engine.execute(invocation(request, operation));
    }

    private static GuardInvocation invocation(GuardRequest request, GuardedOperation<?> operation) {
        Objects.requireNonNull(request, "request");
        Objects.requireNonNull(operation, "operation");
        Map<String, Object> attributes = new LinkedHashMap<>(request.attributes());
        if (request.fallback() != null) {
            attributes.put(MethodHandleFallbackHandler.PROGRAMMATIC_FALLBACK_ATTRIBUTE, request.fallback());
        }
        return new GuardInvocation(
                request.ruleId(),
                null,
                request.returnType(),
                null,
                request.arguments(),
                attributes,
                GuardEntryType.PROGRAMMATIC,
                GuardInvocationKind.OPERATION,
                operation);
    }
}
