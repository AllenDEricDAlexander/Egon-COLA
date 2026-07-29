package top.egon.cola.component.accessguard.execution;

import top.egon.cola.component.accessguard.api.AccessGuardRejectedException;
import top.egon.cola.component.accessguard.core.GuardInvocation;
import top.egon.cola.component.accessguard.core.GuardInvocationKind;
import top.egon.cola.component.accessguard.core.GuardOutcome;
import top.egon.cola.component.accessguard.core.plan.ExecutionConfig;

import java.lang.reflect.Method;
import java.util.Objects;

public final class DefaultRejectionHandler implements RejectionHandler {

    private final FallbackHandler fallbackHandler;
    private final JsonRejectValueParser jsonParser;

    public DefaultRejectionHandler(FallbackHandler fallbackHandler, JsonRejectValueParser jsonParser) {
        this.fallbackHandler = Objects.requireNonNull(fallbackHandler, "fallbackHandler");
        this.jsonParser = Objects.requireNonNull(jsonParser, "jsonParser");
    }

    @Override
    public Object resolve(
            GuardInvocation invocation,
            GuardOutcome rejected,
            ExecutionConfig.RejectionConfig config
    ) throws Throwable {
        Objects.requireNonNull(invocation, "invocation");
        Objects.requireNonNull(rejected, "rejected");
        Objects.requireNonNull(config, "config");
        return switch (config.mode()) {
            case THROW -> throw new AccessGuardRejectedException(rejected);
            case FALLBACK -> fallbackHandler.execute(invocation, rejected, config.fallbackMethod());
            case RETURN_JSON -> jsonParser.parse(config.returnJson(), returnType(invocation));
            case RETURN_NULL -> returnNull(invocation);
        };
    }

    private static Object returnNull(GuardInvocation invocation) {
        if (invocation.kind() == GuardInvocationKind.CONSTRUCTOR) {
            throw new IllegalArgumentException("constructors do not support RETURN_NULL");
        }
        if (returnType(invocation).isPrimitive()) {
            throw new IllegalArgumentException("primitive return types do not support RETURN_NULL");
        }
        return null;
    }

    private static Class<?> returnType(GuardInvocation invocation) {
        if (invocation.kind() == GuardInvocationKind.OPERATION) {
            return invocation.targetClass();
        }
        if (invocation.executable() instanceof Method method) {
            return method.getReturnType();
        }
        throw new IllegalArgumentException("constructors do not support rejection values");
    }
}
