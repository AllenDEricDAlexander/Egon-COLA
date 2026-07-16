package top.egon.cola.component.bytecode.starter.accessguard;

import org.springframework.util.StringUtils;
import top.egon.cola.component.accessguard.agent.AgentProceedingJoinPoint;
import top.egon.cola.component.accessguard.config.AccessGuardRule;
import top.egon.cola.component.accessguard.config.AccessGuardRuleResolver;
import top.egon.cola.component.accessguard.execution.AccessGuardExecutionService;
import top.egon.cola.component.accessguard.execution.AccessGuardFailureHandler;
import top.egon.cola.component.bytecode.bridge.BridgeCapability;
import top.egon.cola.component.bytecode.bridge.BridgeGuardedInvocation;
import top.egon.cola.component.bytecode.runtime.accessguard.GuardedInvocationEvaluator;
import top.egon.cola.component.bytecode.starter.methodextension.MethodMetadataResolver;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.function.Supplier;

public final class AccessGuardRuntimeAdapter implements GuardedInvocationEvaluator {

    private final Supplier<AccessGuardExecutionService> executionServices;
    private final MethodMetadataResolver metadataResolver;
    private final AccessGuardRuleResolver ruleResolver;
    private final AccessGuardFailureHandler failureHandler;
    private volatile boolean ready;

    public AccessGuardRuntimeAdapter(
            Supplier<AccessGuardExecutionService> executionServices,
            MethodMetadataResolver metadataResolver,
            AccessGuardRuleResolver ruleResolver,
            AccessGuardFailureHandler failureHandler
    ) {
        this.executionServices = executionServices;
        this.metadataResolver = metadataResolver;
        this.ruleResolver = ruleResolver;
        this.failureHandler = failureHandler;
    }

    public void markReady() {
        ready = true;
    }

    @Override
    public Object invokeGuarded(BridgeGuardedInvocation invocation) throws Throwable {
        if (!ready) {
            return notReady(invocation, new IllegalStateException(
                    "Access Guard Agent runtime is not ready"));
        }
        AccessGuardExecutionService executionService;
        try {
            executionService = executionServices.get();
        } catch (RuntimeException failure) {
            return notReady(invocation, failure);
        }
        if (executionService == null) {
            return notReady(invocation, new IllegalStateException(
                    "Access Guard execution service is unavailable"));
        }
        Method method;
        AccessGuardRule rule;
        try {
            method = metadataResolver.resolve(
                    invocation.declaringClass(),
                    invocation.methodId(),
                    BridgeCapability.ACCESS_GUARD
            );
            rule = ruleResolver.resolve(method);
            validate(method, rule);
        } catch (RuntimeException failure) {
            if (failureHandler.failOpen(null, "Agent rule validation", failure)) {
                return invocation.proceed();
            }
            throw failure;
        }
        AgentProceedingJoinPoint joinPoint = new AgentProceedingJoinPoint(
                invocation.target(),
                method,
                invocation.continuation(),
                invocation.arguments()
        );
        return executionService.execute(joinPoint);
    }

    private Object notReady(
            BridgeGuardedInvocation invocation,
            RuntimeException failure
    ) throws Throwable {
        if (failureHandler.failOpen(null, "Agent readiness", failure)) {
            return invocation.proceed();
        }
        throw failure;
    }

    private void validate(Method method, AccessGuardRule rule) {
        int modifiers = method.getModifiers();
        if (!Modifier.isPublic(modifiers) && !Modifier.isPrivate(modifiers)) {
            throw unsupported(method, "only public and private methods are supported");
        }
        if (Modifier.isAbstract(modifiers) || Modifier.isNative(modifiers)
                || method.isSynthetic() || method.isBridge()) {
            throw unsupported(method, "abstract, native, synthetic, and bridge methods are unsupported");
        }
        if (Modifier.isSynchronized(modifiers) && rule.timeoutEnabled()) {
            throw unsupported(method, "synchronized methods cannot enable timeout");
        }
        if (Modifier.isStatic(modifiers) && StringUtils.hasText(rule.fallbackMethod())) {
            validateStaticFallback(method, rule.fallbackMethod());
        }
    }

    private void validateStaticFallback(Method guardedMethod, String fallbackName) {
        Method fallback = null;
        for (Method candidate : guardedMethod.getDeclaringClass().getDeclaredMethods()) {
            if (candidate.getName().equals(fallbackName)
                    && java.util.Arrays.equals(
                    candidate.getParameterTypes(), guardedMethod.getParameterTypes())) {
                fallback = candidate;
                break;
            }
        }
        if (fallback == null || !Modifier.isStatic(fallback.getModifiers())) {
            throw unsupported(guardedMethod,
                    "static methods require a static fallback with matching parameters");
        }
    }

    private IllegalArgumentException unsupported(Method method, String reason) {
        return new IllegalArgumentException(
                "Unsupported Access Guard Agent method " + method.toGenericString()
                        + ": " + reason);
    }
}
