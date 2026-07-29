package top.egon.cola.component.bytecode.starter.accessguard;

import top.egon.cola.component.accessguard.adapter.aop.GuardBinding;
import top.egon.cola.component.accessguard.adapter.aop.GuardBindingResolver;
import top.egon.cola.component.accessguard.api.AccessGuardAgentIntegration;
import top.egon.cola.component.accessguard.core.GuardEngine;
import top.egon.cola.component.accessguard.core.GuardEntryType;
import top.egon.cola.component.accessguard.core.GuardInvocation;
import top.egon.cola.component.accessguard.core.GuardInvocationKind;
import top.egon.cola.component.bytecode.bridge.BridgeCapability;
import top.egon.cola.component.bytecode.bridge.BridgeConstructorInvocation;
import top.egon.cola.component.bytecode.bridge.BridgeGuardedInvocation;
import top.egon.cola.component.bytecode.bridge.ConstructorGuardDecision;
import top.egon.cola.component.bytecode.runtime.accessguard.GuardedInvocationEvaluator;
import top.egon.cola.component.bytecode.starter.methodextension.MethodMetadataResolver;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class AccessGuardRuntimeAdapter
        implements GuardedInvocationEvaluator, AccessGuardAgentIntegration {

    private static final String BINDING_KEY_ATTRIBUTE = "accessGuard.bindingKey";

    private final GuardEngine engine;
    private final MethodMetadataResolver metadataResolver;
    private final GuardBindingResolver bindingResolver;
    private volatile boolean ready;

    public AccessGuardRuntimeAdapter(
            GuardEngine engine,
            MethodMetadataResolver metadataResolver,
            GuardBindingResolver bindingResolver
    ) {
        this.engine = Objects.requireNonNull(engine, "engine");
        this.metadataResolver = Objects.requireNonNull(metadataResolver, "metadataResolver");
        this.bindingResolver = Objects.requireNonNull(bindingResolver, "bindingResolver");
    }

    public void markReady() {
        ready = true;
    }

    @Override
    public Object invokeGuarded(BridgeGuardedInvocation invocation) throws Throwable {
        requireReady("method");
        Method method = metadataResolver.resolve(
                invocation.declaringClass(),
                invocation.methodId(),
                BridgeCapability.ACCESS_GUARD);
        GuardBinding binding = bindingResolver.resolve(method, invocation.declaringClass())
                .orElseThrow(() -> new IllegalStateException(
                        "Access Guard binding is missing for " + method.toGenericString()));
        return engine.execute(new GuardInvocation(
                binding.ruleId(),
                invocation.target(),
                invocation.declaringClass(),
                method,
                invocation.arguments(),
                attributes(binding),
                GuardEntryType.AGENT,
                GuardInvocationKind.METHOD,
                invocation::proceed));
    }

    @Override
    public ConstructorGuardDecision guardConstructor(BridgeConstructorInvocation invocation) {
        if (!ready) {
            return ConstructorGuardDecision.throwing(new IllegalStateException(
                    "Access Guard constructor runtime is not ready"));
        }
        try {
            Constructor<?> constructor = metadataResolver.resolveConstructor(
                    invocation.declaringClass(), invocation.methodId());
            GuardBinding binding = bindingResolver.resolve(constructor)
                    .orElseThrow(() -> new IllegalStateException(
                            "Access Guard binding is missing for " + constructor.toGenericString()));
            engine.execute(new GuardInvocation(
                    binding.ruleId(),
                    null,
                    invocation.declaringClass(),
                    constructor,
                    invocation.arguments(),
                    attributes(binding),
                    GuardEntryType.AGENT,
                    GuardInvocationKind.CONSTRUCTOR,
                    () -> null));
            return ConstructorGuardDecision.allow();
        } catch (Throwable failure) {
            return ConstructorGuardDecision.throwing(failure);
        }
    }

    private void requireReady(String target) {
        if (!ready) {
            throw new IllegalStateException("Access Guard Agent " + target + " runtime is not ready");
        }
    }

    private static Map<String, Object> attributes(GuardBinding binding) {
        if (binding.key().isBlank()) {
            return Map.of();
        }
        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put(BINDING_KEY_ATTRIBUTE, binding.key());
        return Map.copyOf(attributes);
    }
}
